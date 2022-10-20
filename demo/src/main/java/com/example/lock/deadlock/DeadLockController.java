package com.example.lock.deadlock;

import com.example.lock.lockscope.LockScopeController;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @ClassName: DeadLockController
 * @Description: 多把锁要小心死锁问题
 * @Create: 2022-10-20 11:24
 */
@Slf4j
@RestController
@RequestMapping("/deadlock")
public class DeadLockController {

    /**
     *    首先，定义一个商品类型Item,包含商品名、库存剩余和商品的库存锁三个属性，
     *    每一种商品默认库存1000个; 然后，初始化10个这样的商品对象来模拟商品清单:
     */
    @Data
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

    /**
     * 初始化10个商品对象来模拟商品清单:
     */
    public DeadLockController() {
        IntStream.range(0, 10).forEach(i -> items.put("item" + i, new Item("item" + i)));
    }

    private ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<>();

    /**
     * 写一个方法模拟在购物车进行商品选购,每次从商品清单(items 字段)中随机选购三个商品
     * (为了逻辑简单，我们不考虑每次选购多个同类商品的逻辑，购物车中不体现商品数量):
     * @return List<Item>
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
                    locks.forEach(ReentrantLock::unlock);
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
            locks.forEach(ReentrantLock::unlock);
        }
        return true;
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
     * @return long
     */
    @GetMapping("/wrong")
    public long wrong(){
        long begin = System.currentTimeMillis();
        // 并发进行100次下单操作，并统计下单成功次数
        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i -> {
                    List<Item> cart = createCart();
                    return createOrder(cart);
                })
                .filter(result -> result)
                .count();
        // success:2, totalRemaining:9994, took:90022ms,
        // items:{item0=DeadLockController.Item(name=item0, remaining=1000),
        // item2=DeadLockController.Item(name=item2, remaining=998),
        // item1=DeadLockController.Item(name=item1, remaining=1000),
        // item8=DeadLockController.Item(name=item8, remaining=1000),
        // item7=DeadLockController.Item(name=item7, remaining=1000),
        // item9=DeadLockController.Item(name=item9, remaining=999),
        // item4=DeadLockController.Item(name=item4, remaining=999),
        // item3=DeadLockController.Item(name=item3, remaining=999),
        // item6=DeadLockController.Item(name=item6, remaining=1000),
        // item5=DeadLockController.Item(name=item5, remaining=999)}
        // 100次下单成功了2次，10间商品总计10000件，库存剩余9994件，消耗6件符合预期(2次下单，每次3件) 耗时90秒+
        // 使用JDK自带的VisualVM工具跟踪，就可以看到提示出现了线程死锁
        // 那为什么会有死锁问题呢?
        // 我们仔细回忆一下购物车添加商品的逻辑，随机添加了三种商品，假设一个购物车中的商品是
        // item1和item2,另一个购物车中的商品是item2和item1,一个线程先获取到了item1的锁，
        // 同时另-个线程获取到了item2的锁，然后两个线程接下来要分别获取item2和item1
        // 的锁，这个时候锁已经被对方获取了，只能相互等待一直到10秒超时。
        log.info("success:{}, totalRemaining:{}, took:{}ms, items:{}",
                success,
                items.entrySet().stream().map(item -> item.getValue().remaining).reduce(0, Integer::sum),
                System.currentTimeMillis() - begin,
                items);
        return success;
    }

    /**
     * 其实,避免死锁的方案很简单，为购物车中的商品排一下序， 让所有的线程一定是先获取
     * item1的锁然后获取item2的锁，就不会有问题了。
     * 所以,我只需要修改一行代码,对createCart获得的购物车按照商品名进行排序即可:
     * success:100, totalRemaining:9700, took:6ms, items:...
     * 日志结果： 不管执行多少次都是100次下单成功，且性能极高。
     * @return long
     */
    @GetMapping("/right")
    public long right(){
        long begin = System.currentTimeMillis();
        // 并发进行100次下单操作，并统计下单成功次数
        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i -> {
                    List<Item> cart = createCart().stream()
                            .sorted(Comparator.comparing(Item::getName))
                            .collect(Collectors.toList());
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
}
