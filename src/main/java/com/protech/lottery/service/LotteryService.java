package com.protech.lottery.service;

import com.protech.lottery.entity.LotteryAttempt;
import com.protech.lottery.entity.Prize;
import com.protech.lottery.entity.User;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
//import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import java.time.Duration;

import javax.annotation.PostConstruct;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
//@DependsOn(value="rocketMQTemplate")
public class LotteryService {

    @Autowired
    private RedisTemplate<String, Prize> prizeRedisTemplate;

    @Autowired
    private RedisTemplate<String, User> userRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisTemplate<String, LotteryAttempt> attemptRedisTemplate;

    @Autowired
    private DefaultMQProducer defaultMQProducer;

    //@Autowired
    //private RocketMQTemplate rocketMQTemplate;

    //private RocketMQTemplate rocketMQTemplate;

    //@PostConstruct
    //public void init() {
    //    this.rocketMQTemplate = new RocketMQTemplate();
    //    this.rocketMQTemplate.setProducer(defaultMQProducer);
    //}

    private static final String PRIZE_KEY = "Prize";
    private static final String USER_KEY = "User";
    private static final String ATTEMPT_KEY = "LotteryAttempt";
    private static final String SUBMISSION_KEY = "submission:";
    private static final String LOTTERY_WIN_TOPIC = "LotteryWinningTopic";
    private static final String LOTTERY_ATTEMPT_TOPIC = "LotteryAttemptTopic";

    public void addPrize(Prize prize) {
        prizeRedisTemplate.opsForHash().put(PRIZE_KEY, String.valueOf(prize.getId()), prize);
    }

    public void addUser(User user) {
        userRedisTemplate.opsForHash().put(USER_KEY, user.getUserId(), user);
    }

    public List<Prize> getAllPrizes() {
        return prizeRedisTemplate.opsForHash().values(PRIZE_KEY)
                .stream()
                .map(obj -> (Prize) obj)
                .collect(Collectors.toList());
    }

    public List<User> getAllUsers() {
        return userRedisTemplate.opsForHash().values(USER_KEY)
                .stream()
                .map(obj -> (User) obj)
                .collect(Collectors.toList());
    }

    public void setAttempts(String userId, int attempts) {
        LotteryAttempt lotteryAttempt = new LotteryAttempt();
        lotteryAttempt.setUserId(userId);
        lotteryAttempt.setAttempts(attempts);
        redisTemplate.opsForHash().put(ATTEMPT_KEY, userId, lotteryAttempt);
    }

    public int getRemainingAttempts(String userId) {
        //LotteryAttempt attempt = (LotteryAttempt) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        //return (attempt != null) ? attempt.getAttempts(): 0;

        LinkedHashMap attemptMap = (LinkedHashMap) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        Integer attempt = (Integer) attemptMap.get("attempts");
        return (attempt != null) ? attempt : 0;
    }

    @CacheEvict(value = "lotteryCache", key = "#userId")
    public synchronized String drawPrize(String userId) {
        System.out.println("inside Draw Prize service");
        // Preventing same user resubmit within 10 seconds.
        if (redisTemplate.hasKey(SUBMISSION_KEY + userId)) {
            return "Please wait for 10 seconds before submitting again";
        }
        // Set a cache entry to prevent duplicate submissions
        redisTemplate.opsForValue().set(SUBMISSION_KEY + userId, true, Duration.ofSeconds(10));

        // Uncomment This if-Statement to test duplication prevention
        /*
        if (1==1) {
            return "purpose stop to test redis cache";
        }
        */

        LinkedHashMap attemptMap = (LinkedHashMap) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        Integer attempt = (Integer) attemptMap.get("attempts");
        if (attempt == null || attempt <= 0) {
            return "No attempts left";
        }

        List<Object> prizeObjects = prizeRedisTemplate.opsForHash().values(PRIZE_KEY);
        List<Prize> prizes = prizeObjects.stream().map(o -> (Prize) o).collect(Collectors.toList());

        if (prizes.isEmpty()) {
            // Return No Prize Created Message
            return "No prizes available";
        }

        Random random = new Random();
        for (Prize prize : prizes) {
            double randomNumber = random.nextDouble(); // This will give 0.01 to 1 maximum where 1 = 100%

            if (prize.getQuantity() > 0 && randomNumber <= prize.getWinningProbability()) {
                // Update User PrizeID if won
                User user = (User) userRedisTemplate.opsForHash().get(USER_KEY, userId);
                user.setPrizeId(prize.getId());
                userRedisTemplate.opsForHash().put(USER_KEY, userId, user);

                //Reduce Prize Total Qty = Qty -1;
                prize.setQuantity(prize.getQuantity() - 1);
                prizeRedisTemplate.opsForHash().put(PRIZE_KEY, String.valueOf(prize.getId()), prize);

                sendDeductionMessage(userId);

                //rocketMQTemplate.convertAndSend(LOTTERY_ATTEMPT_TOPIC, "User " + userId +" won prize " + prize.getName());

                // Return Prize Won Message
                return "Congratulations! You have won the following prize: " + prize.getName();
            }
        }

        System.out.println("!!!!!!!!!!! Sending Message Queue - No Prize Won !!!!!!!!!!!!!");
        //rocketMQTemplate.convertAndSend(LOTTERY_ATTEMPT_TOPIC, "User " + userId + " drew a prize but won nothing");

        // Return No Prize Won Message
        sendDeductionMessage(userId);
        return "Better luck next time!";
    }

    private void sendDeductionMessage(String userId) {
        //Reduce User Attempt Count. This Logic suppose inside Consumer Listener to perform. Temporary replace here
        LotteryAttempt userAttempt = (LotteryAttempt) attemptRedisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        System.out.println("CCCCCCCCCCCCC ="+userAttempt);
        userAttempt.setAttempts(userAttempt.getAttempts()-1);
        attemptRedisTemplate.opsForHash().put(ATTEMPT_KEY, userId, userAttempt);

        String messageBody = "User " + userId + " attempted the lottery.";
        Message message = new Message(LOTTERY_ATTEMPT_TOPIC, "DRAW_PRIZE", messageBody.getBytes());
        try {
            defaultMQProducer.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Prize getUserPrize(String userId) {
        User user = (User) userRedisTemplate.opsForHash().get(USER_KEY, userId);
        if (user != null && user.getPrizeId() != 0) {
            return (Prize) prizeRedisTemplate.opsForHash().get(PRIZE_KEY, String.valueOf(user.getPrizeId()));
        }
        return null;
    }
}