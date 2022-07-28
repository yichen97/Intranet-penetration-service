package com.fanruan.cache;

import lombok.Data;

@Data
public class ClientState {
    // 未连接状态
    static public final int STATE_UNCONNECTED = 0;
    // 未注册状态
    static public final int STATE_UNREGISTER = 1;
    // 已连接注册状态
    static public final int STATE_COMPLETE = 2;
    // 初始状态
    private volatile int state = 0;

    public ClientState(){}
}
