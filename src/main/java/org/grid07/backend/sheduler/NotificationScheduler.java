package org.grid07.backend.sheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
@Component
public class NotificationScheduler {
    @Autowired
    @Qualifier("customStringRedisTemplate")
    private RedisTemplate<String, String> stringTemplate;
    /**
     * Runs every 5 min. Scans user:*:pending_notifs.
     * Pops all messages, logs summary, clears list.
     */
    @Scheduled(fixedRate = 300_000)
    public void sweepPendingNotifications() {
        System.out.println("[SCHEDULER] Starting sweep...");
        Set<String> keys = stringTemplate.keys("user:*:pending_notifs");
        if (keys == null || keys.isEmpty()) {
            System.out.println("[SCHEDULER] No pending notifs.");
            return;
        }
        for (String key : keys) {
            String userId = key.split(":")[1];
            List<String> messages = stringTemplate.opsForList().range(key, 0, -1);
            if (messages == null || messages.isEmpty()) continue;
            int total = messages.size();
            String summary = (total == 1)
                    ? messages.get(0)
                    : messages.get(0) + " and " + (total - 1)
                      + " others interacted with your posts.";
            System.out.println("[SCHEDULER] Summarized Push Notification to User "
                    + userId + ": " + summary);
            stringTemplate.delete(key);
        }
        System.out.println("[SCHEDULER] Sweep complete.");
    }
}
