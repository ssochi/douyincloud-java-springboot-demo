package com.bytedance.douyinclouddemo.model;

import lombok.Data;

@Data
public class LiveGiftModel {
    private String msgId;
    private String secOpenid;
    private String secGiftId;
    private Integer giftNum;
    private Integer giftValue;
    private String avatarUrl;
    private String nickname;
    private Long timestamp;
    private Boolean test;
}