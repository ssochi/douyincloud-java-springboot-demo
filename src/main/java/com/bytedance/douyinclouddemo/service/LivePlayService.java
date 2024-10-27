package com.bytedance.douyinclouddemo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bytedance.douyinclouddemo.constant.Constants;
import com.bytedance.douyinclouddemo.dto.GameRequestHeader;
import com.bytedance.douyinclouddemo.model.LiveDataModel;
import com.bytedance.douyinclouddemo.model.LivePlayAPIResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class LivePlayService {
    
    private final OkHttpClient httpClient;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get(Constants.ContentTypes.JSON_UTF8);

    public LivePlayService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Autowired
    RoomService roomService;

    /**
     * 开始游戏，初始化直播间数据推送
     *
     * @param header 游戏请求头信息
     * @return 是否成功启动所有推送任务
     */
    public boolean startGame(GameRequestHeader header) {
        log.info("Starting game - appID: {}, roomID: {}, anchorOpenID: {}, avatarUrl: {}, nickName: {}", 
                header.getAppID(), header.getRoomID(), header.getAnchorOpenID(), 
                header.getAvatarUrl(), header.getNickName());

        List<String> msgTypeList = Arrays.asList(
            Constants.MessageTypes.LIVE_LIKE,
            Constants.MessageTypes.LIVE_COMMENT,
            Constants.MessageTypes.LIVE_GIFT,
            Constants.MessageTypes.LIVE_FANSCLUB
        );

        boolean allSuccess = true;
        for (String msgType : msgTypeList) {
            boolean result = startLiveDataTask(header.getAppID(), header.getRoomID(), msgType);
            if (result) {
                log.info("{} 推送开启成功", msgType);
            } else {
                log.error("{} 推送开启失败", msgType);
                allSuccess = false;
            }
        }

        try {
            roomService.createRoom(header.getRoomID());
        } catch (Exception e) {
            log.error("Failed to create room for anchor: {}", header.getAnchorOpenID(), e);
            allSuccess = false;
        }

        return allSuccess;
    }

    /**
     * 结束游戏，清理直播间数据推送
     *
     * @param header 游戏请求头信息
     * @return 是否成功结束所有推送任务
     */
    public boolean finishGame(GameRequestHeader header) {
        log.info("Finishing game - appID: {}, roomID: {}, anchorOpenID: {}", 
                header.getAppID(), header.getRoomID(), header.getAnchorOpenID());

        // 清理房间数据
        try {
            roomService.closeRoom(header.getRoomID());
        } catch (Exception e) {
            log.error("Failed to close room for anchor: {}", header.getAnchorOpenID(), e);
        }

        return true;
    }

    /**
     * 启动直播数据推送任务
     *
     * @param appID 应用ID
     * @param roomID 房间ID
     * @param msgType 消息类型
     * @return 是否成功启动推送任务
     */
    private boolean startLiveDataTask(String appID, String roomID, String msgType) {
        JSONObject requestBody = new JSONObject()
                .fluentPut("roomid", roomID)
                .fluentPut("appid", appID)
                .fluentPut("msg_type", msgType);

        Request request = new Request.Builder()
                .url(Constants.Urls.LIVE_DATA_TASK)
                .addHeader(Constants.Headers.CONTENT_TYPE, Constants.ContentTypes.JSON)
                .post(RequestBody.create(JSON_MEDIA_TYPE, requestBody.toString()))
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.error("开启推送任务失败, HTTP状态码: {}", response.code());
                return false;
            }

            LivePlayAPIResponse apiResponse = JSON.parseObject(
                    response.body().string(), 
                    LivePlayAPIResponse.class
            );

            if (apiResponse.getErrNo() != 0) {
                log.error("开启推送任务失败，错误信息: {}", apiResponse.getErrorMsg());
                return false;
            }

            return true;
        } catch (IOException e) {
            log.error("开启推送任务异常", e);
            return false;
        }
    }

    /**
     * 处理直播数据回调
     *
     * @param anchorOpenID 主播ID
     * @param msgType 消息类型
     * @param body 消息内容
     */
    public void handleLiveDataCallback(String anchorOpenID, String msgType, String body) {
        List<LiveDataModel> dataModels = JSON.parseArray(body, LiveDataModel.class);
        if (dataModels == null || dataModels.isEmpty()) {
            log.warn("Received empty live data callback");
            return;
        }

        dataModels.forEach(model -> 
            pushDataToClientByWebsocket(anchorOpenID, model.getMsgID(), msgType, body)
        );
    }

    /**
     * 通过WebSocket推送数据到客户端
     *
     * @param anchorOpenId 主播ID
     * @param msgID 消息ID
     * @param msgType 消息类型
     * @param data 消息数据
     */
    private void pushDataToClientByWebsocket(String anchorOpenId, String msgID, String msgType, String data) {
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("msg_id", msgID);
        bodyMap.put("msg_type", msgType);
        bodyMap.put("data", data);

        Request request = new Request.Builder()
                .url(Constants.Urls.WS_PUSH)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-TT-WS-OPENIDS", JSON.toJSONString(Collections.singletonList(anchorOpenId)))
                .post(okhttp3.RequestBody.create(JSON_MEDIA_TYPE, JSON.toJSONString(bodyMap)))
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.error("WebSocket push failed, status code: {}", response.code());
                return;
            }
            log.info("WebSocket push successful for msgID: {}", msgID);
        } catch (IOException e) {
            log.error("WebSocket push exception for msgID: " + msgID, e);
        }
    }

    /**
     * 处理WebSocket回调事件
     *
     * @param eventType 事件类型
     * @return 处理结果描述
     */
    public String handleWebsocketCallback(String eventType) {
        switch (eventType) {
            case Constants.WebSocketEvents.CONNECT:
                log.info("Client connected");
                return "Client connected successfully";
            case Constants.WebSocketEvents.DISCONNECT:
                log.info("Client disconnected");
                return "Client disconnected successfully";
            case Constants.WebSocketEvents.UPLINK:
                log.info("Received uplink message");
                return "Uplink message received";
            default:
                log.warn("Unknown event type: {}", eventType);
                return "Unknown event type";
        }
    }
}
