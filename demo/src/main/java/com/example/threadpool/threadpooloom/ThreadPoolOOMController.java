package com.example.threadpool.threadpooloom;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * 打印线程池的信息 线程池线程管理策略详解
     * 用一个 printStats 方法实现了最简陋的监控
     * @param threadPool 线程池
     */
    private void printStats(ThreadPoolExecutor threadPool) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            log.info("=========================");
            // 线程数
            log.info("Pool Size: {}", threadPool.getPoolSize());
            // 线程活跃数
            log.info("Active Threads: {}", threadPool.getActiveCount());
            // 完成了多少任务
            log.info("Number of Tasks Completed: {}", threadPool.getCompletedTaskCount());
            // 队列中还有多少积压任务
            log.info("Number of Tasks in Queue: {}", threadPool.getQueue().size());
            log.info("=========================");
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Java中的Executors类定义了一些快捷的工具方法， 来帮助我们快速创建线程池。
     * 《阿里巴巴Java开发手册》中提到，禁止使用这些方法来创建线程池，而应该手动new ThreadPoolExecutor来创建线程池。
     * 这一条规则的背后， 是大量血淋淋的生产事故， 最典型的就是 newFixedThreadPool 和 newCacheThreadPool 可能因为资源耗尽导致0OM问题。
     * 首先，我们来看-下 newFixedThreadPool 为什么可能会出现ooM的问题。
     * 我们写段测试代码，来初始化一个单线程的 newFixedThreadPool,循环1亿次向线程池提交任务
     * 每个任务都会创建一个比较大的字符串然后休眠一小时:
     * 程序执行后，可以发现打印出来的任务队列无限增大
     * 翻看newFixedThreadPool方法的源码不难发现，线程池的工作队列直接new了一个
     * LinkedBlockingQueue,而默认构造方法的LinkedBlockingQueue是-个
     * Integer.MAX_ VALUE长度的队列，可以认为是无界的:
     * 虽然使用newFixedThreadPool可以把工作线程控制在固定的数量上,但任务队列是无界的。
     * 如果任务较多并且执行较慢的话，队列可能会快速积压,撑爆内存导致oom.
     */
    @GetMapping("oom1")
    public void oom1() throws InterruptedException {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        printStats(threadPool);
        for (int i = 0; i < 100000000; i++) {
            threadPool.execute(() -> {
                String payload = IntStream.rangeClosed(1, 1000000)
                        .mapToObj(__ -> "a")
                        .collect(Collectors.joining(""))
                        + UUID.randomUUID().toString();
                try {
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info(payload);
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }

    /**
     * 我们再把刚才的例子稍微改一下,改为使用 newCachedThreadPool方法来获得线程池。
     * 程序运行不久后,同样O0M异常:
     * 打印的信息发现，线程数量一直在无限增大
     * 翻看newCachedThreadPool的源码可以看到，这种线程池的最大线程数是Integer.MAX_ VALUE,可以认为是没有上限的，
     * 而其工作队列SynchronousQueue是-个没有存储空间的阻塞队列。这意味着，只要有请
     * 求到来，就必须找到一条工作线程来处理，如果当前没有空闲的线程就再创建一条新的。
     * 由于我们的任务需要1小时才能执行完成，大量的任务进来后会创建大量的线程。我们知道线
     * 程是需要分配一定的内存空间作为线程栈的，比如1MB，因此无限制创建线程必然会导致OOM
     */
    @GetMapping("oom2")
    public void oom2() throws InterruptedException {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        printStats(threadPool);
        for (int i = 0; i < 100000000; i++) {
            threadPool.execute(() -> {
                String payload = IntStream.rangeClosed(1, 1000000)
                        .mapToObj(__ -> "a")
                        .collect(Collectors.joining(""))
                        + UUID.randomUUID().toString();
                try {
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info(payload);
            });
        }
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }


    /**
     * 首先，自定义一个线程池。这个线程池具有2个核心线程、5个最大线程、使用容量为10的
     * ArrayBlockingQueue阻塞队列作为工作队列，使用默认的AbortPolicy拒绝策略,也就是任
     * 务添加到线程池失败会抛出RejectedExecutionException。 此外,我们借助了HuTool类库的
     * ThreadFactoryBuilder方法来构造一个线程工厂 , 实现线程池线程的自定义命名。
     * 然后,我们写一段测试代码来观察线程池管理线程的策略。测试代码的逻辑为，每次间隔1秒
     * 向线程池提交任务，循环20次，每个任务需要10秒才能执行完成，代码如下:
     * 60 秒后页面输出了 17，有 3 次提交失败了：
     */
    @GetMapping("right")
    public int right() throws InterruptedException {
        // 使用一个计数器跟踪完成的任务
        AtomicInteger atomicInteger = new AtomicInteger();
        // 2个核心线程，5个最大线程，容量为10的ArrayBlockingQueue阻塞队列
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 5,
                5, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadFactoryBuilder().setNameFormat("oom-thread-pool-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());

        printStats(threadPool);
        // 每隔一秒提交一次，一共提交20次
        IntStream.rangeClosed(1, 20).forEach(i -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int id = atomicInteger.incrementAndGet();
            try {
                threadPool.submit(() -> {
                    log.info("{} started", id);
                    // 每个任务执行10秒
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.info("{} finished", id);
                });
            }catch (Exception ex){
                // 如果出现异常，打印错误信息且计数器减1
                log.error("error submitting task {}", id, ex);
                atomicInteger.decrementAndGet();
            }
        });
        TimeUnit.SECONDS.sleep(60);
        return atomicInteger.intValue();
    }

    /***
     * 至此，我们可以总结出线程池默认的工作行为：
     * 不会初始化 corePoolSize 个线程，有任务来了才创建工作线程；
     * 当核心线程满了之后不会立即扩容线程池，而是把任务堆积到工作队列中；
     * 当工作队列满了后扩容线程池，一直到线程个数达到 maximumPoolSize 为止；
     * 如果队列已满且达到了最大线程后还有任务进来，按照拒绝策略处理；
     * 当线程数大于核心线程数时，线程等待 keepAliveTime 后还是没有任务需要处理的话，收缩线程到核心线程数。
     * 了解这个策略，有助于我们根据实际的容量规划需求，为线程池设置合适的初始化参数。
     * 当然，我们也可以通过一些手段来改变这些默认工作行为，比如：
     * 声明线程池后立即调用 prestartAllCoreThreads 方法，来启动所有核心线程；
     * 传入 true 给 allowCoreThreadTimeOut 方法，来让线程池在空闲的时候同样回收核心线程。
     *
     * 那么，我们有没有办法让线程池更激进一点，优先开启更多的线程，而把队列当成一个后备方案呢？
     * 比如我们这个例子，任务执行得很慢，需要 10 秒，如果线程池可以优先扩容到 5个最大线程，
     * 那么这些任务最终都可以完成，而不会因为线程池扩容过晚导致慢任务来不及处理。
     *
     * 限于篇幅，这里我只给你一个大致思路：
     * 1. 由于线程池在工作队列满了无法入队的情况下会扩容线程池，那么我们是否可以重写队列的 offer 方法，造成这个队列已满的假象呢？
     *
     * 2. 由于我们 Hack 了队列，在达到了最大线程后势必会触发拒绝策略，
     * 那么能否实现一个自定义的拒绝策略处理程序，这个时候再把任务真正插入队列呢？
     *
     */
    @GetMapping("better")
    public int better()throws InterruptedException{
        // 这里开始是激进线程池的实现
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(10) {
            @Override
            public boolean offer(Runnable e) {
                // 先返回false，造成队列满的假象，让线程池优先扩容
                return false;
            }
        };
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                2, 5,
                5, TimeUnit.SECONDS,
                queue, new ThreadFactoryBuilder().setNameFormat("demo-threadpool-%d").build(), (r, executor) -> {
            try {
                // 等出现拒绝后再加入队列
                // 如果希望队列满了阻塞线程而不是抛出异常，那么可以注释掉下面三行代码，修改为executor.getQueue().put(r);
                if (!executor.getQueue().offer(r, 0, TimeUnit.SECONDS)) {
                    throw new RejectedExecutionException("ThreadPool queue full, failed to offer " + r.toString());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        // 激进线程池实现结束
        printStats(threadPool);
        //任务编号计数器
        AtomicInteger atomicInteger = new AtomicInteger();
        //每秒提交一个任务，每个任务耗时10秒执行完成，一共提交20个任务
        IntStream.rangeClosed(1, 20).forEach(i -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int id = atomicInteger.incrementAndGet();
            try {
                threadPool.submit(() -> {
                    log.info("{} started", id);
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                    }
                    log.info("{} finished", id);
                });
            } catch (Exception ex) {
                log.error("error submitting task {}", id, ex);
                atomicInteger.decrementAndGet();
            }
        });

        TimeUnit.SECONDS.sleep(60);
        return atomicInteger.intValue();
    }
}
