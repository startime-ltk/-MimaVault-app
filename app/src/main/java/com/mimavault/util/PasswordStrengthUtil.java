package com.mimavault.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 密码强度检测工具，与 PC 端 PasswordStrengthUtil 完全一致
 * 评分规则：黑名单直判弱；长度≥8/≥12 各 1 分；字符种类每类 1 分（共 4 类）；
 * 纯数字/纯字母封顶 3 分；0~2 弱，3~4 中，5~6 强
 */
public final class PasswordStrengthUtil {

    public enum Level {
        WEAK("弱"), MEDIUM("中"), STRONG("强");
        private final String text;
        Level(String text) { this.text = text; }
        public String getText() { return text; }
    }

    public static final Set<String> COMMON_WEAK = new HashSet<>(Arrays.asList(
            "123456", "123456789", "12345678", "1234567", "123123", "111111",
            "password", "password1", "123qwe", "qwerty", "qwerty123", "abc123",
            "abc123456", "admin", "admin123", "root", "root123", "test", "test123",
            "iloveyou", "monkey", "dragon", "welcome", "sunshine", "666666",
            "88888888", "000000", "a123456", "1qaz2wsx", "aa123456", "a1b2c3"
    ));

    public static final class Result {
        public final Level level;
        public final int score;
        public final List<String> reasons;

        Result(Level level, int score, List<String> reasons) {
            this.level = level;
            this.score = score;
            this.reasons = reasons;
        }

        @Override
        public String toString() {
            return level.getText() + "(得分 " + score + "/6)";
        }
    }

    public static Result evaluate(String password) {
        List<String> reasons = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            reasons.add("未设置密码");
            return new Result(Level.WEAK, 0, reasons);
        }
        if (COMMON_WEAK.contains(password)) {
            reasons.add("命中常见弱密码黑名单");
            return new Result(Level.WEAK, 0, reasons);
        }
        int score = 0;
        int len = password.length();
        if (len >= 8) {
            score += 1;
            reasons.add("长度≥8(+1)");
        }
        if (len >= 12) {
            score += 1;
            reasons.add("长度≥12(+1)");
        }
        boolean lower = password.matches(".*[a-z].*");
        boolean upper = password.matches(".*[A-Z].*");
        boolean digit = password.matches(".*\\d.*");
        boolean special = password.matches(".*[^a-zA-Z0-9].*");
        int kinds = 0;
        if (lower) { kinds++; reasons.add("含小写字母(+1)"); }
        if (upper) { kinds++; reasons.add("含大写字母(+1)"); }
        if (digit) { kinds++; reasons.add("含数字(+1)"); }
        if (special) { kinds++; reasons.add("含特殊符号(+1)"); }
        score += kinds;
        boolean pureDigit = password.matches("\\d+");
        boolean pureLetter = password.matches("[a-zA-Z]+");
        if (pureDigit || pureLetter) {
            if (score > 3) {
                score = 3;
            }
            reasons.add("纯数字/纯字母，强度封顶为中");
        }
        Level level;
        if (score <= 2) {
            level = Level.WEAK;
            reasons.add("总得分较低");
        } else if (score <= 4) {
            level = Level.MEDIUM;
        } else {
            level = Level.STRONG;
        }
        return new Result(level, score, reasons);
    }

    /** 明文密码一句话强度描述，如 "强(得分 6/6)" */
    public static String describePlain(String password) {
        return evaluate(password).toString();
    }

    /** 明文密码是否为弱 */
    public static boolean weakPlain(String password) {
        return evaluate(password).level == Level.WEAK;
    }
}
