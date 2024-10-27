package com.bytedance.douyinclouddemo.dto;

import com.bytedance.douyinclouddemo.entity.Player;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GameEndDTO {
    public List<Player> totalRankTop;
    public List<PlayerEndInfo> playerEndInfos;
}

