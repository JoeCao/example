package com.qianmi.example.service;

import com.google.common.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 * @author 曹祖鹏 OF506
 * company qianmi.com
 * Date    2018-06-14
 */
@Service
@RabbitListener(queues = "result")
public class Async2Sync {
    private static final Logger LOGGER = LoggerFactory.getLogger(Async2Sync.class);
    @Autowired
    RabbitTemplate rabbitTemplate;
    private String MESSAGE_SPLITTER = "_";
    private ConcurrentHashMap<String, CountDownLatch> taskPool = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> tempResult = new ConcurrentHashMap<>();

    public void asyncTask() {
        //生成一个唯一key代表任务
        String uuid = UUID.randomUUID().toString();
        String task = "随便什么代表任务啦";

        CountDownLatch doneSignal = new CountDownLatch(1);
        //将任务加入pool中，等待notify
        taskPool.put(uuid, doneSignal);
        //把消息发送到队列
        LOGGER.info("send task to queue");

        rabbitTemplate.convertAndSend("task", uuid + MESSAGE_SPLITTER + task);
        //等待队列返回，await会阻塞，将异步消息调用转为同步

        try {
            LOGGER.info("wait for task done confirm message");
            doneSignal.await();
            LOGGER.info("task {} is done", uuid);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //获得结果
        String result = tempResult.remove(uuid);
        LOGGER.info("获得结果 {}", result);
        //做其他的事情，比如插入数据库之类的


    }

    /**
     * 使用guava的ListenFuture将结果封装到Future的返回值中
     * 和上面的对比来看，封装性会更好一些
     * 但是带来了更多的线程消耗
     */
    public void promiseTaskWithResult() {
        //生成一个唯一key代表任务
        String uuid = UUID.randomUUID().toString();
        String task = "随便什么代表任务啦";

        ListeningExecutorService service =
                MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
        CountDownLatch doneSignal = new CountDownLatch(1);
        //将任务加入pool中，等待notify
        taskPool.put(uuid, doneSignal);

        ListenableFuture<String> future = service.submit(() -> {
            rabbitTemplate.convertAndSend("task", uuid + MESSAGE_SPLITTER + task);
            doneSignal.await();
            String result = tempResult.remove(uuid);
            return result;
        });
        CountDownLatch latch = new CountDownLatch(1);
        Futures.addCallback(future, new FutureCallback<String>() {
            @Override
            public void onSuccess(String result) {
                LOGGER.info("获得结果 {}", result);
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("error", t);
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @RabbitHandler
    public void receiveMessage(String message) {
        LOGGER.info("Received <" + message + ">");
        //返回字符串是 {taskid}|done 这样的格式
        String[] rets = message.split(MESSAGE_SPLITTER);
        //获取返回的taskid
        LOGGER.info("taskid is {}", rets[0]);
        if (taskPool.containsKey(rets[0])) {
            //将结果返回
            tempResult.put(rets[0], message);
            CountDownLatch countDownLatch = taskPool.remove(rets[0]);
            countDownLatch.countDown();
        }

    }
}
