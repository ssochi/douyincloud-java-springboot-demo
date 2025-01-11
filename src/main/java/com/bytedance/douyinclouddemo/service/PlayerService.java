package com.bytedance.douyinclouddemo.service;

import com.bytedance.douyinclouddemo.entity.Player;
import com.bytedance.douyinclouddemo.entity.PlayerExt;
import com.bytedance.douyinclouddemo.model.RankType;
import com.bytedance.douyinclouddemo.repository.PlayerRepository;
import com.bytedance.douyinclouddemo.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
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

        List<Player> players = new ArrayList<>();
        List<String> missingUserIds = new ArrayList<>();

        // Batch query Redis for all keys
        for (String userId : userIds) {
            Player player = redisService.getJson(PLAYER_CACHE_PREFIX + userId, Player.class);
            if (player != null) {
                players.add(player);
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
            players.addAll(dbPlayers);
        }

        return AssignRank(players);
    }

    @NotNull
    private List<Player> AssignRank(List<Player> players) {
        // Add rank information to all players
        Map<String, Integer> ranks = rankService.getPlayerRanks(
            players.stream()
                .map(Player::getUserId)
                .collect(Collectors.toList()),RankType.GLOBAL
        );
        Map<String, Integer> weekRanks = rankService.getPlayerRanks(
                players.stream()
                        .map(Player::getUserId)
                        .collect(Collectors.toList()),RankType.WEEKLY
        );

        for (Player player : players) {
            checkRankTimeRangeAndUpdate(player);
            if (player.getExt() == null) {
                player.setExt(new PlayerExt());
            }
            player.getExt().setRank(ranks.getOrDefault(player.getUserId(), 9999));
            player.getExt().setWeekRank(weekRanks.getOrDefault(player.getUserId(),9999));
        }

        return players;
    }

    private void checkRankTimeRangeAndUpdate(Player player){
        if (!DateUtils.isCurrentWeek(player.getExt().getLastWeekRankUpdateDate())){
            player.getExt().setWeekScore(0L);
            player.getExt().setWeekRank(9999);
            player.getExt().setLastRankUpdateDate(new Date());
        }

        if (!DateUtils.isCurrentMonth(player.getExt().getLastRankUpdateDate())){
            player.setScore(0L);
            player.getExt().setRank(9999);
            player.getExt().setLastRankUpdateDate(new Date());
        }
    }

    public List<Player> findByUserIdDirectly(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Player> players = playerRepository.findByUserIdIn(userIds);
        
        // Add rank information to all players
        return AssignRank(players);
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

    /**
     * Get top N players with their detailed information
     */
    public List<Player> getTopPlayersWithInfo(int n,RankType rankType) {
        try {

            // Extract userIds from tuples
            List<String> userIds = rankService.getTopPlayerID(n, rankType);

            // Get all players at once
            List<Player> players = findByUserId(userIds);

            // Create userId to Player map for ordering
            Map<String, Player> playerMap = players.stream()
                    .collect(Collectors.toMap(Player::getUserId, player -> player));

            // Return players in the same order as the ranking
            return userIds.stream()
                    .map(playerMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get top {} players with info", n, e);
            return new ArrayList<>();
        }
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