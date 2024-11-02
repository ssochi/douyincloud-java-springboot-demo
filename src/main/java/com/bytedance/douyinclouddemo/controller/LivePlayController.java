package com.bytedance.douyinclouddemo.controller;

import com.bytedance.douyinclouddemo.constant.Constants;
import com.bytedance.douyinclouddemo.dto.GameRequestHeader;
import com.bytedance.douyinclouddemo.model.JsonResponse;
import com.bytedance.douyinclouddemo.service.LivePlayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class LivePlayController {

    private final LivePlayService livePlayService;

    public LivePlayController(LivePlayService livePlayService) {
        this.livePlayService = livePlayService;
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
    public JsonResponse finishGame(HttpServletRequest httpRequest) {
        log.info("enter finish game");
        GameRequestHeader header = GameRequestHeader.from(httpRequest);
        header.validate();

        boolean success = livePlayService.finishGame(header);
        
        JsonResponse response = new JsonResponse();
        if (success) {
            response.success("结束玩法成功");
        } else {
            response.failure("结束玩法失败");
        }
        return response;
    }

    /**
     * 直播数据回调接口
     */
    @PostMapping("/live_data_callback")
    public JsonResponse liveDataCallback(
            @RequestHeader(Constants.Headers.ANCHOR_OPEN_ID) String anchorOpenID,
            @RequestHeader(Constants.Headers.MSG_TYPE) String msgType,
            @RequestBody String body) {
        log.info("enter live data callback");
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
