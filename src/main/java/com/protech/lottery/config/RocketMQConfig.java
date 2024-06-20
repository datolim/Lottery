package com.protech.lottery.config;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.common.ServiceState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    @Value("${rocketmq.producer.send-msg-timeout}")
    private int sendMsgTimeout;

    @Bean
    public DefaultMQProducer defaultMQProducer() {
        System.out.println("Server MQ Name = "+nameServer+", Producer Group = "+producerGroup+", Send Message Timeout ="+sendMsgTimeout);
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

/*
    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducerGroup("lottery-group");
        rocketMQTemplate.setNamesrvAddr(nameServer);
        return rocketMQTemplate;
    }
 */

    @Bean
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer) {
        RocketMQTemplate template = new RocketMQTemplate();
//        template.setProducerGroup("lottery-group");
//        template.setNamesrvAddr(nameServer);
//        template.setProducer(producer);
        return template;
        //return new RocketMQTemplate();
    }

}

/*
package com.protech.lottery.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
public class RocketMQConfig {

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new MappingJackson2MessageConverter();
    }
}
*/
/*
package com.protech.lottery.config;


import org.apache.rocketmq.spring.annotation.EnableRocketMQ;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
@EnableRocketMQ
public class RocketMQConfig {

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new MappingJackson2MessageConverter();
    }
}

package com.protech.lottery.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducerGroup("lottery-group");
        rocketMQTemplate.setNamesrvAddr(nameServer);
        return rocketMQTemplate;
    }
}
*/
