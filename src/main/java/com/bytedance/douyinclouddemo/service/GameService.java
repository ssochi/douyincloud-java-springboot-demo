package com.bytedance.douyinclouddemo.service;

import com.bytedance.douyinclouddemo.dto.GameEndDTO;
import com.bytedance.douyinclouddemo.dto.GameResultDTO;
import com.bytedance.douyinclouddemo.dto.PlayerEndInfo;
import com.bytedance.douyinclouddemo.entity.Player;
import com.bytedance.douyinclouddemo.model.LiveCommentModel;
import com.bytedance.douyinclouddemo.model.RankType;
import com.bytedance.douyinclouddemo.model.Room;
import com.bytedance.douyinclouddemo.repository.PlayerRepository;
import com.bytedance.douyinclouddemo.utils.KVPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理游戏相关操作的服务类
 */
@Service
@Slf4j
public class GameService {

    @Autowired
    RedisService redis;
    @Autowired
    PlayerRepository playerRepository;
    @Autowired
    PlayerService playerService;
    @Autowired
    RoomService roomService;
    @Autowired
    RankService rankService;

    /**
     * 玩家缓存的Redis键前缀
     */
    private static final String PLAYER_CACHE_PREFIX = "player:";


    public GameEndDTO endGame(String anchorOpenID, GameResultDTO gameResultDTO) {
        validateEndGameParams(anchorOpenID, gameResultDTO);
        Room room = getRoomAndValidate(anchorOpenID);
        List<String> quitPlayerList = getQuitPlayerList(room, gameResultDTO);
        log.info("quit player : " + quitPlayerList);

        for (String playerID : quitPlayerList) {
            gameResultDTO.getScoreMap().remove(playerID);
        }

        List<Player> players = playerService.findByUserIdDirectly(new ArrayList<>(gameResultDTO.getScoreMap().keySet()));
        return processEndGame(anchorOpenID, players, quitPlayerList, gameResultDTO);
    }

    private void validateEndGameParams(String anchorOpenID, GameResultDTO gameResultDTO) {
        if (anchorOpenID == null || anchorOpenID.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be null or empty");
        }
        if (gameResultDTO == null) {
            throw new IllegalArgumentException("Game result cannot be null");
        }
    }

    private Room getRoomAndValidate(String anchorOpenID) {
        Room room = roomService.getRoomInfo(anchorOpenID);
        if (room == null) {
            throw new IllegalStateException("Room not found: " + anchorOpenID);
        }
        return room;
    }

    private List<String> getQuitPlayerList(Room room, GameResultDTO gameResultDTO) {
        Set<String> roomPlayers = new HashSet<>(room.getPlayerList());
        List<String> quitPlayerList = new ArrayList<>();

        gameResultDTO.getScoreMap().forEach((playerID,score) -> {
            if(!roomPlayers.contains(playerID)){
                quitPlayerList.add(playerID);
                log.info("Player {} quit during the game", playerID);
            }
        });
        return quitPlayerList;
    }

    private List<Player> getAndUpdatePlayers(GameResultDTO gameResultDTO) {
        List<Player> players = playerService.findByUserIdDirectly(new ArrayList<>(gameResultDTO.getScoreMap().keySet()));

        for (Player player : players) {
            updatePlayerStats(player, gameResultDTO.getScoreMap().get(player.getUserId()));
        }
        return players;
    }

    private void updatePlayerStats(Player player, Long score) {
        if (score != null) {
            Long gloryGained = score / 150;
            
            player.setScore(player.getScore() + score);
            player.getExt().setWeekScore(player.getExt().getWeekScore() + score);
            player.getExt().setLastRankUpdateDate(new Date());
            player.getExt().setLastWeekRankUpdateDate(new Date());
            player.setGlory(player.getGlory() + gloryGained);
            player.setGameCount(player.getGameCount() + 1);
            
            rankService.updatePlayerScore(player.getUserId(), player.getScore(), RankType.GLOBAL);
            rankService.updatePlayerScore(player.getUserId(), player.getExt().getWeekScore(), RankType.WEEKLY);
            
            log.info("Player {} earned score: {} and glory: {}", 
                    player.getUserId(), score, gloryGained);
        }
    }

