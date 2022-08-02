package com.fanruan.proxy.interceptor;

public class InterceptorUtils {
    private static final String[] EXCLUDED_METHOD_LIST = new String[]{
            "toString",
            "hashCode",
            "setClient"
    };

    public static boolean isInExcludedList(String methodName){
        for(String ex : EXCLUDED_METHOD_LIST){
            if(ex.equals(methodName)){
                return true;
            }
        }
        return false;
    }
}
