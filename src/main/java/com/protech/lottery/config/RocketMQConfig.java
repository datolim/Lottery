package com.protech.lottery.config;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.common.ServiceState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.consumer.group}")
    private String consumerGroup;

    @Value("${rocketmq.producer.send-msg-timeout}")
    private int sendMsgTimeout;

    @Bean
    public DefaultMQProducer defaultMQProducer() {
        System.out.println("Server MQ Name = "+nameServer+", Producer Group = "+producerGroup+", Consumer Group = "+consumerGroup+ ", Send Message Timeout ="+sendMsgTimeout);
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendMsgTimeout);
        try {
            if (producer.getDefaultMQProducerImpl().getServiceState() != ServiceState.RUNNING) {
                producer.start();
            }
        } catch (MQClientException e) {
            throw new RuntimeException("RocketMQ Producer failed to start", e);
        }

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(producer::shutdown));
        /**
        try {
            producer.start();
        } catch (MQClientException e) {
            if (!e.getErrorMessage().contains("maybe started once, RUNNING")) {
                throw new RuntimeException("RocketMQ Producer failed to start", e);
            }
        }
         **/
        return producer;
    }

    @Bean(destroyMethod = "shutdown")
    public DefaultMQPushConsumer defaultMQPushConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        // Other configuration like subscription, etc.
        return consumer;
    }

/*
    @Bean
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer) {
        RocketMQTemplate template = new RocketMQTemplate();
        //template.setProducer(producer);
        return template;
        //return new RocketMQTemplate();
    }
 */
}
