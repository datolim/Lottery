package com.protech.lottery.listener;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import com.protech.lottery.entity.*;

@Component
@DependsOn(value="rocketMQTemplate")
@RocketMQMessageListener(topic = "LotteryAttemptTopic", consumerGroup = "lotteryGroup")
public class LotteryConsumerService implements RocketMQListener<String> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ATTEMPT_KEY = "LotteryAttempt";

    @Override
    public void onMessage(String message) {
        System.out.println("Listener ********************** Consuming Message Queue *********************************");
        String userId = extractUserIdFromMessage(message);
        if (userId != null) {
            LotteryAttempt attempt = (LotteryAttempt) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
            if (attempt != null && attempt.getAttempts() > 0) {
                attempt.setAttempts(attempt.getAttempts() - 1);
                redisTemplate.opsForHash().put(ATTEMPT_KEY, userId, attempt);
            }
        }
    }

    private String extractUserIdFromMessage(String message) {
        // Extract userId from the message. This is just an example and may need to be adjusted.
        String[] parts = message.split(" ");
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }
}
