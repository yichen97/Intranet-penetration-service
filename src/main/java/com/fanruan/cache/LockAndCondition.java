package com.fanruan.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockAndCondition{
    private ReentrantLock lock;
    private Condition condition;
    private Object result;
    private String BindingID;

    LockAndCondition(ReentrantLock lock, Condition condition){
        this.lock = lock;
        this.condition = condition;
    }
}
