package com.mimavault.service;

import java.security.SecureRandom;

/**
 * 随机强密码生成器（大小写+数字+符号）
 */
public class PasswordGenerator {

    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?";

    public static String generate(int length) {
        if (length < 8) {
            length = 8;
        }
        StringBuilder all = new StringBuilder(LOWER).append(UPPER).append(DIGITS).append(SYMBOLS);
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        // 保证四类至少各一个
        sb.append(LOWER.charAt(rnd.nextInt(LOWER.length())));
        sb.append(UPPER.charAt(rnd.nextInt(UPPER.length())));
        sb.append(DIGITS.charAt(rnd.nextInt(DIGITS.length())));
        sb.append(SYMBOLS.charAt(rnd.nextInt(SYMBOLS.length())));
        for (int i = 4; i < length; i++) {
            sb.append(all.charAt(rnd.nextInt(all.length())));
        }
        // 洗牌
        for (int i = sb.length() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char t = sb.charAt(i);
            sb.setCharAt(i, sb.charAt(j));
            sb.setCharAt(j, t);
        }
        return sb.toString();
    }
}
