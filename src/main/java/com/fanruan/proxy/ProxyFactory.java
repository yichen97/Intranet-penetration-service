package com.fanruan.proxy;


import com.corundumstudio.socketio.SocketIOClient;
import com.fanruan.proxy.interceptor.FetchInterceptor;
import com.fanruan.proxy.interceptor.NotifyInterceptor;
import net.sf.cglib.proxy.Enhancer;

public class ProxyFactory {

    public static Object getNotifyProxy(Class<?> clazz, SocketIOClient client){
        // 创建动态代理增强类
        final Enhancer enhancer = new Enhancer();
        // 设置类加载器
        enhancer.setClassLoader(clazz.getClassLoader());
        // 设置父类
        enhancer.setSuperclass(clazz);
        // 设置被代理类
        enhancer.setCallback(new NotifyInterceptor(clazz, client));
        // 创建代理类
        return enhancer.create();
    }

    public static Object getFetchProxy(Class<?> clazz, SocketIOClient client){
        // 创建动态代理增强类
        final Enhancer enhancer = new Enhancer();
        // 设置类加载器
        enhancer.setClassLoader(clazz.getClassLoader());
        // 设置父类
        enhancer.setSuperclass(clazz);
        // 设置被代理类
        enhancer.setCallback(new FetchInterceptor(clazz, client));
        // 创建代理类
        return enhancer.create();
    }
}
