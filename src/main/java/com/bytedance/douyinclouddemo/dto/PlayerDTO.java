package com.bytedance.douyinclouddemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PlayerDTO {
    private Integer id;
    private String userId;
    private String userName;
    private String avatarUrl;
    private Long score;
    private Long glory;
    private String ext;
    private Integer gameCount;
    private Integer totalPayment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}