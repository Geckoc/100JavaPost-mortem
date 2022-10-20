package com.example.lock.lockscope;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.stream.IntStream;

/**
 * @Description: 加锁前要清楚锁和被保护的对象是不是一个层面的
 * @Create: 2022-10-19 11:34
 */
@Slf4j
@RestController
@RequestMapping("/lockscope")
public class LockScopeController {

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
        // IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().right());
        return Data.getCounter();
    }

}
