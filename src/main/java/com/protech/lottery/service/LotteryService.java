package com.protech.lottery.service;

import com.protech.lottery.entity.LotteryAttempt;
import com.protech.lottery.entity.Prize;
import com.protech.lottery.entity.User;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import java.time.Duration;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@DependsOn(value="rocketMQTemplate")
public class LotteryService {

    @Autowired
    private RedisTemplate<String, Prize> prizeRedisTemplate;

    @Autowired
    private RedisTemplate<String, User> userRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

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
//        LotteryAttempt attempt = (LotteryAttempt) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        LinkedHashMap attemptMap = (LinkedHashMap) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        Integer attempt = (Integer) attemptMap.get("attempts");
        return (attempt != null) ? attempt: 0;
    }

    @CacheEvict(value = "lotteryCache", key = "#userId")
    public synchronized String drawPrize(String userId) {

        // Preventing same user resubmit within 10 seconds.
        if (redisTemplate.hasKey(SUBMISSION_KEY  + userId)) {
            return "Please wait for 10 seconds before submitting again";
        }
        // Set a cache entry to prevent duplicate submissions
        redisTemplate.opsForValue().set(SUBMISSION_KEY  + userId, true, Duration.ofSeconds(10));

        // Uncomment This if-Statement to test duplication prevention
        /*
        if (1==1) {
            return "purpose stop to test redis cache";
        }
        */

//        LotteryAttempt attempt = (LotteryAttempt) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        LinkedHashMap attemptMap = (LinkedHashMap) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        Integer attempt = (Integer) attemptMap.get("attempts");
        if (attempt == null || attempt <= 0) {
            // Return No Attempt Left Message
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
            System.out.println("************************ looping Prizes ************************");
            double randomNumber = random.nextDouble(); // This will give 0.01 to 1 maximum where 1 = 100%
            System.out.println("Prize name "+prize.getName());
            System.out.println("Prize Probability "+prize.getWinningProbability());
            System.out.println("Prize randomNumber "+randomNumber);
            System.out.println("Prize Quantity "+prize.getQuantity());

            if (prize.getQuantity() > 0 && randomNumber <= prize.getWinningProbability()) {
                // Update User PrizeID if won
                User user = (User) userRedisTemplate.opsForHash().get(USER_KEY, userId);
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+user.getPrizeId());
                user.setPrizeId(prize.getId());
                userRedisTemplate.opsForHash().put(USER_KEY, userId, user);

                // Message Queue
                //rocketMQTemplate.convertAndSend(LOTTERY_WIN_TOPIC, "User " + userId + " won prize " + prize.getName());
                System.out.println("!!!!!!!!!!! Sending Message Queue !!!!!!!!!!!!!");
                rocketMQTemplate.convertAndSend(LOTTERY_ATTEMPT_TOPIC, "User " + userId +" won prize " + prize.getName());

                // Return Prize Won Message
                return "Congratulations! You have won the following prize: " + prize.getName();
                // return prize;
            }
        }

        // Attempt Key Reduce By 1
        //attempt.setAttempts(attempt.getAttempts() - 1);
        //redisTemplate.opsForHash().put(ATTEMPT_KEY, userId, attempt);
        System.out.println("!!!!!!!!!!! Sending Message Queue 2 !!!!!!!!!!!!!");
        rocketMQTemplate.convertAndSend(LOTTERY_ATTEMPT_TOPIC, "User " + userId + " drew a prize but won nothing");
        // Return No Prize Won Message
        return "Better luck next time!";
    }

    public Prize getUserPrize(String userId) {
        User user = (User) userRedisTemplate.opsForHash().get(USER_KEY, userId);
        if (user !=null && user.getPrizeId() != 0) {
            return (Prize) prizeRedisTemplate.opsForHash().get(PRIZE_KEY, String.valueOf(user.getPrizeId()));
        }
        return null;
    }

    /*
    public List<Prize> getUserPrize(String userId) {
        User user = (User) userRedisTemplate.opsForHash().get(USER_KEY, userId);
        System.out.println("***************************************************");
        System.out.println("Prize Id = "+user.getPrizeId());
        if (user != null) {
            return prizeRedisTemplate.opsForHash().values(PRIZE_KEY)
                    .stream()
                    .map(obj -> (Prize) obj)
                    .collect(Collectors.toList());
        }
        return null;
    }
    */
    private void sendDeductionMessage(String userId) {
        Message<String> message = MessageBuilder.withPayload(userId).build();
        rocketMQTemplate.send("deduction-topic", message);
    }
}

