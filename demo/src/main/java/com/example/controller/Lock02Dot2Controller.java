package com.example.controller;

import com.example.entity.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @ClassName: Lock02Dot2Controller
 * @Description: 代码加锁，不要让锁事成为烦心事
 * @Author: Richard_Chen
 * @Create: 2022-10-19 11:34
 */
@Slf4j
@RestController
@RequestMapping("/lockscope")
public class Lock02Dot2Controller {

    /**
     *
     * @param count default 1000000次
     * @return int
     * 因为默认运行1000000次，执行后应该输出100万，但页面输出的结果并不正确。
     * 问题分析：
     * 在非静态的wrong方法上加锁，只能确保多个线程无法执行同一个实例的wrong方法,
     * 却不能保证不会执行不同实例的wrong方法。而静态的counter在多个实例中共享,
     * 所以必然会出现线程安全问题。
     * 理清思路后，修正方法就很清晰了:同样在Data类中定义一个Object类型的静态字段,在操作counter之前对这个字段加锁。
     * 锁静态字段，属于同一个保护层面。
     * 你可能要问了，把wrong方法定义为静态不就可以了，这个时候锁是类级别的。
     * 可以是可以,但我们不可能为了解决线程安全问题改变代码结构，把实例方法改为静态方法。
     */
    @GetMapping("/wrong")
    public int wrong(@RequestParam(value = "count", defaultValue = "1000000") int count){
        Data.reset();
        // 多线程循环一定次数调用Data类不同实例的Wrong方法
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().wrong());
        // 修正后输出正确的100万
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().right());
        return Data.getCounter();
    }


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
    @GetMapping("/wrong1")
    public int wrong1(){
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

    /**
     * 多把锁要小心死锁问题
     * 刚才我们聊到锁的粒度够用就好，这就意味着我们的程序逻辑中有时会存在一些细粒度的锁。
     * 但一个业务逻辑如果涉及多把锁，容易产生死锁问题。
     * 之前我遇到过这样一个案例: 下单操作需要锁定订单中多个商品的库存，到所有商品的锁之
     * 后进行下单扣减库存操作，全部操作完成之后释放所有的锁。代码上线后发现，下单失败概率
     * 很高，失败后需要用户重新下单，极大影响了用户体验，还影响到了销量。
     * 经排查发现是死锁引起的问题，背后原因是扣减库存的顺序不同，导致并发的情况下多个线程
     * 可能相互持有部分商品的锁，又等待其他线程释放另一部分商品的锁， 于是出现了死锁问题。
     * 接下来，我们剖析一下核心的业务代码。
     *
     * 首先，定义一个商品类型Item,包含商品名、库存剩余和商品的库存锁三个属性，
     * 每一种商品默认库存1000个; 然后，初始化10个这样的商品对象来模拟商品清单:
     * @return long
     */
    @GetMapping("/wrong-lock")
    public long wrongLock(){
        long begin = System.currentTimeMillis();
        // 并发进行100次下单操作，并统计下单成功次数
        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i -> {
                    List<Item> cart = createCart();
                    return createOrder(cart);
                })
                .filter(result -> result)
                .count();
        log.info("success:{}, totalRemaining:{}, took:{}ms, items:{}",
                success,
                items.entrySet().stream().map(item -> item.getValue().remaining).reduce(0, Integer::sum),
                System.currentTimeMillis() - begin,
                items);
        return success;
    }

    @lombok.Data
    @RequiredArgsConstructor
    static class Item {
        /**
         * 商品名
         */
        final String name;
        /**
         * 库存
         */
        int remaining = 1000;
        /**
         * toString不包含这个字段
         */
        @ToString.Exclude
        ReentrantLock lock = new ReentrantLock();
    }

    static Map<Integer,Item> items ;
    static {
        items = new HashMap();
        for (int i = 0; i < 1000; i++) {
            items.put(i, new Item(""));
        }
    }

    /**
     * 写一个方法模拟在购物车进行商品选购,每次从商品清单(items 字段)中随机选购三个商品
     * (为了逻辑简单，我们不考虑每次选购多个同类商品的逻辑，购物车中不体现商品数量):
     * @return
     */
    private List<Item> createCart(){
        return IntStream.rangeClosed(1, 3)
                .mapToObj(i -> "item" + ThreadLocalRandom.current().nextInt(items.size()))
                .map(name -> items.get(name)).collect(Collectors.toList());
    }


    private boolean createOrder(List<Item> order){
        // 存放所有获得的锁
        List<ReentrantLock> locks = new ArrayList<>();
        for (Item item : order) {
            try {
                // 获得锁十秒超时
                if (item.lock.tryLock(10, TimeUnit.SECONDS)){
                    locks.add(item.lock);
                }else {
                    locks.forEach(ReentrantLock::lock);
                    return false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 锁全部拿到之后执行扣减库存业务逻辑
        try {
            order.forEach(item -> item.remaining--);
        }finally {
            locks.forEach(ReentrantLock::lock);
        }
        return true;
    }
}
