package com.protech.lottery.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class LotteryMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(LotteryMessageListener.class);

    @Autowired
    private DefaultMQPushConsumer defaultMQConsumer;

    private static final String LOTTERY_ATTEMPT_TOPIC = "LotteryAttemptTopic";

    @PostConstruct
    public void init() throws Exception {
        logger.info("Initializing LotteryMessageListener...");
        logger.info("Subscribing to topic: {}", LOTTERY_ATTEMPT_TOPIC);

        defaultMQConsumer.subscribe(LOTTERY_ATTEMPT_TOPIC, "DRAW_PRIZE");

        defaultMQConsumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String messageBody = new String(msg.getBody());
                    processMessage(messageBody);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        defaultMQConsumer.start();
        logger.info("LotteryMessageListener started.");

    }

    private void processMessage(String messageBody) {
        // Example logic to process the message immediately
        logger.info("Processing message: {}", messageBody);
        // Implement logic to update Lottery Attempt
    }
}