    private GameEndDTO processEndGame(String anchorOpenID, List<Player> players, List<String> quitPlayerList, GameResultDTO gameResultDTO) {
        try {
            for (Player player : players) {
                updatePlayerStats(player, gameResultDTO.getScoreMap().get(player.getUserId()));
            }
            // Update database
            playerRepository.batchUpdatePlayers(players);
            playerService.clearPlayerCache(players.stream().map(Player::getUserId).collect(Collectors.toList()));

            // Get initial rankings for calculating rank changes
            Map<String, Integer> initialRanks = rankService.getPlayerRanks(
                players.stream().map(Player::getUserId).collect(Collectors.toList()),RankType.GLOBAL
            );
            Map<String, Integer> initialWeekRanks = rankService.getPlayerRanks(
                    players.stream().map(Player::getUserId).collect(Collectors.toList()),RankType.WEEKLY
            );
            
            // Create PlayerEndInfo list and clear cache
            List<PlayerEndInfo> playerEndInfos = new ArrayList<>();
            
            // Process active players
            for (Player player : players) {
                // Get score from gameResultDTO
                Long deltaScore = gameResultDTO.getScoreMap().get(player.getUserId());
                Long deltaGlory = deltaScore != null ? deltaScore / 150 : 0L;
                
                // Get rank change
                int currentRank = rankService.getPlayerRank(player.getUserId(),RankType.GLOBAL);
                int currentWeekRank = rankService.getPlayerRank(player.getUserId(),RankType.WEEKLY);
                int deltaRank = initialRanks.get(player.getUserId()) - currentRank;
                int deltaWeekRank = initialWeekRanks.get(player.getUserId()) - currentWeekRank;
                
                playerEndInfos.add(PlayerEndInfo.builder()
                        .player(player)
                        .deltaScore(deltaScore)
                        .deltaWeekScore(deltaScore)
                        .deltaGlory(deltaGlory)
                        .deltaRank(deltaRank)
                        .deltaWeekRank(deltaWeekRank)
                        .isQuit(false)
                        .build());
            }


            List<Player> quitPlayers = playerService.findByUserId(quitPlayerList);
            for (Player quitPlayer : quitPlayers) {
                if (quitPlayer != null) {
                    playerEndInfos.add(PlayerEndInfo.builder()
                            .player(quitPlayer)
                            .deltaScore(0L)
                            .deltaGlory(0L)
                            .deltaRank(0)
                            .isQuit(true)
                            .build());
                }
            }

            List<Player> totalRankTop = playerService.getTopPlayersWithInfo(20,RankType.GLOBAL);
            List<Player> weekRankTop = playerService.getTopPlayersWithInfo(20,RankType.WEEKLY);
            roomService.closeRoom(anchorOpenID);
            
            log.info("Game ended successfully for room {}", anchorOpenID);

            return GameEndDTO.builder()
                    .totalRankTop(totalRankTop)
                    .weekRankTop(weekRankTop)
                    .playerEndInfos(playerEndInfos)
                    .build();

        } catch (Exception e) {
            log.error("Failed to end game for room {}", anchorOpenID, e);
            throw new RuntimeException("Failed to end game", e);
        }
    }


    /**
     * 处理玩家加入房间
     * 
     * @param anchorOpenID 目标房间ID
     * @param comment 评论消息
     * @throws IllegalArgumentException 如果玩家或房间ID为空/null
     */
    public Player Join(String anchorOpenID, LiveCommentModel comment) {

        Player player = playerService.findByUserId(comment.getSecOpenid());
        if (player == null) {
            player = new Player();
            player.setUserId(comment.getSecOpenid());
            player.setAvatarUrl(comment.getAvatarUrl());
            player.setUserName(comment.getNickname());
            player.setCreatedAt(LocalDateTime.now());
            playerService.createPlayer(player);
        }

        if (anchorOpenID == null || anchorOpenID.trim().isEmpty()) {
            throw new IllegalArgumentException("Player and room ID cannot be empty or null");
        }

        // 获取之前的房间（如果存在）
        String previousRoom = roomService.joinRoom(player.getUserId(), anchorOpenID);

        // 如果玩家在另一个房间，处理转换
        if (previousRoom != null && !previousRoom.equals(anchorOpenID)) {
            log.info("Player {} left room {} and joined room {}", 
                    player.getUserId(), previousRoom, anchorOpenID);
            // 未来实现：通知之前的房间玩家离开
        }

        log.info("Player {} joined room {}", player.getUserId(), anchorOpenID);
        return player;
    }

    /**
     * 处理玩家的荣耀消耗
     * 
     * @param player 想要使用荣耀的玩家
     * @param glory 要使用的荣耀数量
     * @return 如果荣耀成功使用返回true，否则返回false
     */
    public boolean UseGlory(Player player, int glory) {
        if (player == null || glory <= 0) {
            return false;
        }

        // 检查玩家是否有足够的荣耀
        if (player.getGlory() < glory) {
            log.warn("Player {} does not have enough glory. Required: {}, Available: {}", 
                    player.getUserId(), glory, player.getGlory());
            return false;
        }

        try {
            player.setGlory(player.getGlory() - glory);
            playerService.updatePlayer(player);
            log.info("Successfully used {} glory for player {}", glory, player.getUserId());
            return true;
        } catch (Exception e) {
            log.error("Failed to use glory for player {}", player.getUserId(), e);
            return false;
        }
    }

    /**
     * 根据提供的信息检索或创建玩家
     * 
     * @param userID 用户标识符
     * @param userName 用户显示名称
     * @param avatarURL 用户头像URL
     * @return 从缓存/数据库获取或新创建的玩家对象
     * @throws IllegalArgumentException 如果userID为null或空
     */
    public Player GetPlayerOrCreate(String userID, String userName, String avatarURL) {
        if (userID == null || userID.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        // 尝试从Redis获取
        String cacheKey = PLAYER_CACHE_PREFIX + userID;
        Object cachedPlayer = redis.get(cacheKey);
        if (cachedPlayer instanceof Player) {
            log.debug("Found player {} in cache", userID);
            return (Player) cachedPlayer;
        }

        // 尝试从数据库获取
        Optional<Player> playerOpt = playerRepository.findByUserId(userID);
        Player player;

        if (playerOpt.isPresent()) {
            player = playerOpt.get();
            // 如果提供了信息，则更新玩家信息
            if (userName != null && !userName.isEmpty()) {
                player.setUserName(userName);
            }
            if (avatarURL != null && !avatarURL.isEmpty()) {
                player.setAvatarUrl(avatarURL);
            }
            player = playerRepository.save(player);
        } else {
            // 创建新玩家
            player = new Player();
            player.setUserId(userID);
            player.setUserName(userName != null ? userName : "Player_" + userID);
            player.setAvatarUrl(avatarURL != null ? avatarURL : "");
            player = playerRepository.save(player);
            log.info("Created new player with ID: {}", player.getUserId());
        }

        // 更新缓存
        redis.set(cacheKey, player);
        
        return player;
    }
}
