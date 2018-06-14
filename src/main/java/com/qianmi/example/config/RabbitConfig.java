package com.qianmi.example.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author 曹祖鹏 OF506
 * company qianmi.com
 * Date    2018-06-01
 */
@Configuration
public class RabbitConfig {
    @Bean
    public Queue taskQueue() {
        return new Queue("task");
    }

    @Bean
    public Queue resultQueue() {
        return new Queue("result");
    }

}
