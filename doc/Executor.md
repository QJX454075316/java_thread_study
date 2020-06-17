### Executor 框架
#####　Exectutor 框架的两极调度模型

在HotSpot VM的线程模型中，Java线程（java.lang.Thread）被一对一映射为本地操作系统线程。Java线程启动时会创建一个本地操作系统线程；当该Java线程终止时，这个操作系统线程也会被回收。操作系统会调度所有线程并将它们分配给可用的CPU。

在上层，Java多线程程序通常把应用分解为若干个任务，然后使用用户级的调度器（Executor框架）将这些任务映射为固定数量的线程；在底层，操作系统内核将这些线程映射到硬件处理器上。

##### Executor框架的结构和成员

###### Executor框架主要由3大部分组成如下。

- 任务：主要包括**Runnable**和**Callable**两个接口组成。所有被执行的任务都要实现其中的一个接口。

  1. **Runnable**：没有返回结果。工具类Executors可以把一个Runnable对象封装为一个Callable对象（Executors.callable（Runnable task）或Executors.callable（Runnable task，Object resule））。
  2. **Callable**：可以通过Future获取到返回结果。
- 任务的执行：包括任务执行的核心接口**Executor**，以及其子接口**ExecutorService**接口，其实现类有**ThreadPoolExecutor**和**ScheduleThreadPoolExecutor**。

  1. **Executor**：将任务的提交也任务的执行分离开来。

  2. **ThreadPoolExecutor**：是线程池的核心实现类，用来执行被提交的任务。**ThreadPoolExecutor**通常使用工厂类**Executors**来创建。**Executors**可以创建3种类型的ThreadPoolExecutor：**SingleThreadExecutor**、**FixedThreadPool**和**CachedThreadPool**。

     - **FixedThreadPool**。**FixedThreadPool**适用于为了满足资源管理的需求，而需要限制当前线程数量的应用场景，它适用于负载比较重的服务器。

       源码:

       ```java
       public static ExecutorService newFixedThreadPool(int nThreads) {
               return new ThreadPoolExecutor(nThreads, nThreads,
                                             0L, TimeUnit.MILLISECONDS,
                                             new LinkedBlockingQueue<Runnable>());
       }
       //重载 
       public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
             return new ThreadPoolExecutor(nThreads, nThreads,
                                             0L, TimeUnit.MILLISECONDS,
                                           new LinkedBlockingQueue<Runnable>(),
                                             threadFactory);
       }
       ```
       
       
       
     - **SingleThreadExecutor**。**SingleThreadExecutor**适用于需要保证顺序地执行各个任务；并且在任意时间点，不会有多个线程是活动的应用场景。

       使用：

       源码:

       ```java
       public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
               return new FinalizableDelegatedExecutorService
                   (new ThreadPoolExecutor(1, 1,
                                           0L, TimeUnit.MILLISECONDS,
                                           new LinkedBlockingQueue<Runnable>(),
                                           threadFactory));
       }
       //重载
       public static ExecutorService newSingleThreadExecutor() {
               return new FinalizableDelegatedExecutorService
                   (new ThreadPoolExecutor(1, 1,
                                           0L, TimeUnit.MILLISECONDS,
                                           new LinkedBlockingQueue<Runnable>()));
       }
       ```

       

     - **CachedThreadPool**。**CachedThreadPool**是大小无界的线程池，适用于执行很多的短期异步任务的小程序，或者是负载较轻的服务器。

       源码：

       ```java
       public static ExecutorService newCachedThreadPool() {
               return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                             60L, TimeUnit.SECONDS,
                                             new SynchronousQueue<Runnable>());
       }
       //重载
       public static ExecutorService getCachedThreadPool(ThreadFactory threadFactory){
               if (threadFactory != null){
                   return Executors.newCachedThreadPool(threadFactory);
               }
               return Executors.newCachedThreadPool();
       }
       ```

       

  3. **ScheduleThreadPoolExecutor**：可以在给定的延迟后运行命令，或定期执行命令。适用于需要多个后台线程执行周期任务，同时为了满足资源管理的需求而需要限制后台线程的数量的应用场景。ScheduledThreadPoolExecutor通常使用工厂类Executors来创建。Executors可以创建2种类型的ScheduledThreadPoolExecutor。

     - **ScheduledThreadPoolExecutor**：包含若干个线程的ScheduledThreadPoolExecutor。

       源码：

       ```java
       public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
               return new DelegatedScheduledExecutorService
                   (new ScheduledThreadPoolExecutor(1));
       }
       // 重载方法
       public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
               return new DelegatedScheduledExecutorService
                   (new ScheduledThreadPoolExecutor(1, threadFactory));
       }
       ```

       

     - **SingleThreadScheduledExecutor**：只包含一个线程的ScheduledThreadPoolExecutor。

       源码：

       ```java
        public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
               return new DelegatedScheduledExecutorService
                   (new ScheduledThreadPoolExecutor(1));
        }
       // 重载
       public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
               return new DelegatedScheduledExecutorService
                   (new ScheduledThreadPoolExecutor(1, threadFactory));
       }
       ```

       

- 异步计算的结果：包括**Future**接口和其实现类**FutureTask**

  1. **Future**：Java通过上述API返回的是一个**FutureTask**对象。但从API可以看到，Java仅仅保证返回的是一个实现了**Future**接口的对象。在将来的JDK实现中，返回的可能不一定是**FutureTask**。
  2. **FutureTask**：当我们把Runnable接口或Callable接口的实现类提交（submit）给**ThreadPoolExecutor**或**ScheduledThreadPoolExecutor**时，**ThreadPoolExecutor**或**ScheduledThreadPoolExecutor**会 向我们返回一个**FutureTask**对象。





