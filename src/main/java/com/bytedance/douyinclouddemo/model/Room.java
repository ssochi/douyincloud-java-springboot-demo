package com.bytedance.douyinclouddemo.model;

import lombok.Data;
import java.util.List;

@Data
public class Room {
    private String anchorOpenID;
    private List<String> playerList;
    private long createdAt;
    private long updatedAt;
}
