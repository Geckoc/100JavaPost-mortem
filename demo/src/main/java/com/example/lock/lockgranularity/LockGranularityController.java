package com.example.lock.lockgranularity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @ClassName: LockGranularityController
 * @Description: 加锁要考虑锁的粒度和场景问题
 * @Create: 2022-10-20 11:19
 */
@Slf4j
@RestController
@RequestMapping("/lockgranularity")
public class LockGranularityController {

    /**
     * 共享资源
     */
    private final List<Integer> integerList = new ArrayList<>();

    /**
     * 不涉及资源共享的慢方法
     */
    private void slow(){
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加锁要考虑锁的粒度和场景问题
     * 方法上加synchronized关键字实现加锁确实简单,也因此我曾看到一些业务代码中帆乎所
     * 有方法都加了synchronized,但这种滥用synchronized的做法: .
     * 一是,没必要。通常情况下60%的业务代码是三层架构，数据经过无状态的Controller.
     * Service、Repository 流转到数据库,没必要使用synchronized来保护什么数据。
     * 二是，可能会极大地降低性能。使用Spring框架时，默认情况下Controller、Service、
     * Repository是单例的,加上synchronized导致整个程序几乎就只能支持单线程，造成极大的性能问题。
     * 即使我们确实有-些共享资源需要保护，也要尽可能降低锁的粒度，仅对必要的代码块甚至是
     * 需要保护的资源本身加锁。
     * @return int
     * 比如，在业务代码中，有一个ArrayList因为会被多个线程操作而需要保护，
     * 又有一段比较耗时的操作(代码中的slow方法)不涉及线程安全问题，应该如何加锁呢?
     * 错误的做法是，给整段业务逻辑加锁，把slow方法和操作ArrayList 的代码同时纳入
     * synchronized代码块;更合适的做法是，把加锁的粒度降到最低，只在操作ArrayList的时候给这个ArrayList加锁。
     *
     * 执行这段代码，同样是1000次业务操作，正确加锁的版本耗时1.4秒，而对整个业务逻辑加锁的话耗时11秒。
     *
     */
    @GetMapping("/wrong")
    public int wrong(){
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i ->{
            // 加锁粒度太粗
            synchronized (this){
                slow();
                integerList.add(i);
            }
        });
        log.info("responseWrong:{}", System.currentTimeMillis() - begin);
        return integerList.size();
    }


    /**
     * 如果精细化考虑了锁应用范围后，性能还无法满足需求的话，我们就要考虑另一个维度的粒度
     * 问题了，即:区分读写场景以及资源的访问冲突,考虑使用悲观方式的锁还是乐观方式的锁。
     * -般业务代码中,很少需要进一步考虑这两种更细粒度的锁，所以我只和你分享几个大概的结
     * 论,你可以根据自己的需求来考虑是否有必要进一步优化:
     * ●对于读写比例差异明显的场景，考虑使用ReentrantReadWriteLock细化区分读写锁，来提高性能。
     * ●如果你的JDK版本于1.8、共享资源的冲突概率也没那么大的话,考虑使用
     * StampedLock的乐观读的特性,进一步提高性能。
     * ●JDK里ReentrantLock和ReentrantReadWriteLock都提供了公平锁的版本，在没有明确
     * 需求的情况下不要轻易开启公平锁特性,在任务很轻的情况下开启公平锁可能会让性能下降
     * 上百倍。
     * @return int
     */
    @GetMapping("/right")
    public int right(){
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
            slow();
            // 只针对List共享资源加锁
            synchronized (integerList){
                integerList.add(i);
            }
        });
        log.info("response:{}", System.currentTimeMillis() - begin);
        return integerList.size();
    }
}
