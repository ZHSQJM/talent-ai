package com.talent.ai.spring;

import lombok.Data;

/**
 * @author watson
 * @date 2026年/11月/05日/19:47
 */
@Data
public class TalentResult<T> {



        private int code;
        private boolean success;
        private String message;
        private T data;

    public TalentResult(){
    }

    public TalentResult(int code, String message, T data,boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }


        /**
         * 成功
         */
        public static <T> TalentResult<T> success() {
        return success(null);
    }

        /**
         * 成功
         */
        public static <T> TalentResult<T> success(T data) {
        TalentResult<T> responseResult = new TalentResult<>();
        responseResult.setCode(200);
        responseResult.setMessage("成功");
        responseResult.setData(data);
        responseResult.setSuccess(true);
        return responseResult;
    }


        /**
         * 失败
         */
        public static <T> TalentResult<T> error(Integer code, String message) {
        TalentResult<T> responseResult = new TalentResult<>();
        responseResult.setCode(code);
        responseResult.setMessage(message);
        responseResult.setData(null);
        responseResult.setSuccess(false);
        return responseResult;
    }




    }
