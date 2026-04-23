package org.grid07.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
@Service
public class NotificationService {
    private static final long NOTIF_COOLDOWN_SECONDS = 900; // 15 min
    @Autowired
    @Qualifier("customStringRedisTemplate")
    private RedisTemplate<String, String> stringTemplate;
    public void handleBotInteraction(Long userId, String botName, String postId) {
        String cooldownKey = "user:" + userId + ":notif_sent";
        String pendingKey = "user:" + userId + ":pending_notifs";
        String msg = "Bot " + botName + " replied to your post #" + postId;
        Boolean onCooldown = stringTemplate.hasKey(cooldownKey);
        if (Boolean.FALSE.equals(onCooldown) || onCooldown == null) {
            //not on cooldown
            System.out.println("[NOTIFICATION] Push Notification Sent to User "
                    + userId + ": " + msg);
            stringTemplate.opsForValue().set(cooldownKey, "1",
                    Duration.ofSeconds(NOTIF_COOLDOWN_SECONDS));
        } else {
            //on cooldown
            System.out.println("[NOTIFICATION] Queued for User "
                    + userId + " (on cooldown)");
        }
    }
}
