package com.bytedance.douyinclouddemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.bytedance.douyinclouddemo.model.Room;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RoomService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RankService rankService;

    // Redis key前缀常量
    private static final String ROOM_PLAYERS_KEY_PREFIX = "room:players:";      // 房间玩家集合的key前缀
    private static final String ROOM_CREATE_TIME_KEY_PREFIX = "room:create_time:"; // 房间创建时间的key前缀
    private static final String ROOM_UPDATE_TIME_KEY_PREFIX = "room:update_time:"; // 房间更新时间的key前缀
    private static final String USER_ROOM_KEY_PREFIX = "user_room:";            // 用户所在房间的key前缀

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 建房间或获取已存在的房间
     * 
     * @param roomID 房间ID
     * @return 房间信息，如果创建失败返回null
     * @throws IllegalArgumentException 当roomID为空时抛出
     */
    public Room createRoom(String roomID) {
        if (roomID == null || roomID.trim().isEmpty()) {
            throw new IllegalArgumentException("roomID cannot be null or empty");
        }

        String playersKey = ROOM_PLAYERS_KEY_PREFIX + roomID;
        String createTimeKey = ROOM_CREATE_TIME_KEY_PREFIX + roomID;
        String updateTimeKey = ROOM_UPDATE_TIME_KEY_PREFIX + roomID;
        
        // 如果房间已存在，直接返回房间信息
        if (Boolean.TRUE.equals(redisTemplate.hasKey(createTimeKey))) {
            return getRoomInfo(roomID);
        }

        try {
            long currentTime = System.currentTimeMillis();
            
            // 使用 redisTemplate.execute 来确保事务的原子性
            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    operations.opsForValue().set(createTimeKey, String.valueOf(currentTime));
                    operations.opsForValue().set(updateTimeKey, String.valueOf(currentTime));
                    return operations.exec();
                }
            });

            Room room = new Room();
            room.setAnchorOpenID(roomID);
            room.setPlayerList(new ArrayList<>());
            room.setCreatedAt(currentTime);
            room.setUpdatedAt(currentTime);
            log.info("create room " + roomID + " success");
            return room;
        } catch (Exception e) {
            log.error("Failed to create room for room: {}", roomID, e);
            return null;
        }
    }

    /**
     * 获取房间信息
     * 
     * @param roomID 房间ID
     * @return 房间信息，如果房间不存在返回null
     * @throws IllegalArgumentException 当roomID为空时抛出
     */
    public Room getRoomInfo(String roomID) {
        if (roomID == null || roomID.trim().isEmpty()) {
            throw new IllegalArgumentException("roomID cannot be null or empty");
        }

        try {
            String playersKey = ROOM_PLAYERS_KEY_PREFIX + roomID;
            String createTimeKey = ROOM_CREATE_TIME_KEY_PREFIX + roomID;
            String updateTimeKey = ROOM_UPDATE_TIME_KEY_PREFIX + roomID;

            Set<String> players = redisTemplate.opsForSet().members(playersKey);
            String createTime = redisTemplate.opsForValue().get(createTimeKey);
            String updateTime = redisTemplate.opsForValue().get(updateTimeKey);

            if (createTime == null) {
                return null;
            }

            Room room = new Room();
            room.setAnchorOpenID(roomID);
            room.setPlayerList(new ArrayList<>(players));
            room.setCreatedAt(Long.parseLong(createTime));
            room.setUpdatedAt(Long.parseLong(updateTime));

            return room;
        } catch (Exception e) {
            log.error("Failed to get room info for room: {}", roomID, e);
            return null;
        }
    }

    /**
     * 只删掉房间中的玩家信息
     * 
     * @param roomID 房间ID
     * @return 被关闭的房间信息，如果房间不存在返回null
     * @throws IllegalArgumentException 当roomID为空时抛出
     */
    public Room closeRoom(String roomID) {
        if (roomID == null || roomID.trim().isEmpty()) {
            throw new IllegalArgumentException("roomID cannot be null or empty");
        }

        try {
            Room room = getRoomInfo(roomID);
            if (room == null) {
                return null;
            }

            String playersKey = ROOM_PLAYERS_KEY_PREFIX + roomID;
            String createTimeKey = ROOM_CREATE_TIME_KEY_PREFIX + roomID;
            String updateTimeKey = ROOM_UPDATE_TIME_KEY_PREFIX + roomID;

            // 使用 SessionCallback 来确保事务的原子性
            redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    for (String userID : room.getPlayerList()) {
                        operations.delete(USER_ROOM_KEY_PREFIX + userID);
                    }
                    operations.delete(playersKey);
//                    operations.delete(createTimeKey);
//                    operations.delete(updateTimeKey);
                    return operations.exec();
                }
            });

            return room;
        } catch (Exception e) {
            log.error("Failed to close room for room: {}", roomID, e);
            return null;
        }
    }

    /**
     * 用户加入房间
     * 如果用户已在其他房间，会先退出原房间再加入新房间
     * 
     * @param userID 用户ID
     * @param roomID 房间ID
     * @return 用户之前所在的房间的ID，如果之前未在任何房间则返回null
     * @throws IllegalArgumentException 当参数为空时抛出
     */
    public String joinRoom(String userID, String roomID) {
        if (userID == null || userID.trim().isEmpty()) {
            throw new IllegalArgumentException("userID cannot be null or empty");
        }
        if (roomID == null || roomID.trim().isEmpty()) {
            throw new IllegalArgumentException("roomID cannot be null or empty");
        }

        try {
            String userRoomKey = USER_ROOM_KEY_PREFIX + userID;

            // Lua脚本确保操作的原子性
            String script = "local prevRoom = redis.call('get', KEYS[1]) " +
                           "if prevRoom then " +
                           "   redis.call('srem', KEYS[2] .. prevRoom, ARGV[1]) " +
                           "end " +
                           "redis.call('set', KEYS[1], ARGV[2]) " +
                           "redis.call('sadd', KEYS[2] .. ARGV[2], ARGV[1]) " +
                           "redis.call('set', KEYS[3] .. ARGV[2], ARGV[3]) " +
                           "return prevRoom";

            List<String> keys = Arrays.asList(
                userRoomKey, 
                ROOM_PLAYERS_KEY_PREFIX,
                ROOM_UPDATE_TIME_KEY_PREFIX
            );

            return redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, String.class),
                keys,
                userID,
                roomID,
                String.valueOf(System.currentTimeMillis())
            );
        } catch (Exception e) {
            log.error("Failed to join room. userID: {}, roomID: {}", userID, roomID, e);
            return null;
        }
    }
}
