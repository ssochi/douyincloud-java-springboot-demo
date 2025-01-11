package com.bytedance.douyinclouddemo.entity;

import java.util.Date;

import lombok.Data;

@Data
public class PlayerExt {
    private Integer rank = 9999 ;
    private Integer weekRank = 9999;
    private Long weekScore = 0L;
    private Date lastRankUpdateDate = new Date();
    private Date lastWeekRankUpdateDate = new Date();
}