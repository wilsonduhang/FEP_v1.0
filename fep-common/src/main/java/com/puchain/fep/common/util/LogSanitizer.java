package com.puchain.fep.common.util;

/**
 * 日志输入清理工具 — 防止 CRLF 注入 (CWE-117)。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class LogSanitizer {

    private LogSanitizer() { }

    /**
     * 替换字符串中的 CR/LF 字符为转义形式。
     *
     * @param input 待清理字符串（可为 null）
     * @return 清理后的字符串，null 输入返回空字符串
     */
    public static String sanitize(final String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\r", "\\r").replace("\n", "\\n");
    }
}
