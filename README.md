# go-concurrency-to-java

将go的并发工具(RwMutex Once WaitGroup SingleFlight Context ErrGroup Channel Select)带到Java中。

- **RwMutex 读写锁**

Java的ReentrantReadWriteLock容易出现读饥饿或者写饥饿。GoRwMutexTest中比较了GoRwMutex、ReentrantWriteReadLock和StampedLock(简单用法)，
发现在该测试中GoRwMutex最不容易出现读饥饿或者写饥饿。

- **WaitGroup**

基于Java的Phaser实现Go的WaitGroup，一些行为不同于原WaitGroup(不会:[1.并发Add和Wait会panic 2.前一个Wait还没有完成就Add也会panic](https://colobu.com/2019/04/28/gopher-2019-concurrent-in-action/#Waitgroup))。测试中也提供了Phaser的简单用法。

- **SingleFlight和ErrGroup**

SingleFlight支持泛型和try-catch，ErrGroup支持try-catch。

- **Channel和Select**

将[stuglaser/pychan](https://github.com/stuglaser/pychan)的Python代码翻译成Java代码。修改[goncurrent](https://github.com/anolivetree/goncurrent)的Channel，1.使重复关闭丢异常 2.增加额外的shuffle行为。修改pychan，1.增加泛型 2.修改消费者和Select处理Channel关闭的逻辑。封装goncurrent和pychan，提供统一的抽象和调用方法。测试了[0yuyuko0/selector](https://github.com/0yuyuko0/selector)，增加重复次数，发现线程不安全，见本人提的第一个[issue](https://github.com/0yuyuko0/selector/issues/1)。测试Quasar的Channel，能够出现同样的[断言错误](https://github.com/puniverse/quasar/blob/master/quasar-core/src/main/java/co/paralleluniverse/strands/channels/Selector.java#L360)。故不提供对selector和Quasar的封装。

goncurrent使用一把静态的全局的锁，所有channel用一把锁，影响性能。但是使用ThreadLocal来重用一些中间对象。
pychan send（或receive）时，可以是1.释放一把锁后，再持有另一把锁 2.持有另一把锁时继续持有另一把锁。

本人也实现了一个Channel，基于Java 5 的SynchronousQueue的前身[util.concurrent.SynchronousChannel](http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/SynchronousChannel.java)，见另一个[项目](https://github.com/EugeneYoung1515/go-channel)。

- **其他**

Once和Context的实现见代码。


