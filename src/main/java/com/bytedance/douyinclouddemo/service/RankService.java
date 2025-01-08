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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RankService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String GLOBAL_RANK_KEY = "rank:global";

    /**
     * Update player's score in ranking with absolute score value
     */
    public void updatePlayerScore(String playerId, long score) {
        try {
            redisTemplate.opsForZSet().add(GLOBAL_RANK_KEY, playerId, score);
            log.debug("Updated ranking for player {}, new score: {}", playerId, score);
        } catch (Exception e) {
            log.error("Failed to update ranking for player: {}", playerId, e);
        }
    }

    public List<String> getTopPlayerID(int n){
        var tuples = redisTemplate.opsForZSet().reverseRangeWithScores(GLOBAL_RANK_KEY, 0, n - 1);
        if (tuples == null) {
            return new ArrayList<>();
        }

        // Extract userIds from tuples
        return  tuples.stream()
                .map(tuple -> tuple.getValue().toString())
                .collect(Collectors.toList());
    }
    
    /**
     * Reset or initialize rankings
     */
    public void resetRankings() {
        try {
            redisTemplate.delete(GLOBAL_RANK_KEY);
            log.info("Rankings reset successfully");
        } catch (Exception e) {
            log.error("Failed to reset rankings", e);
        }
    }
    
    public Integer getPlayerRank(String playerId) {
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(GLOBAL_RANK_KEY, playerId);
            return rank != null ? rank.intValue() + 1 : 9999;
        } catch (Exception e) {
            log.error("Failed to get rank for player: {}", playerId, e);
            return null;
        }
    }

    public Map<String, Integer> getPlayerRanks(List<String> playerIds) {
        Map<String, Integer> ranks = new HashMap<>();
        for (String playerId : playerIds) {
            Integer rank = getPlayerRank(playerId);
            if (rank != null) {
                ranks.put(playerId, rank);
            }
        }
        return ranks;
    }
}
