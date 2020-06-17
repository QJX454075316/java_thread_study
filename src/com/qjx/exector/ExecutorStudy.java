package com.qjx.exector;
import java.util.concurrent.*;


public class ExecutorStudy {
    public static void main(String[] args) {

    }

    public static ExecutorService getFixedThreadPool(int nThread,ThreadFactory threadFactory){
        if (threadFactory != null){
            return Executors.newFixedThreadPool(nThread,threadFactory);
        }
        return Executors.newFixedThreadPool(nThread);
    }


    public static  ExecutorService getSingleThreadExecutor(ThreadFactory threadFactory){
        if (threadFactory != null){
            return Executors.newSingleThreadExecutor(threadFactory);
        }
        return Executors.newSingleThreadExecutor();
    }

    public static ExecutorService getCachedThreadPool(ThreadFactory threadFactory){
        if (threadFactory != null){
            return Executors.newCachedThreadPool(threadFactory);
        }
        return Executors.newCachedThreadPool();
    }


    public static ScheduledExecutorService getSingleThreadScheduledExecutor(int i){
        if(i > 0) {
            return Executors.newScheduledThreadPool(i);
        }else{
            return Executors.newSingleThreadScheduledExecutor();
        }
    }
}
