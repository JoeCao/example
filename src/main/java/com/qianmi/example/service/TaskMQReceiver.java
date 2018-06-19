package com.qianmi.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author 曹祖鹏 OF506
 * company qianmi.com
 * Date    2018-06-01
 */
@Component
@RabbitListener(queues = "task", concurrency = "5")
public class TaskMQReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskMQReceiver.class);
    @Autowired
    RabbitTemplate rabbitTemplate;

    Random random = new Random();

    @RabbitHandler
    public void receiveMessage(String message) {
        LOGGER.info("Received <" + message + ">");
        try {
            Thread.sleep(random.nextInt(300));
            //模拟隔一段时间再给返回
            LOGGER.info("Task done");
            rabbitTemplate.convertAndSend("result", String.format("%s|done", message));
        } catch (Exception e) {
            LOGGER.error(String.format("message %s can not be done", message), e);
        }


    }
}
