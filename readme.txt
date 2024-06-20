***********************
  Pre-requisition Setup
***********************
1. JDK 17 minimum.
2. REDIS & ROCKETMQ Container will be automatically started by running <docker-compose up -d> at the terminal.
3. Run LotteryProjectApplication
4. Port set to http://localhost:8888
5. Docker command :-
     docker-compose up -d            - to start up container
     docker-compose down --remove-orphans - to stop container
     docker logs rocketmq-nameserver - ensure it is up
     docker logs rocketmq-broker     - ensure it is up



**********
  Test
**********
1. Refer to attached Postman to call and test the API.
2. Alternatively refer following CURL command.
   Add A User :-
      curl -X POST http://localhost:8888/lottery/addUser -H "Content-Type: application/json" -d '{"id": "ID-00003", "prizeId": null, "userId": "user3"}'

   Add a Prize :-
      curl -X POST http://localhost:8888/lottery/addUser -H "Content-Type: application/json" -d '{"id": "ID-00003", "prizeId": null, "userId": "user3"}'

   Set User Attempts :-
      curl -X POST http://localhost:8888/lottery/addUser -H "Content-Type: application/json" -d '{"id": "ID-00003", "prizeId": null, "userId": "user3"}'

   Drar A Prize :-
      curl -X POST http://localhost:8888/lottery/user3/draw

   Get Remaining Attempts :-
      curl -X GET http://localhost:8888/lottery/remainingAttempts/user3


version: '3.8'

services:
  redis:
    image: redis:latest
    container_name: redis
    ports:
      - "6379:6379"

  rocketmq-nameserver:
    image: apacherocketmq/rocketmq:4.8.0
    container_name: rocketmq-nameserver
    ports:
      - "9876:9876"
    command: sh mqnamesrv

  rocketmq-broker:
    image: apacherocketmq/rocketmq:4.8.0
    container_name: rocketmq-broker
    ports:
      - "10909:10909"
      - "10911:10911"
    environment:
      - "NAMESRV_ADDR=rocketmq-nameserver:9876"
    command: sh mqbroker -n rocketmq-nameserver:9876
    depends_on:
      - rocketmq-nameserver

drawPrize logic should reduce 1 lottery attempt each draw by send request via rocketmq and a subscriber to the topic to perform the action

package com.protech.lottery.service;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "LotteryTopic", consumerGroup = "lotteryGroup")
public class LotteryConsumerService implements RocketMQListener<String> {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ATTEMPT_KEY = "attempt";

    @Override
    public void onMessage(String message) {
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
