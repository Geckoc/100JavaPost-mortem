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



}
