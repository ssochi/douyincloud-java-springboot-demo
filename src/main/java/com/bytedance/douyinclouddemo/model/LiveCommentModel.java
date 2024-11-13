package com.bytedance.douyinclouddemo.model;

import com.bytedance.douyinclouddemo.entity.Player;
import lombok.Data;

@Data
public class LiveCommentModel {
    private String msgId;
    private String secOpenid;
    private String content;
    private String avatarUrl;
    private String nickname;
    private Long timestamp;
    private Player player;
}