##### ThreadPoolExecutor详解 

**Executor**框架最核心的类是**ThreadPoolExecutor**，它是线程池的实现类，主要由下列4个组件构成。

- **corePool**：核心线程池的大小。
-  **maximumPool**：最大线程池的大小。
-  **BlockingQueue**：用来暂时保存任务的工作队列。
-  **RejectedExecutionHandler**：当**ThreadPoolExecutor**已经关闭或**ThreadPoolExecutor**已经饱和时（达到了最大线程池大小且工作队列已满），**execute()**方法将要调用的Handler。

通过**Executor**框架的工具类**Executors**，可以创建3种类型的**ThreadPoolExecutor**。

- **FixedThreadPool**：**FixedThreadPool**被称为可重用固定线程数的线程池。FixedThreadPool的corePoolSize和maximumPoolSize都被设置为创建FixedThreadPool时指定的参数nThreads。当线程池中的线程数大于corePoolSize时，**keepAliveTime为多余的空闲线程等待新任务的最长时间，超过这个时间后多余的线程将被终止。**这里把keepAliveTime设置为0L，意味着多余的空闲线程会被立即终止。

  1. 如果当前运行的线程数少于corePoolSize，则创建新线程来执行任务。
  2. 在线程池完成预热之后（当前运行的线程数等于corePoolSize），将任务加入LinkedBlockingQueue。
  3. 线程执行完1中的任务后，会在循环中反复从LinkedBlockingQueue获取任务来执行。

  **FixedThreadPool**使用无界队列**LinkedBlockingQueue**作为线程池的工作队列（队列的容量为Integer.MAX_VALUE）。使用无界队列作为工作队列会对线程池带来如下影响。

  1. 当线程池中的线程数达到corePoolSize后，新任务将在无界队列中等待，因此线程池中的线程数不会超过corePoolSize。
  2. 使用无界队列时**maximumPoolSize**将是一个无效参数
  3. 使用无界队列时**keepAliveTime**将是一个无效参数
  4. 由于使用无界队列，运行中的FixedThreadPool（未执行方法shutdown()或shutdownNow()）不会拒绝任务（不会调用**RejectedExecutionHandler.rejectedExecution**方法）。

- **SingleThreadExecutor**：SingleThreadExecutor是使用单个worker线程的Executor。**SingleThreadExecutor**的**corePoolSize**和**maximumPoolSize**被设置为1。其他参数与FixedThreadPool相同。SingleThreadExecutor使用无界队列SingleThreadExecutor的corePoolSize和maximumPoolSize被设置为1。其他参数与FixedThreadPool相同。SingleThreadExecutor使用无界队列LinkedBlockingQueue作为线程池的工作队列（队列的容量为Integer.MAX_VALUE）。SingleThreadExecutor使用无界队列作为工作队列对线程池带来的影响与FixedThreadPool相同，作为线程池的工作队列（队列的容量为Integer.MAX_VALUE）。SingleThreadExecutor使用无界队列作为工作队列对线程池带来的影响与FixedThreadPool相同。

  1. 如果当前运行的线程数少于corePoolSize（即线程池中无运行的线程），则创建一个新线程来执行任务。
  2. 在线程池完成预热之后（当前线程池中有一个运行的线程），将任务加入Linked-BlockingQueue。
  3. 在线程池完成预热之后（当前线程池中有一个运行的线程），将任务加入Linked-BlockingQueue。

- **CachedThreadPool**：是一个会根据需要创建新线程的线程池，CachedThreadPool的**corePoolSize**被设置为**0**，即corePool为空；**maximumPoolSize**被设置为**Integer.MAX_VALUE**，即maximumPool是无界的。这里把keepAliveTime设置为60L，意味着CachedThreadPool中的空闲线程等待新任务的最长时间为60秒，空闲线程超过60秒后将会被终止。**CachedThreadPool**使用没有容量的**SynchronousQueue**作为线程池的工作队列，但CachedThreadPool的maximumPool是无界的。这意味着，**如果主线程提交任务的速度高于maximumPool中线程处理任务的速度时，CachedThreadPool会不断创建新线程。极端情况下，CachedThreadPool会因为创建过多线程而耗尽CPU和内存资源。**
  1. 首先执行**SynchronousQueue. offer**（Runnable task）。如果当前maximumPool中有空闲线程正在执行**SynchronousQueue. poll（keepAliveTime，TimeUnit.NANOSECONDS）**，那么主线程执行offer操作与空闲线程执行的poll操作配对成功，主线程把任务交给空闲线程执行，execute()方法执行完成；否则执行下面的步骤2）。
  2. 当初始maximumPool为空，或者maximumPool中当前没有空闲线程时，将没有线程执行**SynchronousQueue. poll（keepAliveTime，TimeUnit.NANOSECONDS）**。这种情况下，步骤1）将失败。此时CachedThreadPool会创建一个新线程执行任务，execute()方法执行完成。
  3. 在步骤2）中新创建的线程将任务执行完后，会执行**SynchronousQueue. poll（keepAliveTime，TimeUnit.NANOSECONDS）**。这个poll操作会让空闲线程最多在SynchronousQueue中等待60秒钟。如果60秒钟内主线程提交了一个新任务（主线程执行步骤1）），那么这个空闲线程将执行主线程提交的新任务；否则，这个空闲线程将终止。由于空闲60秒的空闲线程会被终止，因此长时间保持空闲的CachedThreadPool不会使用任何资源。