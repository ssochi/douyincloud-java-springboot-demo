package com.bytedance.douyinclouddemo.dto;

import lombok.Data;

@Data
public class RankPlayerDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private Double score;
    private Integer rank;
} 