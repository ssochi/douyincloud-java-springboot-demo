package com.bytedance.douyinclouddemo.model;

public enum RankType {
    GLOBAL("rank:global"),
    WEEKLY("rank:week");

    private final String key;

    RankType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}