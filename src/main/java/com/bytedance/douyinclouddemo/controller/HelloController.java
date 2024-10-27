package com.bytedance.douyinclouddemo.controller;

import com.bytedance.douyinclouddemo.model.JsonResponse;
import com.bytedance.douyinclouddemo.model.TextAntidirt;
import com.bytedance.douyinclouddemo.model.TextAntidirtRequest;
import com.bytedance.douyinclouddemo.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
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
        JsonResponse response = new JsonResponse();
        Object value = redisService.get(key);
        if (value != null) {
            response.success(value.toString());
        } else {
            response.failure("Key not found in Redis");
        }
        return response;
    }
}