/*
import com.protech.lottery.entity.LotteryAttempt;
import com.protech.lottery.entity.Prize;
import com.protech.lottery.entity.User;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class LotteryService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisTemplate<String, Prize> prizeRedisTemplate;

    @Autowired
    private RedisTemplate<String, User> userRedisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static final String PRIZE_KEY = "Prize";
    private static final String USER_KEY = "User";
    private static final String ATTEMPT_KEY = "LotteryAttempt";

    public void addPrize(Prize prize) {
        redisTemplate.opsForHash().put(PRIZE_KEY, prize.getId(), prize);
    }

    public void addUser(User user) {
        redisTemplate.opsForHash().put(USER_KEY, user.getId(), user);
    }

    public Prize drawPrize(String userId) {
        LotteryAttempt attempt = (LotteryAttempt) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        if (attempt == null || attempt.getAttempts() <= 0) {
            throw new RuntimeException("No attempts left");
        }

        //List<Prize> prizes = (List<Prize>) redisTemplate.opsForHash().values(PRIZE_KEY);

        List<Object> prizeObjects = prizeRedisTemplate.opsForHash().values(PRIZE_KEY);
        List<Prize> prizes = prizeObjects.stream().map(o -> (Prize) o).collect(Collectors.toList());

        Random random = new Random();
        for (Prize prize : prizes) {
            if (prize.getQuantity() > 0 && random.nextDouble() <= prize.getWinningProbability()) {
                attempt.setAttempts(attempt.getAttempts() - 1);
                redisTemplate.opsForHash().put(ATTEMPT_KEY, userId, attempt);
                sendDeductionMessage(userId);
                return prize;
            }
        }

        return null;
    }

    public int getRemainingAttempts(String userId) {
        LotteryAttempt attempt = (LotteryAttempt) redisTemplate.opsForHash().get(ATTEMPT_KEY, userId);
        return (attempt != null) ? attempt.getAttempts() : 0;
    }
    public List<Prize> getUserPrizes(String userId) {
        User user = (User) redisTemplate.opsForHash().get(USER_KEY, userId);
        return (user != null) ? (List<Prize>) redisTemplate.opsForHash().values(PRIZE_KEY) : null;
    }


    private void sendDeductionMessage(String userId) {
        Message<String> message = MessageBuilder.withPayload(userId).build();
        rocketMQTemplate.send("deduction-topic", message);
    }
}
*/
/*
package com.protech.lottery.service;

import com.protech.lottery.entity.*;
import com.protech.lottery.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class LotteryService {

    //@Autowired
    //private RocketMQTemplate rocketMQTemplate;

    private final Random random = new Random();

    private static final String PRIZE_KEY_PREFIX = "PRIZE:";
    private static final String USER_KEY_PREFIX = "USER:";
    private static final String ATTEMPTS_KEY_PREFIX = "ATTEMPTS:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String drawPrize(String userId) {
        String userKey = USER_KEY_PREFIX + userId;
        User user = (User) redisTemplate.opsForValue().get(userKey);
        if (user == null) {
            return "User not found";
        }
        if (user.getLotteryAttempts() <= 0) {
            return "No attempts left for the user";
        }
        int prizeId = user.getPrizeId();
        String prizeKey = PRIZE_KEY_PREFIX + prizeId;
        Prize prize = (Prize) redisTemplate.opsForValue().get(prizeKey);
        if (prize == null) {
            return "Prize not found";
        }
        if (prize.getQuantity() <= 0) {
            return "No more prizes available";
        }
        if (Math.random() < prize.getWinningProbability()) {
            prize.setQuantity(prize.getQuantity() - 1);
            redisTemplate.opsForValue().set(prizeKey, prize);
            user.setLotteryAttempts(user.getLotteryAttempts() - 1);
            redisTemplate.opsForValue().set(userKey, user);
            return "Congratulations! You won " + prize.getName();
        } else {
            user.setLotteryAttempts(user.getLotteryAttempts() - 1);
            redisTemplate.opsForValue().set(userKey, user);
            return "Sorry, you did not win this time";
        }
    }

    /*
    public String drawPrize(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return "User not found.";
        }

        User user = userOptional.get();
        if (user.getLotteryAttempts() <= 0) {
            return "No lottery attempts left.";
        }

        List<Prize> prizes = (List<Prize>) prizeRepository.findAll();
        if (prizes.isEmpty()) {
            return "No prizes available.";
        }

        double totalProbability = prizes.stream().mapToDouble(Prize::getWinningProbability).sum();
        double rand = random.nextDouble() * totalProbability;

        for (Prize prize : prizes) {
            if (rand < prize.getWinningProbability()) {
                if (prize.getQuantity() <= 0) {
                    return "Prize out of stock.";
                }

                // Deduct one from prize quantity
                prize.setQuantity(prize.getQuantity() - 1);
                prizeRepository.save(prize);

                // Update user data
                user.setPrizeId(Integer.parseInt(prize.getId()));
                user.setLotteryAttempts(user.getLotteryAttempts() - 1);
                userRepository.save(user);

                // Send RocketMQ message
                rocketMQTemplate.convertAndSend("lottery-topic", "Lottery attempt for user: " + userId + " successful.");

                return "Congratulations! You won " + prize.getName();
            }
            rand -= prize.getWinningProbability();
        }

        // No win
        user.setLotteryAttempts(user.getLotteryAttempts() - 1);
        userRepository.save(user);

        return "Better luck next time!";
    }

    public List<Prize> getUserPrizes(String userId) {
        String userKey = USER_KEY_PREFIX + userId;
        User user = (User) redisTemplate.opsForValue().get(userKey);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        int prizeId = user.getPrizeId();
        String prizeKey = PRIZE_KEY_PREFIX + prizeId;
        Prize prize = (Prize) redisTemplate.opsForValue().get(prizeKey);
        if (prize == null) {
            throw new RuntimeException("Prize not found");
        }
        return List.of(prize); // Assuming user can win only one prize
    }

    public int getRemainingAttempts(String userId) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + userId;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);
        return attempts != null ? attempts : 0;
    }
}

 */
