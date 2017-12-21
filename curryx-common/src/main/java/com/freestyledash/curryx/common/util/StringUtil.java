package com.freestyledash.curryx.common.util;

/**
 * 字符串常用操作工具类
 *
 * @author zhangyanqi
 * @since 1.0 2017/11/8
 */
public final class StringUtil {

    private StringUtil() {

    }

    /**
     * 判断字符串<code>str</code>是否为空
     *
     * @param str 要判断的字符串
     * @return true 若字符串为空 false 若字符串非空
     */
    public static boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }
}
