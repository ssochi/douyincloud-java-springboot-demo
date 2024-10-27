package com.bytedance.douyinclouddemo.constant;

/**
 * HTTP相关常量
 */
public class Constants {
    
    /**
     * HTTP Headers
     */
    public static class Headers {
        public static final String APP_ID = "X-TT-AppID";
        public static final String ROOM_ID = "X-Room-ID";
        public static final String ANCHOR_OPEN_ID = "X-Anchor-OpenID";
        public static final String AVATAR_URL = "X-Avatar-Url";
        public static final String NICK_NAME = "X-Nick-Name";
        public static final String MSG_TYPE = "x-msg-type";
        public static final String EVENT_TYPE = "x-tt-event-type";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String WS_OPENIDS = "X-TT-WS-OPENIDS";
    }

    /**
     * API URLs
     */
    public static class Urls {
        public static final String LIVE_DATA_TASK = "http://webcast.bytedance.com/api/live_data/task/start";
        public static final String WS_PUSH = "http://ws-push.dycloud-api.service/ws/live_interaction/push_data";
    }

    /**
     * Content Types
     */
    public static class ContentTypes {
        public static final String JSON = "application/json";
        public static final String JSON_UTF8 = "application/json; charset=utf-8";
    }

    /**
     * WebSocket Event Types
     */
    public static class WebSocketEvents {
        public static final String CONNECT = "connect";
        public static final String DISCONNECT = "disconnect";
        public static final String UPLINK = "uplink";
    }

    /**
     * Message Types
     */
    public static class MessageTypes {
        public static final String LIVE_LIKE = "live_like";
        public static final String LIVE_COMMENT = "live_comment";
        public static final String LIVE_GIFT = "live_gift";
        public static final String LIVE_FANSCLUB = "live_fansclub";
    }
}
