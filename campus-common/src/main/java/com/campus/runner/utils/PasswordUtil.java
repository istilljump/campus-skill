package com.campus.runner.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

/**
 * 密码哈希工具：BCrypt 新口径 + 旧 MD5 兼容（渐进迁移）
 *
 * <p>历史存储口径为 MD5(salt + 明文)，抗碰撞性不足；本工具切换为 BCrypt（自带随机盐与慢哈希）。
 * 兼容策略：校验时按存储串前缀自动选择口径（$2 开头为 BCrypt），旧口径校验通过后由调用方
 * 调用 encode 重写存储串，实现登录一次即升级一次，无需一次性重置全部用户密码。
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    /**
     * BCrypt 加密（新口径）。salt 参数保留以兼容旧表结构，不参与计算
     */
    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }

    /**
     * 校验：存储串为 BCrypt 时按新口径校验，否则回退旧口径 MD5(salt + 明文)
     */
    public static boolean matches(String raw, String salt, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (stored.startsWith("$2")) {
            try {
                return ENCODER.matches(raw, stored);
            } catch (IllegalArgumentException e) {
                //存储串损坏按校验失败处理
                return false;
            }
        }
        return stored.equalsIgnoreCase(legacyMd5(raw, salt));
    }

    /**
     * 旧口径 MD5(salt + 明文)，仅用于兼容历史数据
     */
    public static String legacyMd5(String raw, String salt) {
        return DigestUtils.md5DigestAsHex((salt + raw).getBytes());
    }

    /**
     * 是否为旧 MD5 口径（需要重哈希升级）
     */
    public static boolean needsRehash(String stored) {
        return stored != null && !stored.startsWith("$2");
    }
}
