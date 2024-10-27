package com.bytedance.douyinclouddemo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GameResultDTO {
    Map<String,Long> scoreMap;
}
