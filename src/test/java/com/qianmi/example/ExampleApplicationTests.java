package com.qianmi.example;

import com.qianmi.example.service.Async2Sync;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExampleApplicationTests {

    @Autowired
    Async2Sync async2Sync;

    @Test
    public void singleTask() {
        async2Sync.asyncTask();
    }

    @Test
    public void promiseTask() {
        async2Sync.promiseTaskWithResult();
    }

    /**
     * 并发十个线程，去跑100个任务
     */
    @Test
    public void batchSyncTask() throws InterruptedException {

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {

            tasks.add(() -> async2Sync.asyncTask());

        }
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(tasks.size());
        int i = 1;
        for (final Runnable task : tasks) {

            Thread t = new Thread("thread create by joe" + i) {
                public void run() {
                    try {
                        startGate.await();
                        try {
                            task.run();
                        } finally {
                            endGate.countDown();
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            };
            i++;
            t.start();
        }
        long begin = System.currentTimeMillis();
        startGate.countDown();
        endGate.await();
        long end = System.currentTimeMillis();
        System.out.println("total spend " + (end - begin) / 1000);
    }

}
