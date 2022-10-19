package com.example.services;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: Lock2Dot2Service
 * @Description: 分享一个有趣的案例吧。
 * 有一天, 一位同学在群里说"见鬼了，疑似遇到了一个JVM的Bug" , 我们都很好奇是什么Bug。
 * 于是，他贴出了这样一段代码:在一个类里有两个int类型的字段a和b,有一个add方法循环1万次
 * 对a和b进行++操作，有另一个compare方法，同样循环1万次判断a否小于b，条件成立就打印a和b的值,并判断a>b否成立。
 * 他起了两个线程来分别执行add和compare方法。
 * 按道理，a和b同样进行累加操作，应该始终相等，compare 中的第一次判断应该始终不会成立，不会输出任何日志。
 * 但执行代码后发现不但输出了日志,而且更诡异的是, compare方法在判断 a<b 成立的情况下还输出了 a>b 也成立:
 * @Author: Richard_Chen
 * @Create: 2022-10-19 14:51
 */
@Slf4j
public class Lock2Dot2Service {

    volatile int a = 1;
    volatile int b = 1;

    public void add(){
        log.info("add start");
        for (int i = 0; i < 10000; i++) {
            a++;
            b++;
        }
        log.info("add done");
    }

    public void compare(){
        log.info("compare start");
        for (int i = 0; i < 10000; i++) {
            // a始终等于b吗
            if (a < b){
                // a > b 始终等于false吗?
                log.info("a:{},b:{},{}", a, b, a > b);
            }
        }
        log.info("compare done");
    }

    /**
     *
     * 群里一位同学看到这个问题笑了，说:”这哪是 JVM的Bug，分明是线程安全问题嘛。
     * 很明显,你这是在操作两个字段a和b,有线程安全问题，应该为add方法加上锁，
     * 确保a和b的++是原子性的,就不会错乱了。”随后,他为add方法加上了锁:
     *
     * 但，加锁后问题没有得到解决。
     *
     * 我们来仔细想一下，为什么锁可以解决线程安全问题呢。因为只有一个线程可以拿到锁,所以
     * 加锁后的代码中的资源操作是线程安全的。
     * 但是,这个案例中的add方法始终只有一个线程在操作，显然只为add方法加锁是没用的。
     * 之所以出现这种错乱，是因为两个线程是佼错执行add和compare方法中的业务逻辑，而且
     * 这些业务逻辑不是原子性的: a++ 和b++操作中可以穿插在compare方法的比较代码中;
     * 更需要注意的是，a<b这种比较操作在字节码层面是加载a、加载b和比较三步,代码虽然是一行但也不是原子性的。
     * 正确的做法应该是,为add和compare都加上方法锁，确保add方法执行时,compare无法读取a和b
     *
     * 所以，使用锁解决问题之前一定要理清楚,我们要保护的是什么逻辑，多线程执行的情况又是怎样的。
     * @param args
     */
    public static void main(String[] args) {
        Lock2Dot2Service lock2Dot2Service = new Lock2Dot2Service();
        new Thread(() -> lock2Dot2Service.add()).start();
        new Thread(() -> lock2Dot2Service.compare()).start();
    }



}
