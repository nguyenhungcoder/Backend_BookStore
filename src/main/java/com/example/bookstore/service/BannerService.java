package com.example.bookstore.service;

import com.example.bookstore.dto.BannerResponse;
import com.example.bookstore.dto.CreateBannerRequest;
import com.example.bookstore.dto.UpdateBannerRequest;
import com.example.bookstore.dto.UpdateBannerStatusRequest;
import com.example.bookstore.entity.Banner;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BannerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class BannerService {

    private final BannerRepository bannerRepository;
    private final FileUploadService fileUploadService;

    public BannerService(BannerRepository bannerRepository, FileUploadService fileUploadService) {
        this.bannerRepository = bannerRepository;
        this.fileUploadService = fileUploadService;
    }

    @Transactional(readOnly = true)
    public Page<BannerResponse> getAllBanners(Pageable pageable) {
        return bannerRepository.findAllActive(pageable)
                .map(this::toBannerResponse);
    }

    @Transactional
    public BannerResponse createBanner(CreateBannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .image(request.getImage())
                .link(request.getLink())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .build();

        Banner savedBanner = bannerRepository.save(banner);
        return toBannerResponse(savedBanner);
    }

    @Transactional
    public BannerResponse uploadBannerImage(MultipartFile file) {
        String imagePath = fileUploadService.uploadFile(file, "banners");

        Banner banner = Banner.builder()
                .title("Uploaded Banner")
                .image(imagePath)
                .link("")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        Banner savedBanner = bannerRepository.save(banner);
        return toBannerResponse(savedBanner);
    }

    @Transactional
    public BannerResponse updateBanner(Long bannerId, UpdateBannerRequest request) {
        Banner banner = bannerRepository.findByIdAndNotDeleted(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));

        if (request.getTitle() != null) {
            banner.setTitle(request.getTitle());
        }
        if (request.getImage() != null) {
            banner.setImage(request.getImage());
        }
        if (request.getLink() != null) {
            banner.setLink(request.getLink());
        }
        if (request.getStatus() != null) {
            banner.setStatus(request.getStatus());
        }

        Banner updatedBanner = bannerRepository.save(banner);
        return toBannerResponse(updatedBanner);
    }

    @Transactional
    public BannerResponse updateBannerStatus(Long bannerId, UpdateBannerStatusRequest request) {
        Banner banner = bannerRepository.findByIdAndNotDeleted(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));

        banner.setStatus(request.getStatus());
        Banner updatedBanner = bannerRepository.save(banner);
        return toBannerResponse(updatedBanner);
    }

    @Transactional
    public void deleteBanner(Long bannerId) {
        Banner banner = bannerRepository.findByIdAndNotDeleted(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        
        // Soft delete: set status to DELETED
        banner.setStatus("DELETED");
        bannerRepository.save(banner);
    }

    private BannerResponse toBannerResponse(Banner banner) {
        return BannerResponse.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .image(banner.getImage())
                .link(banner.getLink())
                .status(banner.getStatus())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}
