package com.bytedance.douyinclouddemo.controller;

import com.bytedance.douyinclouddemo.entity.Player;
import com.bytedance.douyinclouddemo.model.JsonResponse;
import com.bytedance.douyinclouddemo.model.Room;
import com.bytedance.douyinclouddemo.model.TextAntidirt;
import com.bytedance.douyinclouddemo.model.TextAntidirtRequest;
import com.bytedance.douyinclouddemo.service.PlayerService;
import com.bytedance.douyinclouddemo.service.RedisService;
import com.bytedance.douyinclouddemo.service.RoomService;
import com.bytedance.douyinclouddemo.service.RankService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.bytedance.douyinclouddemo.utils.KVPair;
import com.bytedance.douyinclouddemo.dto.RankPlayerDTO;

@RestController
@Slf4j
public class HelloController {

    @GetMapping("/api/get_open_id")
    public JsonResponse getOpenID(@RequestHeader("X-TT-OPENID") String openID) {
        JsonResponse response = new JsonResponse();
        if(openID.isEmpty()){
            response.failure("openid is empty");
        }else{
            response.success(openID);
        }
        return response;
    }

    @PostMapping("/api/text/antidirt")
    public JsonResponse textAntidirt(@RequestBody TextAntidirtRequest textAntidirtRequest) throws JsonProcessingException {

        TextAntidirt textAntidirt = new TextAntidirt(textAntidirtRequest.getContent());

        ObjectMapper objectMapper = new ObjectMapper();

        String jsonString = objectMapper.writeValueAsString(textAntidirt);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonString, headers);

        String url = "http://developer.toutiao.com/api/v2/tags/text/antidirt";
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        String responseBody = responseEntity.getBody();
        JsonResponse response = new JsonResponse();
        response.success(responseBody);
        return response;
    }
    @Autowired
    private RedisService redisService;

    @PostMapping("/api/redis/write")
    public JsonResponse writeToRedis(@RequestParam String key, @RequestParam String value) {
        JsonResponse response = new JsonResponse();
        boolean result = redisService.set(key, value);
        if (result) {
            response.success("Successfully wrote to Redis");
        } else {
            response.failure("Failed to write to Redis");
        }
        return response;
    }

    @GetMapping("/api/redis/read")
    public JsonResponse readFromRedis(@RequestParam String key) {
        log.info("enter api redis read");
        JsonResponse response = new JsonResponse();
        Object value = redisService.get(key);
        if (value != null) {
            response.success(value.toString());
        } else {
            response.failure("Key not found in Redis");
        }
        return response;
    }

    @Autowired
    private PlayerService playerService;
    
    @Autowired
    private RoomService roomService;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RankService rankService;

    /**
     * 查询玩家信息
     */
    @GetMapping("/api/player/{userId}")
    public JsonResponse getPlayer(@PathVariable String userId) {
        log.info("Getting player info for userId: {}", userId);
        JsonResponse response = new JsonResponse();
        
        try {
            Player player = playerService.findByUserId(userId);
            if (player != null) {
                response.success(objectMapper.writeValueAsString(player));
            } else {
                response.failure("Player not found");
            }
        } catch (Exception e) {
            log.error("Failed to get player info for userId: {}", userId, e);
            response.failure("Error getting player info: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 查询房间信息
     */
    @GetMapping("/api/room/{roomId}")
    public JsonResponse getRoom(@PathVariable String roomId) {
        log.info("Getting room info for roomId: {}", roomId);
        JsonResponse response = new JsonResponse();
        
        try {
            Room room = roomService.getRoomInfo(roomId);
            if (room != null) {
                response.success(objectMapper.writeValueAsString(room));
            } else {
                response.failure("Room not found");
            }
        } catch (Exception e) {
            log.error("Failed to get room info for roomId: {}", roomId, e);
            response.failure("Error getting room info: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 删除房间
     */
    @DeleteMapping("/api/room/{roomId}")
    public JsonResponse deleteRoom(@PathVariable String roomId) {
        log.info("Deleting room: {}", roomId);
        JsonResponse response = new JsonResponse();
        
        try {
            Room closedRoom = roomService.closeRoom(roomId);
            if (closedRoom != null) {
                response.success("Room successfully closed");
            } else {
                response.failure("Room not found or already closed");
            }
        } catch (Exception e) {
            log.error("Failed to close room: {}", roomId, e);
            response.failure("Error closing room: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取排行榜前N名玩家
     */
    @GetMapping("/api/rank/top/{n}")
    public JsonResponse getTopPlayers(@PathVariable int n) {
        log.info("Getting top {} players from ranking", n);
        JsonResponse response = new JsonResponse();
        
        try {
            List<Player> topPlayers = rankService.getTopPlayersWithInfo(n);
            response.success(objectMapper.writeValueAsString(topPlayers));
        } catch (Exception e) {
            log.error("Failed to get top players", e);
            response.failure("Error getting top players: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 重置排行榜
     */
    @DeleteMapping("/api/rank/reset")
    public JsonResponse resetRankings() {
        log.info("Resetting rankings");
        JsonResponse response = new JsonResponse();
        
        try {
            rankService.resetRankings();
            response.success("Rankings reset successfully");
        } catch (Exception e) {
            log.error("Failed to reset rankings", e);
            response.failure("Error resetting rankings: " + e.getMessage());
        }
        
        return response;
    }
}
