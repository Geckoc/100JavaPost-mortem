package com.example.concurrenttool;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: ThreadLocalMisuseController
 * @Create: 2023-10-19 15:13
 */
@RestController
@RequestMapping("threadlocal")
public class ThreadLocalMisuseController {
    private static final ThreadLocal<Integer> currentUser = ThreadLocal.withInitial(() -> null);

    /**
     * 按理说，在设置用户信息之前第一次获取的值始终应该是 null
     * 但我们要意识到，程序运行在 Tomcat 中
     * 执行程序的线程是 Tomcat 的工作线程，而 Tomcat 的工作线程是基于线程池的。
     * 顾名思义，线程池会重用固定的几个线程，一旦线程重用
     * 那么很可能首次从 ThreadLocal 获取的值是之前其他用户的请求遗留的值。
     * 这时，ThreadLocal 中的用户信息就是其他用户的信息。
     * 为了更快地重现这个问题,可以修改配置文件，把工作线程池最大线程数设置为 1
     * server.tomcat.max-threads=1
     * @param userId
     * @return
     */
    @GetMapping("wrong")
    public Map wrong(@RequestParam("userId") Integer userId) {
        // 设置用户信息之前先查询一次ThreadLocal中的用户信息
        String before  = Thread.currentThread().getName() + ":" + currentUser.get();
        // 设置用户信息到ThreadLocal
        currentUser.set(userId);
        // 设置用户信息之后再查询一次ThreadLocal中的用户信息
        String after  = Thread.currentThread().getName() + ":" + currentUser.get();
        // 汇总输出两次查询结果
        Map result = new HashMap();
        result.put("before", before);
        result.put("after", after);
        return result;
    }

    @GetMapping("right")
    public Map right(@RequestParam("userId") Integer userId) {
        String before  = Thread.currentThread().getName() + ":" + currentUser.get();
        currentUser.set(userId);
        try {
            String after = Thread.currentThread().getName() + ":" + currentUser.get();
            Map result = new HashMap();
            result.put("before", before);
            result.put("after", after);
            return result;
        } finally {
            currentUser.remove();
        }
    }

}
