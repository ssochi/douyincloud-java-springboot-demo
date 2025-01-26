package com.bytedance.douyinclouddemo.model;

public enum RankType {
    GLOBAL("rank:global"), // 月榜

    WEEKLY("rank:week"); // 周榜

    private final String key;

    RankType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}