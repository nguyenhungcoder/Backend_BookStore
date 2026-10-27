package com.example.bookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            if (!jwtTokenProvider.validateToken(token)) {
                logger.debug("JWT token invalid or expired for request {}", request.getRequestURI());
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                logger.debug("JWT token for user '{}' detected for request {}", username, request.getRequestURI());
                UsernamePasswordAuthenticationToken authentication = null;
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    logger.debug("Loaded user details for '{}' from DB, authorities={}", username, userDetails.getAuthorities());
                } catch (UsernameNotFoundException ex) {
                    // Fallback: use role from token
                    String role = jwtTokenProvider.getRoleFromToken(token);
                    if (role != null && !role.isBlank()) {
                        var authorities = java.util.List.of(new SimpleGrantedAuthority(role));
                        authentication = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );
                        logger.debug("Fallback to role from token for '{}' authorities={}", username, authorities);
                    } else {
                        logger.debug("No role claim found in token for '{}'", username);
                    }
                }
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authentication set for user '{}'", username);
            }
        } else {
            logger.trace("No Authorization header found for request {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
