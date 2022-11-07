package com.example.collection.sublist;

import cn.hutool.core.collection.ListUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @ClassName: SubListApplication
 * @Create: 2022-10-24 14:55
 */
@Slf4j
public class SubListApplication {
    private static List<List<Integer>> data = new ArrayList<>();

    public static void main(String[] args){
        // oom();
        // oomfix();
//        right1();
        xo();
    }

    private static void oom() {
        for (int i = 0; i < 1000; i++) {
            List<Integer> rawList = IntStream.rangeClosed(1, 100000).boxed().collect(Collectors.toList());
            data.add(rawList.subList(0, 1));
        }
    }

    private static void oomfix() {
        for (int i = 0; i < 1000; i++) {
            List<Integer> rawList = IntStream.rangeClosed(1, 100000).boxed().collect(Collectors.toList());
            data.add(new ArrayList<>(rawList.subList(0, 1)));
        }
    }

    private static void right1() {
        List<Integer> list = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
        List<Integer> subList = new ArrayList<>(list.subList(1, 4));
        List<Integer> collect = list.stream().skip(0).limit(5).collect(Collectors.toList());
        System.out.println(collect);
        System.out.println(subList);
        subList.remove(1);
        System.out.println(list);
        list.add(0);
        subList.forEach(System.out::println);
    }

    private static void xo(){
        List<Integer> list = IntStream.rangeClosed(1, 15).boxed().collect(Collectors.toList());
        List<List<Integer>> partition = new ArrayList<>(ListUtil.partition(list, 5));
        partition.remove(0);
        System.out.println(list);
        System.out.println(partition);
        list.add(19);
        System.out.println(list);
        for (List<Integer> integers : partition) {
            ArrayList<Integer> integers1 = new ArrayList<>(integers);
            System.out.println(integers1);
            System.out.println("-->");
        }
    }
}
