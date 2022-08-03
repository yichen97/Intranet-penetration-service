package com.fanruan.cache;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ResultWrapper {

    final public ReentrantLock lock = new ReentrantLock();
    final public Condition condition = lock.newCondition();
}
