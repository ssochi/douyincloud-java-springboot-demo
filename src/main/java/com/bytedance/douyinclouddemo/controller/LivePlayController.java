package com.bytedance.douyinclouddemo.controller;

import com.bytedance.douyinclouddemo.constant.Constants;
import com.bytedance.douyinclouddemo.dto.GameEndDTO;
import com.bytedance.douyinclouddemo.dto.GameRequestHeader;
import com.bytedance.douyinclouddemo.dto.GameResultDTO;
import com.bytedance.douyinclouddemo.model.JsonResponse;
import com.bytedance.douyinclouddemo.service.LivePlayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class LivePlayController {

    private final LivePlayService livePlayService;
    private final ObjectMapper objectMapper;  // 添加 ObjectMapper
    public LivePlayController(LivePlayService livePlayService, ObjectMapper objectMapper) {
        this.livePlayService = livePlayService;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始玩法对局
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
     * 结束玩法
     */
    @PostMapping(path = "/finish_game")
    public JsonResponse finishGame(
            HttpServletRequest httpRequest,
            @RequestBody GameResultDTO gameResultDTO) {
        log.info("enter finish game with result: {}", gameResultDTO);
        GameRequestHeader header = GameRequestHeader.from(httpRequest);
        header.validate();

        JsonResponse resp = new JsonResponse();

        try {
            // Then process game results
            GameEndDTO gameEndDTO = livePlayService.processGameEnd(header.getAnchorOpenID(), gameResultDTO);
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
        log.info("enter websocket data callback");
        String eventType = request.getHeader(Constants.Headers.EVENT_TYPE);
        String result = livePlayService.handleWebsocketCallback(eventType);
        
        JsonResponse response = new JsonResponse();
        response.success(result);
        return response;
    }
}
