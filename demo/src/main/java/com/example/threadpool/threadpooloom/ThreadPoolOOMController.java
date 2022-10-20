package com.example.threadpool.threadpooloom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池的声明需要手动进行
 *
 * @Create: 2022-10-20 14:16
 */
@Slf4j
@RestController
@RequestMapping("/threadpooloom")
public class ThreadPoolOOMController {

    /**
     * 打印线程池的信息
     * @param threadPool
     */
    private void printStats(ThreadPoolExecutor threadPool) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            log.info("=========================");
            log.info("Pool Size: {}", threadPool.getPoolSize());
            log.info("Active Threads: {}", threadPool.getActiveCount());
            log.info("Number of Tasks Completed: {}", threadPool.getCompletedTaskCount());
            log.info("Number of Tasks in Queue: {}", threadPool.getQueue().size());
            log.info("=========================");
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Java中的Executors类定义了一些快捷的工具方法， 来帮助我们快速创建线程池。
     * 《阿里巴巴Java开发手册》中提到，禁止使用这些方法来创建线程池，而应该手动new ThreadPoolExecutor来创建线程池。
     * 这一条规则的背后， 是大量血淋淋的生产事故， 最典型的就是 newFixedThreadPool 和 newCacheThreadPool 可能因为资源耗尽导致0OM问题。
     * 首先，我们来看-下 newFixedThreadPool 为什么可能会出现ooM的问题。
     * 我们写段测试代码，来初始化一个单线程的 newFixedThreadPool,循环1亿次向线程池提交
     * 任务，
     * 每个任务都会创建一个比较大的字符串然后休眠一-小时:
     */
    @GetMapping("/oom1")
    public void oom1(){
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        printStats(threadPool);
    }
}
