package com.pu429640.config;

import com.pu429640.services.IUserTagStorage;
import com.pu429640.services.UserTagLocalStorage;
import com.pu429640.services.UserTagRedisStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.pu429640.domain.UserTagEvent;

@Configuration
public class UserTagStorageConfig {

    @Value("${user.tag.storage.type}")
    private String storageType;

    @Bean
    public IUserTagStorage userTagStorage(RedisConnectionFactory redisConnectionFactory) {
        if ("redis".equalsIgnoreCase(storageType)) {
            return new UserTagRedisStorage(redisTemplate(redisConnectionFactory));
        } else {
            return new UserTagLocalStorage();
        }
    }

    @Bean
    public RedisTemplate<String, UserTagEvent> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, UserTagEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(UserTagEvent.class));
        return template;
    }
}