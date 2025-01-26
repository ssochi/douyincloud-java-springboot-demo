package com.bytedance.douyinclouddemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String LAST_WEEKLY_RESET_KEY = "rank:last_weekly_reset";
    private static final String LAST_MONTHLY_RESET_KEY = "rank:last_monthly_reset";

    /**
     * Set key-value with expiration
     */
    public boolean set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Redis set error: ", e);
            return false;
        }
    }

    /**
     * Set key-value without expiration
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis set error: ", e);
            return false;
        }
    }

    /**
     * Get value by key
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get error: ", e);
            return null;
        }
    }

    /**
     * Delete key
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.error("Redis delete error: ", e);
            return false;
        }
    }

    /**
     * Check if key exists
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis hasKey error: ", e);
            return false;
        }
    }

    /**
     * Increment value
     */
    public long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis increment error: ", e);
            return 0;
        }
    }

    /**
     * Set expiration for key
     */
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
        } catch (Exception e) {
            log.error("Redis expire error: ", e);
            return false;
        }
    }

    /**
     * Execute a Lua script
     */
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        try {
            return redisTemplate.execute(script, keys, args);
        } catch (Exception e) {
            log.error("Redis script execution error: ", e);
            return null;
        }
    }

    /**
     * Set object as JSON string
     */
    public boolean setJson(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Redis setJson error: ", e);
            return false;
        }
    }

    /**
     * Get JSON and convert to specified class
     */
    public <T> T getJson(String key, Class<T> clazz) {
        try {
            String jsonValue = (String) redisTemplate.opsForValue().get(key);
            if (jsonValue == null) {
                return null;
            }
            return objectMapper.readValue(jsonValue, clazz);
        } catch (Exception e) {
            log.error("Redis getJson error: ", e);
            return null;
        }
    }

    /**
     * Delete multiple keys
     */
    public long deleteAll(List<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis deleteAll error: ", e);
            return 0;
        }
    }

    /**
     * Get last reset time
     */
    public Long getLastResetTime(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : null;
        } catch (Exception e) {
            log.error("Redis get last reset time error: ", e);
            return null;
        }
    }

    /**
     * Update last reset time
     */
    public void updateLastResetTime(String key, long timestamp) {
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(timestamp));
        } catch (Exception e) {
            log.error("Redis update last reset time error: ", e);
        }
    }

    public String getLastWeeklyResetKey() {
        return LAST_WEEKLY_RESET_KEY;
    }

    public String getLastMonthlyResetKey() {
        return LAST_MONTHLY_RESET_KEY;
    }
}
