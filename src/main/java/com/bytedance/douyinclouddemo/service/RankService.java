package com.bytedance.douyinclouddemo.service;

import com.bytedance.douyinclouddemo.utils.KVPair;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RankService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String GLOBAL_RANK_KEY = "rank:global";
    
    /**
     * Update player's score in ranking
     */
    public void updatePlayerScore(String playerId, long deltaScore) {
        try {
            redisTemplate.opsForZSet().incrementScore(GLOBAL_RANK_KEY, playerId, deltaScore);
            log.debug("Updated ranking for player {}, delta score: {}", playerId, deltaScore);
        } catch (Exception e) {
            log.error("Failed to update ranking for player: {}", playerId, e);
        }
    }

    /**
     * Get top N players with their scores
     */
    public List<KVPair<String, Double>> getTopPlayers(int n) {
        try {
            var tuples = redisTemplate.opsForZSet().reverseRangeWithScores(GLOBAL_RANK_KEY, 0, n - 1);
            if (tuples == null) {
                return new ArrayList<>();
            }
            
            return tuples.stream()
                .map(tuple -> new KVPair<>(tuple.getValue().toString(), tuple.getScore()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get top {} players", n, e);
            return new ArrayList<>();
        }
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
