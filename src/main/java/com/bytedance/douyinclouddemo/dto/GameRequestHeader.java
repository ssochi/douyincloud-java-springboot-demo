package com.bytedance.douyinclouddemo.dto;
import com.bytedance.douyinclouddemo.constant.Constants;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import lombok.Data;
import lombok.Builder;
@Data
@Builder
public class GameRequestHeader {
    private String appID;
    private String roomID;
    private String anchorOpenID;
    private String avatarUrl;
    private String nickName;

    /**
     * 从HttpServletRequest中提取header信息
     */
    public static GameRequestHeader from(HttpServletRequest request) {
        return GameRequestHeader.builder()
                .appID(request.getHeader(Constants.Headers.APP_ID))
                .roomID(request.getHeader(Constants.Headers.ROOM_ID))
                .anchorOpenID(request.getHeader(Constants.Headers.ANCHOR_OPEN_ID))
                .avatarUrl(request.getHeader(Constants.Headers.AVATAR_URL))
                .nickName(request.getHeader(Constants.Headers.NICK_NAME))
                .build();
    }

    /**
     * 验证必要的header是否存在
     */
    public void validate() {
        List<String> missingFields = new ArrayList<>();
        
        if (StringUtils.isEmpty(appID)) missingFields.add(Constants.Headers.APP_ID);
        if (StringUtils.isEmpty(roomID)) missingFields.add(Constants.Headers.ROOM_ID);
        if (StringUtils.isEmpty(anchorOpenID)) missingFields.add(Constants.Headers.ANCHOR_OPEN_ID);
        
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Missing required headers: " + String.join(", ", missingFields));
        }
    }
}
