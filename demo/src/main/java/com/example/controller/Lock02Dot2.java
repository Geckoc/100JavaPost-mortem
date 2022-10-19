package com.example.controller;

import com.example.entity.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.IntStream;

/**
 * @ClassName: Lock02Dot2
 * @Description: 代码加锁，不要让锁事成为烦心事
 * @Author: Richard_Chen
 * @Create: 2022-10-19 11:34
 */
@RestController
@RequestMapping("/lockscope")
public class Lock02Dot2 {

    /**
     *
     * @param count default 1000000次
     * @return int
     * 因为默认运行1000000次，执行后应该输出100万，但页面输出的结果并不正确。
     */
    @GetMapping("/wrong")
    public int wrong(@RequestParam(value = "count", defaultValue = "1000000") int count){
        Data.reset();
        // 多线程循环一定次数调用Data类不同实例的Wrong方法
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().wrong());
        return Data.getCounter();
    }

}
