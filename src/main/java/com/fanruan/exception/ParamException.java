package com.fanruan.exception;
import com.fanruan.utils.CodeMsg;

public class ParamException extends Exception{

    CodeMsg msg;

    public ParamException(CodeMsg codeMsg){
        this.msg = codeMsg;
    }

    @Override
    public String getMessage() {
        return "error code: " + msg.getCode()
                + "error message: " + msg.getMsg();
    }

}
