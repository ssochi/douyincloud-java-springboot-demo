package com.bytedance.douyinclouddemo.service;

import com.bytedance.douyinclouddemo.dto.GameEndDTO;
import com.bytedance.douyinclouddemo.dto.GameResultDTO;
import com.bytedance.douyinclouddemo.dto.PlayerEndInfo;
import com.bytedance.douyinclouddemo.entity.Player;
import com.bytedance.douyinclouddemo.model.Room;
import com.bytedance.douyinclouddemo.repository.PlayerRepository;
import com.bytedance.douyinclouddemo.utils.KVPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    RoomService roomService;
    @Autowired
    RankService rankService;

    /**
     * 玩家缓存的Redis键前缀
     */
    private static final String PLAYER_CACHE_PREFIX = "player:";


    public GameEndDTO endGame(String roomID, GameResultDTO gameResultDTO) {
        validateEndGameParams(roomID, gameResultDTO);
        Room room = getRoomAndValidate(roomID);
        List<String> quitPlayerList = getQuitPlayerList(room, gameResultDTO);
        List<Player> players = getAndUpdatePlayers(gameResultDTO);
        return processEndGame(roomID, players, quitPlayerList, gameResultDTO);
    }

    private void validateEndGameParams(String roomID, GameResultDTO gameResultDTO) {
        if (roomID == null || roomID.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be null or empty");
        }
        if (gameResultDTO == null) {
            throw new IllegalArgumentException("Game result cannot be null");
        }
    }

    private Room getRoomAndValidate(String roomID) {
        Room room = roomService.getRoomInfo(roomID);
        if (room == null) {
            throw new IllegalStateException("Room not found: " + roomID);
        }
        return room;
    }

    private List<String> getQuitPlayerList(Room room, GameResultDTO gameResultDTO) {
        Set<String> roomPlayers = new HashSet<>(room.getPlayerList());
        List<String> quitPlayerList = new ArrayList<>();
        
        for (String playerId : roomPlayers) {
            if (!gameResultDTO.getScoreMap().containsKey(playerId)) {
                quitPlayerList.add(playerId);
                log.info("Player {} quit during the game", playerId);
            }
        }
        return quitPlayerList;
    }

    private List<Player> getAndUpdatePlayers(GameResultDTO gameResultDTO) {
        List<Player> players = playerRepository.findAllById(
            gameResultDTO.getScoreMap().keySet().stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList())
        );

        for (Player player : players) {
            updatePlayerStats(player, gameResultDTO.getScoreMap().get(player.getUserId()));
        }
        return players;
    }

    private void updatePlayerStats(Player player, Long score) {
        if (score != null) {
            Long gloryGained = score / 150;
            
            player.setScore(player.getScore() + score);
            player.setGlory(player.getGlory() + gloryGained);
            player.setGameCount(player.getGameCount() + 1);
            
            rankService.updatePlayerScore(player.getUserId(), score);
            
            log.info("Player {} earned score: {} and glory: {}", 
                    player.getUserId(), score, gloryGained);
        }
    }

    private GameEndDTO processEndGame(String roomID, List<Player> players, List<String> quitPlayerList, GameResultDTO gameResultDTO) {
        try {
            // Update database
            playerRepository.batchUpdatePlayers(players);
            
            // Get initial rankings for calculating rank changes
            Map<String, Integer> initialRanks = rankService.getPlayerRanks(
                players.stream().map(Player::getUserId).collect(Collectors.toList())
            );
            
            // Create PlayerEndInfo list and clear cache
            List<PlayerEndInfo> playerEndInfos = new ArrayList<>();
            
            // Process active players
            for (Player player : players) {
                // Clear cache
                redis.delete(PLAYER_CACHE_PREFIX + player.getUserId());
                
                // Get score from gameResultDTO
                Long deltaScore = gameResultDTO.getScoreMap().get(player.getUserId());
                Long deltaGlory = deltaScore != null ? deltaScore / 150 : 0L;
                
                // Get rank change
                int currentRank = rankService.getPlayerRank(player.getUserId());
                int deltaRank = initialRanks.get(player.getUserId()) - currentRank;
                
                playerEndInfos.add(PlayerEndInfo.builder()
                        .player(player)
                        .deltaScore(deltaScore)
                        .deltaGlory(deltaGlory)
                        .deltaRank(deltaRank)
                        .isQuit(false)
                        .build());
            }
            
            // Process quit players
            for (String quitPlayerId : quitPlayerList) {
                Player quitPlayer = playerRepository.findByUserId(quitPlayerId).orElse(null);
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
            
            List<Player> totalRankTop = getTopPlayers();
            roomService.closeRoom(roomID);
            
            log.info("Game ended successfully for room {}", roomID);

            return GameEndDTO.builder()
                    .totalRankTop(totalRankTop)
                    .playerEndInfos(playerEndInfos)
                    .build();

        } catch (Exception e) {
            log.error("Failed to end game for room {}", roomID, e);
            throw new RuntimeException("Failed to end game", e);
        }
    }

    private List<Player> getTopPlayers() {
        List<KVPair<String, Double>> topRankings = rankService.getTopPlayers(10);
        return playerRepository.findAllById(
            topRankings.stream()
                .map(entry -> Integer.parseInt(entry.getKey()))
                .collect(Collectors.toList())
        );
    }

    /**
     * 处理玩家加入房间
     * 
     * @param player 要加入的玩家对象
     * @param roomID 目标房间ID
     * @throws IllegalArgumentException 如果玩家或房间ID为空/null
     */
    public void Join(Player player, String roomID) {
        if (player == null || roomID == null || roomID.trim().isEmpty()) {
            throw new IllegalArgumentException("Player and room ID cannot be empty or null");
        }

        // 获取之前的房间（如果存在）
        String previousRoom = roomService.joinRoom(player.getUserId(), roomID);

        // 如果玩家在另一个房间，处理转换
        if (previousRoom != null && !previousRoom.equals(roomID)) {
            log.info("Player {} left room {} and joined room {}", 
                    player.getUserId(), previousRoom, roomID);
            // 未来实现：通知之前的房间玩家离开
        }

        log.info("Player {} joined room {}", player.getUserId(), roomID);
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
            // 更新玩家对象中的荣耀
            player.setGlory(player.getGlory() - glory);
            
            // 更新数据库
            playerRepository.save(player);
            
            // 从缓存中移除以强制刷新
            redis.delete(PLAYER_CACHE_PREFIX + player.getUserId());
            
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
