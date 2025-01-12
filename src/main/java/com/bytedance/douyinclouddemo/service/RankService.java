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

    private static final String ANNOUNCEMENT_KEY = "game:announcement";
    private static final String MIN_VERSION_KEY = "game:min_version";

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

    /**
     * Set game announcement
     * @param announcement announcement text
     */
    public void setAnnouncement(String announcement) {
        try {
            redisTemplate.opsForValue().set(ANNOUNCEMENT_KEY, announcement);
            log.info("Game announcement updated successfully");
        } catch (Exception e) {
            log.error("Failed to update game announcement", e);
        }
    }

    /**
     * Get current game announcement
     * @return announcement text, or null if not set
     */
    public String getAnnouncement() {
        try {
            Object announcement = redisTemplate.opsForValue().get(ANNOUNCEMENT_KEY);
            return announcement != null ? announcement.toString() : null;
        } catch (Exception e) {
            log.error("Failed to get game announcement", e);
            return null;
        }
    }

    /**
     * Set minimum required app version
     * @param version minimum version number
     */
    public void setMinVersion(int version) {
        try {
            redisTemplate.opsForValue().set(MIN_VERSION_KEY, version);
            log.info("Minimum version requirement updated to: {}", version);
        } catch (Exception e) {
            log.error("Failed to update minimum version requirement", e);
        }
    }

    /**
     * Get minimum required app version
     * @return minimum version number, or 1 if not set
     */
    public int getMinVersion() {
        try {
            Object version = redisTemplate.opsForValue().get(MIN_VERSION_KEY);
            return version != null ? Integer.parseInt(version.toString()) : 1;
        } catch (Exception e) {
            log.error("Failed to get minimum version requirement", e);
            return 1;
        }
    }
}
