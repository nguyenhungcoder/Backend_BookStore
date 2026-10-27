

package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponseDTO {
    private Long id;
    private Long bookId;
    private String title;
    private String coverImage;
    private BigDecimal price;
    private String author;

    // ❤️ THÊM DÒNG NÀY VÀO LÀ HẾT BÁO ĐỎ NGAY LẬP TỨC:
    private String description;
    
    // (Nếu em muốn hiện cả Thể loại thì thêm luôn dòng này)
    // private String category; 
}