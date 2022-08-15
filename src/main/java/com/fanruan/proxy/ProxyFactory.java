package com.fanruan.proxy;


import com.fanruan.proxy.interceptor.Interceptor;
import net.sf.cglib.proxy.Enhancer;

import java.util.Properties;

/**
 * @author Yichen Dai
 */
public class ProxyFactory {

    public static Object getProxy(Class<?> clazz, Properties info){
        final Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(clazz.getClassLoader());
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(new Interceptor(clazz, info));
        return enhancer.create();
    }
}
