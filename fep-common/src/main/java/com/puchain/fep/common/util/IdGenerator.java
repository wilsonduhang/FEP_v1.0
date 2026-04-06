package com.puchain.fep.common.util;

import java.util.UUID;

/**
 * ID 生成工具。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class IdGenerator {

    private IdGenerator() { }

    /**
     * 生成 32 字符的 UUID（无横线）。
     *
     * @return 32 字符 hex UUID
     */
    public static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
