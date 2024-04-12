package com.bytedance.douyinclouddemo.controller;

import com.bytedance.douyinclouddemo.model.JsonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 抖音云x弹幕玩法的服务端demo展示
 */
@RestController
@Slf4j
public class LivePlayDemoController {

    /**
     * 抖音云unity_sdk的callContainer的服务端demo
     */
    @PostMapping(path = "/call_container_example")
    public JsonResponse callContainerExample(HttpServletRequest httpRequest) {
        // 开发者可以直接通过请求头获取直播间信息,无需自行通过token置换
        String roomID = httpRequest.getHeader("X-Room-ID");
        String anchorOpenID = httpRequest.getHeader("X-Anchor-OpenID");
        String avatarUrl = httpRequest.getHeader("X-Avatar-Url");
        String nickName = httpRequest.getHeader("X-Nick-Name");
        log.info("roomID: {}, anchorOpenID: {}, avatarUrl: {}, nickName: {}", roomID, anchorOpenID, avatarUrl, nickName);

        // TODO: 开发者自行实现业务逻辑

        JsonResponse response = new JsonResponse();
        response.success("success");
        return response;
    }

    /**
     * 通过抖音云服务接受直播间数据，内网专线加速+免域名备案
     * 通过内网专线会自动携带X-Anchor-OpenID字段
     * ref: <a href="https://developer.open-douyin.com/docs/resource/zh-CN/developer/tools/cloud/develop-guide/danmu-callback">...</a>
     */
    @PostMapping(path = "/live_data_callback_example")
    public JsonResponse liveDataCallbackExample(@RequestHeader("X-Anchor-OpenID") String anchorOpenID, @RequestBody String body) {

        // TODO: 开发者业务自行处理

        // 需要将直播间数据推送到主播端,这里使用抖音云websocket能力推送
        pushDataToClientByDouyinCloudWebsocket();

        JsonResponse response = new JsonResponse();
        response.success("success");
        return response;
    }

    /**
     * 通过抖音云服务访问直播小玩法openAPI，内网加速+免鉴权+免access_token维护
     */
    @PostMapping(path = "/openapi_example")
    public JsonResponse openAPIExample() {
        // TODO: 访问小玩法openAPI，仅需使用http协议，无需传递access_token参数
        JsonResponse response = new JsonResponse();
        response.success("success");
        return response;
    }


    //---------------- 抖音云websocket相关demo ---------------------

    /**
     * 抖音云websocket监听的回调函数,客户端建连/上行发消息都会走到该HTTP回调函数中
     * ref: <a href="https://developer.open-douyin.com/docs/resource/zh-CN/developer/tools/cloud/develop-guide/websocket-guide/websocket#%E5%BB%BA%E8%BF%9E%E8%AF%B7%E6%B1%82">...</a>
     */
    @RequestMapping(path = "/douyincloud/websocket_callback", method = {RequestMethod.POST, RequestMethod.GET})
    public JsonResponse websocketCallback(HttpServletRequest request) {
        String eventType = request.getHeader("x-tt-event-type");
        switch (eventType) {
            case "connect":
                // 客户端建连
            case "disconnect": {
                // 客户端断连
            }
            case "uplink": {
                // 客户端上行发消息
            }
            default:
                break;
        }
        JsonResponse response = new JsonResponse();
        response.success("success");
        return response;
    }

    /**
     * 使用抖音云websocket网关,将数据推送到主播端
     * ref: <a href="https://developer.open-douyin.com/docs/resource/zh-CN/developer/tools/cloud/develop-guide/websocket-guide/websocket#%E4%B8%8B%E8%A1%8C%E6%B6%88%E6%81%AF%E6%8E%A8%E9%80%81">...</a>
     */
    private void pushDataToClientByDouyinCloudWebsocket() {
        // TODO: 这里通过HTTP POST请求将数据推送给抖音云网关,进而抖音云网关推送给主播端
    }
}
