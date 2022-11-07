package com.example.collection.aslist;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: AsListApplication
 * @Description:
 * @Create: 2022-10-24 14:13
 */
@Slf4j
public class AsListApplication {

    public static void main(String[] arg){
       // wrong2();
        right2();
    }

    /**
     * 不能直接使用Arrays.asList来转换基本类型数组
     * 通过日志可以发现，这个List包含的其实是一个int数组,整个List的元素个数是1,元素类型是整数数组。
     * 其原因是，只能是把int装箱为Integer,不可能把int数组装箱为Integer数组。我们知道,
     * Arrays.asList 方法传入的是一个泛型T类型可变参数,最终int数组整体作为了一个对象
     * 成为了泛型类型T:
     */
    private static void wrong1(){
        int[] arr = {1,2,3};
        List<int[]> ints = Arrays.asList(arr);
        log.info("list:{} size:{} class:{}", ints, ints.size(), ints.get(0).getClass());
    }

    /**
     * 直接遍历这样的List必然会出现Bug,修复方式有两种:
     * 如果使用Java8以上版本可以使用Arrays.stream方法来转换，
     * 否则可以把int数组声明为包装类型Integer数组:
     */
    private static void right1() {
        int[] arr1 = {1, 2, 3};
        List list1 = Arrays.stream(arr1).boxed().collect(Collectors.toList());
        log.info("list:{} size:{} class:{}", list1, list1.size(), list1.get(0).getClass());

        Integer[] arr2 = {1, 2, 3};
        List list2 = Arrays.asList(arr2);
        log.info("list:{} size:{} class:{}", list2, list2.size(), list2.get(0).getClass());
    }

    /**
     * 第二个坑，Arrays.asList 返回的List 不支持增删操作。Arrays.asList 返回的List并不是我
     * 们期望的java.util.ArrayList,而是Arrays的内部类ArrayList。 ArrayList 内部类继承自
     * AbstractList类,并没有覆写父类的add方法，而父类中add方法的实现，就是抛出
     * UnsupportedOperationException。
     * 第三个坑，对原始数组的修改会影响到我们获得的那个List。看一下 ArrayList的实现，可以
     * 发现ArrayList实是直接使用了原始的数组。所以，我们要特别小心，把通过Arrays.asList
     * 获得的List 交给其他方法处理，很容易因为共享了数组，相互修改产生Bug。
     * 修复方式比较简单，重新new一个ArrayList 初始化Arrays.asList 返回的List即可
     */
    private static void wrong2() {
        String[] arr = {"1", "2", "3"};
        List list = Arrays.asList(arr);
        arr[1] = "4";
        try {
            list.add("5");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.info("arr:{} list:{}", Arrays.toString(arr), list);
    }

    /**
     * 修改后的代码实现了原始数组和List的“解耦”
     * 不再相互影响。同时，因为操作的是真正的ArrayList, add 也不再出错:
     */
    private static void right2() {
        String[] arr = {"1", "2", "3"};
        List list = new ArrayList(Arrays.asList(arr));
        arr[1] = "4";
        try {
            list.add("5");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.info("arr:{} list:{}", Arrays.toString(arr), list);
    }
}
