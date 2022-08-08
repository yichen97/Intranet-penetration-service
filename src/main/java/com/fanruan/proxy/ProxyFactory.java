package com.fanruan.proxy;


import com.fanruan.proxy.interceptor.Interceptor;
import net.sf.cglib.proxy.Enhancer;

import java.util.Properties;

public class ProxyFactory {

    public static Object getProxy(Class<?> clazz, Properties info){
        // 创建动态代理增强类
        final Enhancer enhancer = new Enhancer();
        // 设置类加载器
        enhancer.setClassLoader(clazz.getClassLoader());
        // 设置父类
        enhancer.setSuperclass(clazz);
        // 设置被代理类
        enhancer.setCallback(new Interceptor(clazz, info));
        // 创建代理类
        Object proxy = enhancer.create();
        return proxy;
    }
}
