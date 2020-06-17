package com.qjx.lock;

import javax.print.attribute.standard.Finishings;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;


/**
 * 独占锁就是在同一时刻只能有一个线程获取到锁，而其他获取锁的线程只能处于同步队列中等待，只有获取锁的线程释放了锁，后继的线程才能够获取锁。
 * Mutex中定义了一个静态内部类，该内部类继承了同步器并实现了独占式获取和释放同步状态。
 * @author qjx
 */
public class Mutex implements Lock {

    // 静态内部类，自定义同步器
    private static class Sync extends AbstractQueuedLongSynchronizer{

        // 是否处于占用状态
        protected boolean isHeldExclusively(){
            return getState() == 1;
        }

        // 当前状态为0时获取锁 ，如果经过CAS设置成功（同步状态设置为1），则代表获取了同步状态
        public boolean tryAcquire(int acquires){
            if (compareAndSetState(0,1)){
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 释放锁，将状态设置为0


        protected boolean tryRelease(int release){
            if (getState() == 0){
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
        // 返回一个Condition，每个Condition都含有一个Condition队列
        Condition newCondition(){
            return new ConditionObject();
        }
    }
    // 把操作都代理到Sync上
    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1,unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}
