package com.freestyledash.curryx.common.util;

/**
 * 字符串常用操作工具类
 */
public abstract class StringUtil {

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

    /**
     * 判断字符串<code>str1</code>和字符串<code>str2</code>是否相等
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return true 若两个字符串均为空或具有相同的内容 false 若两个字符串不相等
     */
    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

}
