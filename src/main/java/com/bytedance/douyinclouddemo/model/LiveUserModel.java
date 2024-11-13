package com.bytedance.douyinclouddemo.model;

import lombok.Data;

@Data
public class LiveUserModel {
    private String msgId;
    private String secOpenid;
    private String avatarUrl;
    private String nickname;
    private Long timestamp;
}