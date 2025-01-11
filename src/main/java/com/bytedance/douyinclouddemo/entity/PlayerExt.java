package com.bytedance.douyinclouddemo.entity;

import java.util.Date;

import lombok.Data;

@Data
public class PlayerExt {
    private Integer rank;
    private Integer weekRank;
    private Long weekScore;
    private Date lastRankUpdateDate;
    private Date lastWeekRankUpdateDate;
}