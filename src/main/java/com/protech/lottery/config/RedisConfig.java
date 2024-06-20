package com.protech.lottery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.protech.lottery.entity.Prize;
import com.protech.lottery.entity.User;
import com.protech.lottery.entity.LotteryAttempt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Configure serializers
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        //serializer.setObjectMapper(new ObjectMapper());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        //serializer.setObjectMapper(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, Prize> prizeRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Prize> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Configure serializers
        Jackson2JsonRedisSerializer<Prize> serializer = new Jackson2JsonRedisSerializer<>(Prize.class);
        //serializer.setObjectMapper(new ObjectMapper());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setEnableTransactionSupport(true);

        return template;
    }

    @Bean
    public RedisTemplate<String, User> userRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, User> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Configure serializers
        Jackson2JsonRedisSerializer<User> serializer = new Jackson2JsonRedisSerializer<>(User.class);
        //serializer.setObjectMapper(new ObjectMapper());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setEnableTransactionSupport(true);

        return template;
    }

    @Bean
    public RedisTemplate<String, LotteryAttempt> attemptRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, LotteryAttempt> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Configure serializers
        Jackson2JsonRedisSerializer<LotteryAttempt> serializer = new Jackson2JsonRedisSerializer<>(LotteryAttempt.class);
        //serializer.setObjectMapper(new ObjectMapper());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setEnableTransactionSupport(true);

        return template;
    }
}