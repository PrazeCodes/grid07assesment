package org.grid07.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
@Service
public class ViralityService {
    private static final int MAX_BOT_REPLIES = 100;
    private static final long COOLDOWN_SECONDS = 600; // 10 min
    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    @Autowired
    @Qualifier("customStringRedisTemplate")  // <-- match RedisConfig bean name
    private RedisTemplate<String, String> stringTemplate;

    // =============== VIRALITY SCORE ===============
    public void updateViralityScore(Long postId, String interactionType) {
        String key = "post:" + postId + ":virality_score";
        long points = switch (interactionType) {
            case "BOT_REPLY" -> 1L;
            case "HUMAN_LIKE" -> 20L;
            case "HUMAN_COMMENT" -> 50L;
            default -> 0L;
        };
        if (points > 0) {
            redisTemplate.opsForValue().increment(key, points);
        }
    }

    public Long getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        Long v = redisTemplate.opsForValue().get(key);
        return v == null ? 0L : v;
    }
    // =============== HORIZONTAL CAP ===============
// Atomic INCR. Two threads at count=99:
// Thread A -> 100 (allowed)
// Thread B -> 101 -> DECR back -> rejected
    public boolean checkAndIncrementBotCount(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Long newCount = redisTemplate.opsForValue().increment(key);
        if (newCount == null || newCount > MAX_BOT_REPLIES) {
            redisTemplate.opsForValue().decrement(key);
            return false;
        }
        return true;
    }
    // =============== COOLDOWN CAP ===============
    public boolean isCooldownActive(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        return Boolean.TRUE.equals(stringTemplate.hasKey(key));
    }
    public void setCooldown(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        stringTemplate.opsForValue().set(key, "1",
                Duration.ofSeconds(COOLDOWN_SECONDS));
    }
}
