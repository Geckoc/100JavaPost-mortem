# 100JavaPost-mortem

### 介绍
100个案例、约130个小坑，其中40%来自于我经历过或者是见过的200多个线上生产事故，剩下的60%来自于我开发业务项目，以及日常审核别人的代码发现的问题。贴近实际，而不是讲述过时的或日常开发根本用不到的技术或框架。

### 代码篇
+ 01 使用了并发工具类库，线程安全就高枕无忧了吗？：[concurrenttool](https://github.com/Geckoc/100JavaPost-mortem/tree/master/demo/src/main/java/com/example/concurrenttool)
+ 02 代码加锁：不要让“锁”事成为烦心事：[lock](https://github.com/Geckoc/100JavaPost-mortem/tree/master/demo/src/main/java/com/example/lock)
+ 03 线程池：业务代码最常用也最容易犯错的组件：[threadpool](https://github.com/Geckoc/100JavaPost-mortem/tree/master/demo/src/main/java/com/example/threadpool)
+ 04 连接池：别让连接池帮了倒忙：[connectionpool]()
+ 05 HTTP调用：你考虑到超时、重试、并发了吗？：httpinvoke
+ 06 20%的业务代码的Spring声明式事务，可能都没处理正确：transaction
+ 07 数据库索引：索引不是万能药：sqlindex
+ 08 判等问题：程序里如何确定你就是你？：equals
+ 09 数值计算：注意精度、舍入和溢出问题：numeralcalculations
+ 10 集合类：坑满地的List列表操作：[collection](https://github.com/Geckoc/100JavaPost-mortem/tree/master/demo/src/main/java/com/example/collection)
+ 11 空值处理：分不清楚的null和恼人的空指针：nullvalue
+ 12 异常处理：别让自己在出问题的时候变为瞎子：exception
+ 13 日志：日志记录真没你想象的那么简单：logging
+ 14 文件IO：实现高效正确的文件读写并非易事：io
+ 15 序列化：一来一回，你还是原来的你吗？：serialization
+ 16 用好Java 8的日期时间类，少踩一些“老三样”的坑：datetime
+ 17 别以为“自动挡”就不可能出现OOM：oom
+ 18 当反射、注解和泛型遇到OOP时，会有哪些坑？：advancedfeatures
+ 19 Spring框架：IoC和AOP是扩展的核心：springpart1
+ 20 Spring框架：帮我们做了很多工作也带来了复杂度：springpart2

### 设计篇
+ 21 代码重复：搞定代码重复的三个绝招：redundantcode
+ 22 接口设计：系统间对话的语言，一定要统一：apidesign
+ 23 缓存设计：缓存可以锦上添花也可以落井下石：cachedesign
+ 24 业务代码写完，就意味着生产就绪了？：productionready
+ 25 异步处理好用，但非常容易用错：asyncprocess
+ 26 数据存储：NoSQL与RDBMS如何取长补短、相辅相成？：nosqluse

### 安全篇
+ 27 数据源头：任何客户端的东西都不可信任：clientdata
+ 28 安全兜底：涉及钱时，必须考虑防刷、限量和防重：securitylastdefense
+ 29 数据和代码：数据就是数据，代码就是代码：dataandcode
+ 30 敏感数据：如何正确保存和传输敏感数据？：sensitivedata

## 加餐
+ 带你吃透课程中Java 8的那些重要知识点：java8
+ 分析定位Java问题，一定要用好这些工具：troubleshootingtools

### 学习方法
我们都知道，编程是一门实践科学,只看不练、不思考，效果通常不会太好。
因此,我建议你能够按照下面的方式深入学习:
1. 对于每一个坑点,实际运行调试一下源码,使用文中提到的工具和方法重现问题，眼见为实。
2. 对于每一个坑点， 再思考下除了文内的解决方案和思路外,是否还有其他修正方式。
3. 对于坑点根因中涉及的JDK或框架源码分析，你可以找到相关类再系统阅读一下源码。
4. 实践课后思考题。这些思考题，有的是对文章内容的补充，有的是额外容易踩的坑。

理解了课程涉及的所有案例后，你应该就对业务代码大部分容易犯错的点了如指掌了，不仅仅自己可以写出更高质量的业务代码，还可以在审核别人代码时发现可能存在的问题，帮助整个
团队成长。
当然了，你从这里收获的将不仅是解决案例中那些问题的方法，还可以提升自己分析定位问题、阅读源码的能力。当你再遇到其他诡异的坑时，也能有清晰的解决思路，也可以成长为
一名救火专家，帮助大家-起定位、分析问题。


