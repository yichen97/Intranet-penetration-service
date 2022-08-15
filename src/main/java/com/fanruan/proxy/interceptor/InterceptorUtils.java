package com.fanruan.proxy.interceptor;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * @author Yichen Dai
 */
public class InterceptorUtils {
    private static final String[] EXCLUDED_METHOD_LIST = new String[]{
            "toString",
            "hashCode",
            "setInfo",
            "setSql",
            "getSql",
            "setID",
            "getID"
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

    private final static String[] BIND_LIST = new String[]{
            ".*MyDriver.*",
            ".*MyConnection.*",
            ".*MyStatement.*",
            ".*MyPreparedStatement.*",
            ".*MyResultSet.*",
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
        if(n == 0) {
            throw new RuntimeException("the class name invoked is wrong");
        }
        return arr[n-1];
    }

    public static boolean isInBindList(Object o){
        if (o == null) {
            return false;
        }
        return isInBindList(o.getClass().getName());
    }

    public static boolean isInBindList(String className){
        for(String pattern : BIND_LIST){
            if(Pattern.matches(pattern, className)){
                return true;
            }
        }
        return false;
    }

    public static void setInvokeHelper(Object o, String methodName, String ID){
        try {
            Method method = o.getClass().getDeclaredMethod(methodName, String.class);
            method.invoke(o, ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T getInvokeHelper(Object o, String methodName, Class<?> T){
        try {
            Method method = o.getClass().getDeclaredMethod(methodName);
            T res = (T) method.invoke(o);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
