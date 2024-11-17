package com.bytedance.douyinclouddemo.service;

import com.bytedance.douyinclouddemo.entity.Player;
import com.bytedance.douyinclouddemo.entity.PlayerExt;
import com.bytedance.douyinclouddemo.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Map;

@Service
public class PlayerService {
    
    private static final String PLAYER_CACHE_PREFIX = "player:";
    private static final long CACHE_DURATION = 30; // 30 minutes cache
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private RankService rankService;
    
    @Transactional
    public Player createPlayer(Player player) {
        Player savedPlayer = playerRepository.save(player);
        // Cache the new player as JSON
        redisService.setJson(PLAYER_CACHE_PREFIX + player.getUserId(), 
                           savedPlayer, 
                           CACHE_DURATION, 
                           TimeUnit.MINUTES);
        return savedPlayer;
    }
    
    public Player findByUserId(String userId) {
        List<String> userIds = new ArrayList<>();
        userIds.add(userId);

        List<Player> byUserId = findByUserId(userIds);
        if (byUserId.size() == 0 ){
            return null;
        }

        return byUserId.get(0);
    }

    public List<Player> findByUserId(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Player> result = new ArrayList<>();
        List<String> missingUserIds = new ArrayList<>();

        // Batch query Redis for all keys
        for (String userId : userIds) {
            Player player = redisService.getJson(PLAYER_CACHE_PREFIX + userId, Player.class);
            if (player != null) {
                result.add(player);
            } else {
                missingUserIds.add(userId);
            }
        }

        // If there are any missing players, query them from DB
        if (!missingUserIds.isEmpty()) {
            List<Player> dbPlayers = playerRepository.findByUserIdIn(missingUserIds);
            
            // Cache the players that were found in DB
            for (Player player : dbPlayers) {
                redisService.setJson(PLAYER_CACHE_PREFIX + player.getUserId(), 
                                   player, 
                                   CACHE_DURATION, 
                                   TimeUnit.MINUTES);
            }
            result.addAll(dbPlayers);
        }

        // Add rank information to all players
        Map<String, Integer> ranks = rankService.getPlayerRanks(
            result.stream()
                .map(Player::getUserId)
                .collect(Collectors.toList())
        );
        
        for (Player player : result) {
            if (player.getExt() == null) {
                player.setExt(new PlayerExt());
            }
            player.getExt().setRank(ranks.getOrDefault(player.getUserId(), 9999));
        }

        return result;
    }

    public List<Player> findByUserIdDirectly(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Player> players = playerRepository.findByUserIdIn(userIds);
        
        // Add rank information to all players
        Map<String, Integer> ranks = rankService.getPlayerRanks(
            players.stream()
                .map(Player::getUserId)
                .collect(Collectors.toList())
        );
        
        for (Player player : players) {
            if (player.getExt() == null) {
                player.setExt(new PlayerExt());
            }
            player.getExt().setRank(ranks.getOrDefault(player.getUserId(), 9999));
        }
        
        return players;
    }
    
    @Transactional
    public Player updatePlayer(Player player) {
        Player updatedPlayer = playerRepository.save(player);
        // Update cache with JSON
        redisService.setJson(PLAYER_CACHE_PREFIX + player.getUserId(), 
                           updatedPlayer, 
                           CACHE_DURATION, 
                           TimeUnit.MINUTES);
        return updatedPlayer;
    }
    
    public void clearPlayerCache(String userId) {
        redisService.delete(PLAYER_CACHE_PREFIX + userId);
    }

    public void clearPlayerCache(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        List<String> keys = userIds.stream()
            .map(userId -> PLAYER_CACHE_PREFIX + userId)
                    .collect(Collectors.toList());
        
        redisService.deleteAll(keys);
    }
}