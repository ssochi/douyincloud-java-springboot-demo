package com.bytedance.douyinclouddemo.dto;

import com.bytedance.douyinclouddemo.entity.Player;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerEndInfo {
    public Player player;
    public long deltaScore;
    public long deltaWeekScore;
    public long deltaGlory;
    public int deltaRank;
    public int deltaWeekRank;
    public boolean isQuit;
}
