package com.bytedance.douyinclouddemo.service;

import com.bytedance.douyinclouddemo.dto.RankPlayerDTO;
import com.bytedance.douyinclouddemo.entity.Player;
import com.bytedance.douyinclouddemo.service.PlayerService;
import com.bytedance.douyinclouddemo.utils.KVPair;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.bytedance.douyinclouddemo.model.RankType;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RankService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Update player's score in specified ranking
     */
    public void updatePlayerScore(String playerId, long score, RankType rankType) {
        try {
            redisTemplate.opsForZSet().add(rankType.getKey(), playerId, score);
            log.debug("Updated rankings for player {}, new score: {}", playerId, score);
        } catch (Exception e) {
            log.error("Failed to update rankings for player: {}", playerId, e);
        }
    }

    public List<String> getTopPlayerID(int n, RankType rankType) {
        var tuples = redisTemplate.opsForZSet().reverseRangeWithScores(rankType.getKey(), 0, n - 1);
        if (tuples == null) {
            return new ArrayList<>();
        }

        return tuples.stream()
                .map(tuple -> tuple.getValue().toString())
                .collect(Collectors.toList());
    }
    
    /**
     * Reset or initialize rankings
     */
    public void resetRankings(RankType rankType) {
        try {
            redisTemplate.delete(rankType.getKey());
            log.info("{} rankings reset successfully", rankType);
        } catch (Exception e) {
            log.error("Failed to reset rankings for {}", rankType, e);
        }
    }
    
    public Integer getPlayerRank(String playerId, RankType rankType) {
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(rankType.getKey(), playerId);
            return rank != null ? rank.intValue() + 1 : 9999;
        } catch (Exception e) {
            log.error("Failed to get rank for player: {}", playerId, e);
            return null;
        }
    }

    public Map<String, Integer> getPlayerRanks(List<String> playerIds, RankType rankType) {
        Map<String, Integer> ranks = new HashMap<>();
        for (String playerId : playerIds) {
            Integer rank = getPlayerRank(playerId, rankType);
            if (rank != null) {
                ranks.put(playerId, rank);
            }
        }
        return ranks;
    }
}
