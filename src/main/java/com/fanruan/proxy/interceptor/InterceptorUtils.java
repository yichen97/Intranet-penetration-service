package com.fanruan.proxy.interceptor;

import java.util.regex.Pattern;

public class InterceptorUtils {
    private static final String[] EXCLUDED_METHOD_LIST = new String[]{
            "toString",
            "hashCode",
            "setInfo",
            "setSql",
            "getSql"
    };

    private static final String[] NEED_REPLY_LIST = new String[]{
            "next",
    };

    private static final String[] WRAPPER_CLASS_LIST = new String[]{
            "Boolean",
            "Integer",
            "Double",
            "Long",
            "Character",
            "Byte",
            "Short",
            "Float"
    };

    public static boolean isInExcludedList(String methodName){
        for(String ex : EXCLUDED_METHOD_LIST){
            if(ex.equals(methodName)){
                return true;
            }
        }
        return false;
    }

    public static boolean isInReplyList(String methodName){
        String pattern = "get.*";
        boolean isMatch = Pattern.matches(pattern, methodName);

        for(String ex : NEED_REPLY_LIST){
            if(ex.equals(methodName)){
                return true;
            }
        }
        return isMatch;
    }
    
    public static boolean isWraps(Object o){
        for(String ex : WRAPPER_CLASS_LIST){
            if(ex.equals(getClassName(o.getClass().getName()))){
                return true;
            }
        }
        return false;
    }

    public static String getClassName(String fullyQualifiedClassName){
        String[] arr = fullyQualifiedClassName.split("\\.");
        int n = arr.length;
        if(n == 0) throw new RuntimeException("the class name invoked is wrong");
        return arr[n-1];
    }
}
