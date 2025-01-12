package com.bytedance.douyinclouddemo.controller;

import com.bytedance.douyinclouddemo.constant.Constants;
import com.bytedance.douyinclouddemo.dto.GameEndDTO;
import com.bytedance.douyinclouddemo.dto.GameRequestHeader;
import com.bytedance.douyinclouddemo.dto.GameResultDTO;
import com.bytedance.douyinclouddemo.model.JsonResponse;
import com.bytedance.douyinclouddemo.service.LivePlayService;
import com.bytedance.douyinclouddemo.service.RankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class LivePlayController {

    private final LivePlayService livePlayService;
    private final ObjectMapper objectMapper;  // 添加 ObjectMapper
    private final RankService rankService;  // Add RankService

    public LivePlayController(LivePlayService livePlayService, ObjectMapper objectMapper, RankService rankService) {
        this.livePlayService = livePlayService;
        this.objectMapper = objectMapper;
        this.rankService = rankService;
    }

    /**
     * 开始一局对战
     */
    @PostMapping("/start_game")
    public JsonResponse startGame(HttpServletRequest httpRequest) {
        log.info("enter start game");
        GameRequestHeader header = GameRequestHeader.from(httpRequest);
        header.validate();

        boolean success = livePlayService.startGame(header);

        JsonResponse response = new JsonResponse();
        if (success) {
            response.success("开始玩法对局成功");
        } else {
            response.failure("开始玩法对局失败");
        }
        return response;
    }

    /**
     * 开播
     */
    @PostMapping("/start_live")
    public JsonResponse startLive(HttpServletRequest httpRequest) {
        log.info("enter start live");
        GameRequestHeader header = GameRequestHeader.from(httpRequest);
        header.validate();

        boolean success = livePlayService.startLive(header);
        
        JsonResponse response = new JsonResponse();
        if (success) {
            response.success("开始玩法对局成功");
        } else {
            response.failure("开始玩法对局失败");
        }
        return response;
    }

    /**
     * 结束玩法
     */
    @PostMapping(path = "/finish_game")
    public JsonResponse finishGame(
            HttpServletRequest httpRequest,
            @RequestBody GameResultDTO gameResultDTO) {
        log.info("enter finish game with result: {}", gameResultDTO);
        GameRequestHeader header = GameRequestHeader.from(httpRequest);
        header.validate();
        // TODO 必须要做幂等，不然寄
        JsonResponse resp = new JsonResponse();

        try {
            // Then process game results
            GameEndDTO gameEndDTO = livePlayService.processGameEnd(header.getAnchorOpenID(), gameResultDTO);
            log.info("finish game result: {}",gameEndDTO);
            // finish the game in LivePlayService
            boolean success = livePlayService.finishGame(header);
            if (!success) {
                resp.failure("结束玩法失败");
                return resp;
            }
            // Convert GameEndDTO to JSON string
            String gameEndJson = objectMapper.writeValueAsString(gameEndDTO);

            JsonResponse response = resp;
            response.success("结束玩法成功");
            response.setData(gameEndJson);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to finish game", e);
            resp.failure("结束玩法失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 直播数据回调接口
     */
    @PostMapping("/live_data_callback")
    public JsonResponse liveDataCallback(
            @RequestHeader(Constants.Headers.ANCHOR_OPEN_ID) String anchorOpenID,
            @RequestHeader(Constants.Headers.MSG_TYPE) String msgType,
            @RequestBody String body) {
        log.info("enter live data callback, anchorOpenID: {}, msgType: {}, body: {}",
                anchorOpenID, msgType, body);
        livePlayService.handleLiveDataCallback(anchorOpenID, msgType, body);

        JsonResponse response = new JsonResponse();
        response.success("success");
        return response;
    }

    /**
     * WebSocket回调接口
     */
    @RequestMapping(path = "/websocket_callback", method = {RequestMethod.POST, RequestMethod.GET})
    public JsonResponse websocketCallback(HttpServletRequest request) {
        String eventType = request.getHeader(Constants.Headers.EVENT_TYPE);
        log.info("enter websocket data callback, type: {}",eventType);
        String result = livePlayService.handleWebsocketCallback(eventType);
        
        JsonResponse response = new JsonResponse();
        response.success(result);
        return response;
    }

    /**
     * 获取游戏公告
     */
    @GetMapping("/game/announcement")
    public JsonResponse getGameAnnouncement() {
        log.info("Getting game announcement");
        JsonResponse response = new JsonResponse();
        
        try {
            String announcement = rankService.getAnnouncement();
            response.success(announcement != null ? announcement : "");
        } catch (Exception e) {
            log.error("Failed to get game announcement", e);
            response.failure("获取游戏公告失败");
        }
        return response;
    }

    /**
     * 检查客户端版本是否可用
     * @param version 客户端版本号
     */
    @GetMapping("/game/check_version")
    public JsonResponse checkVersion(@RequestParam int version) {
        log.info("Checking client version: {}", version);
        JsonResponse response = new JsonResponse();
        
        try {
            int minVersion = rankService.getMinVersion();
            boolean isCompatible = version >= minVersion;
            
            if (isCompatible) {
                response.success("版本检查通过");
            } else {
                response.failure("当前版本过低，请更新到最新版本");
                response.setData(String.valueOf(minVersion));  // 返回所需的最低版本号
            }
        } catch (Exception e) {
            log.error("Failed to check version compatibility", e);
            response.failure("版本检查失败");
        }
        return response;
    }
}
