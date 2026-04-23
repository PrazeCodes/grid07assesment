package org.grid07.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
@Configuration
public class RedisConfig {

    // For atomic counters (INCR/DECR)
    @Bean
    public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Long> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        t.setHashValueSerializer(new GenericToStringSerializer<>(Long.class));
        t.afterPropertiesSet();
        return t;
    }

    // For cooldowns + notification lists
    @Bean("customStringRedisTemplate")                              // <-- ADD THIS
    public RedisTemplate<String, String> customStringRedisTemplate(  // <-- RENAME METHOD
                                                                     RedisConnectionFactory cf) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setHashValueSerializer(new StringRedisSerializer());
        t.afterPropertiesSet();
        return t;
    }
}