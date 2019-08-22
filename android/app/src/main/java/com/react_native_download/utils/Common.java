package com.react_native_download.utils;

public class Common {
    /** 自定义进制(0,1没有加入,容易与o,l混淆) */
    private static final char[] RANDOMS = new char[]{'q', 'w', 'e', '8', 'a', 's', '2', 'd', 'z', 'x', '9', 'c', '7', 'p', '5', 'i', 'k', '3', 'm', 'j', 'u', 'f', 'r', '4', 'v', 'y', 'l', 't', 'n', '6', 'b', 'g', 'h'};

    /** 序列最小长度 */
    private static final int minLength = 6;

    /**
     * 生成随机码
     */
    public static String createUUID (){

        StringBuffer buffer = new StringBuffer("");

        for (int i = 0; i < minLength; i++) {
            int index = (int)Math.floor(Math.random() * RANDOMS.length);
            buffer.append(RANDOMS[index]);
        }

        return buffer.toString();
    }

    public static void main(String[] args){
        String result =  createUUID();
//        String result = "Run Main";
        System.out.println(result);
    }

}
