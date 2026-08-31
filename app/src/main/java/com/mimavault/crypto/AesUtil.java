package com.mimavault.crypto;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM 加解密工具，与 PC 端 util/AesUtil.java 1:1 兼容
 *
 * 密钥派生：
 * - 新版：PBKDF2WithHmacSHA256（盐 16 字节，迭代 600000，输出 256 位），
 *   存储格式 pbkdf2$迭代次数$盐hex$哈希hex（settings.master_hash）
 * - 旧版（兼容迁移）：主密码直接 SHA-256 派生
 *
 * GCM 密文格式：Base64(iv(12B) + ciphertext)，与 PC 端完全一致
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    public static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    public static final int PBKDF2_SALT_LENGTH = 16;
    public static final int PBKDF2_ITERATIONS = 600000;
    public static final int PBKDF2_KEY_LENGTH = 256;
    public static final String PBKDF2_PREFIX = "pbkdf2$";

    private static final SecureRandom RANDOM = new SecureRandom();

    private AesUtil() {
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[PBKDF2_SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /** 由主密码经 PBKDF2 派生 AES-256 密钥 */
    public static SecretKey deriveKeyPbkdf2(char[] masterPassword, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(masterPassword, salt, iterations, PBKDF2_KEY_LENGTH);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
                byte[] derived = factory.generateSecret(spec).getEncoded();
                return new SecretKeySpec(derived, ALGORITHM);
            } finally {
                spec.clearPassword();
            }
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 密钥派生失败", e);
        }
    }

    /** PBKDF2 派生原始字节（用于存储校验哈希） */
    public static byte[] pbkdf2Bytes(char[] masterPassword, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(masterPassword, salt, iterations, PBKDF2_KEY_LENGTH);
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
                return factory.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 哈希失败", e);
        }
    }

    /** 生成新格式存储串：pbkdf2$迭代次数$盐hex$哈希hex */
    public static String buildPbkdf2Record(char[] masterPassword, byte[] salt, int iterations) {
        byte[] hash = pbkdf2Bytes(masterPassword, salt, iterations);
        return PBKDF2_PREFIX + iterations + "$" + toHex(salt) + "$" + toHex(hash);
    }

    /** 校验主密码：自动识别新/旧格式 */
    public static boolean verifyPassword(char[] masterPassword, String stored) {
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        if (stored.startsWith(PBKDF2_PREFIX)) {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = fromHex(parts[2]);
                byte[] expected = fromHex(parts[3]);
                byte[] actual = pbkdf2Bytes(masterPassword, salt, iterations);
                return MessageDigest.isEqual(expected, actual);
            } catch (Exception e) {
                return false;
            }
        }
        return MessageDigest.isEqual(stored.toLowerCase().getBytes(StandardCharsets.UTF_8),
                sha256Hex(new String(masterPassword)).getBytes(StandardCharsets.UTF_8));
    }

    /** 是否为旧版 SHA-256 格式 */
    public static boolean isLegacyRecord(String stored) {
        return stored != null && !stored.isEmpty() && !stored.startsWith(PBKDF2_PREFIX);
    }

    /** 从存储串解析盐（仅新格式） */
    public static byte[] saltFromRecord(String stored) {
        if (stored == null || !stored.startsWith(PBKDF2_PREFIX)) {
            return null;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return null;
        }
        return fromHex(parts[2]);
    }

    /** 从存储串解析迭代次数（仅新格式） */
    public static int iterationsFromRecord(String stored) {
        if (stored == null || !stored.startsWith(PBKDF2_PREFIX)) {
            return PBKDF2_ITERATIONS;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return PBKDF2_ITERATIONS;
        }
        return Integer.parseInt(parts[1]);
    }

    /** 【旧版】SHA-256 派生 AES 密钥（仅兼容迁移/旧备份） */
    public static SecretKey deriveKey(String masterPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(masterPassword.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, ALGORITHM);
        } catch (Exception e) {
            throw new IllegalStateException("密钥派生失败", e);
        }
    }

    /** 【旧版】SHA-256 十六进制哈希 */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("哈希计算失败", e);
        }
    }

    /** 加密明文，返回 Base64(iv + ciphertext)，与 PC 端一致（标准编码无换行） */
    public static String encrypt(String plainText, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
            try {
                byte[] encrypted = cipher.doFinal(plainBytes);
                byte[] combined = new byte[iv.length + encrypted.length];
                System.arraycopy(iv, 0, combined, 0, iv.length);
                System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
                return Base64.encodeToString(combined, Base64.NO_WRAP);
            } finally {
                Arrays.fill(plainBytes, (byte) 0);
            }
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /** 解密 Base64(iv + ciphertext) */
    public static String decrypt(String cipherText, SecretKey key) {
        try {
            byte[] combined = Base64.decode(cipherText, Base64.NO_WRAP);
            try {
                byte[] iv = new byte[IV_LENGTH];
                System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
                byte[] encrypted = new byte[combined.length - IV_LENGTH];
                System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
                byte[] decrypted = cipher.doFinal(encrypted);
                try {
                    return new String(decrypted, StandardCharsets.UTF_8);
                } finally {
                    Arrays.fill(decrypted, (byte) 0);
                }
            } finally {
                Arrays.fill(combined, (byte) 0);
            }
        } catch (Exception e) {
            throw new IllegalStateException("解密失败，主密码可能不正确", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static byte[] fromHex(String hex) {
        return hexToBytes(hex);
    }
}
