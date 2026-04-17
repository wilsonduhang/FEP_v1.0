package com.puchain.fep.web.auth.service;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.auth.RedisKeyConstants;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.wf.captcha.SpecCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 图形验证码服务。
 *
 * <p>参见 PRD v1.3 §5.1.3: 4 位，5 分钟有效，仅能使用一次。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int CAPTCHA_WIDTH = 130;
    private static final int CAPTCHA_HEIGHT = 48;
    private static final int CAPTCHA_LENGTH = 4;

    private final StringRedisTemplate redisTemplate;

    /**
     * E2E 测试 bypass token。非空时，等于该值的用户输入即视为验证码校验通过
     * （不查 Redis、不消费）。默认空字符串（禁用），仅通过 {@code dev-e2e}
     * profile 或 {@code FEP_E2E_CAPTCHA_BYPASS_TOKEN} 环境变量激活。
     * 生产环境必须保持未配置（默认空串）。
     */
    private final String bypassToken;

    /**
     * 构造 CaptchaService。
     *
     * @param redisTemplate Spring Redis 模板
     * @param bypassToken   E2E captcha bypass token (empty string = disabled)
     */
    @Autowired
    public CaptchaService(final StringRedisTemplate redisTemplate,
                          @Value("${fep.e2e.captcha-bypass-token:}") final String bypassToken) {
        this.redisTemplate = redisTemplate;
        this.bypassToken = bypassToken;
    }

    /** Minimal 1x1 transparent PNG as data URI for E2E bypass mode. */
    private static final String BYPASS_IMAGE =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVQI12NgAAIABQAB"
            + "Nl7BcQAAAABJRU5ErkJggg==";

    /**
     * 生成一个新的验证码，返回 captchaId + base64 图片。
     *
     * <p>当 E2E bypass token 已配置（非空）时，返回固定 dummy captcha
     * （不访问 Redis），使 E2E 登录流程可在无 Redis 环境下完成。
     * 生产/dev profile 的 bypass token 为空，此分支永远不会执行。</p>
     *
     * @return 包含 captchaId、base64 图片和有效期的响应
     */
    public CaptchaResponse generate() {
        // E2E bypass: return a fixed dummy captcha without touching Redis.
        if (bypassToken != null && !bypassToken.isEmpty()) {
            return new CaptchaResponse("e2e-captcha-" + IdGenerator.uuid32(),
                    BYPASS_IMAGE, TTL.getSeconds());
        }
        SpecCaptcha captcha = new SpecCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_LENGTH);
        captcha.setCharType(SpecCaptcha.TYPE_DEFAULT); // 字母数字混合
        String code = captcha.text().toLowerCase(java.util.Locale.ROOT);
        String captchaId = IdGenerator.uuid32();
        redisTemplate.opsForValue().set(RedisKeyConstants.CAPTCHA_PREFIX + captchaId, code, TTL);
        // SpecCaptcha.toBase64() already returns a full "data:image/png;base64,..." URI;
        // do not prepend the prefix again (would produce double-prefixed string that
        // browsers reject). Fix discovered during P7.1 E2E smoke test.
        return new CaptchaResponse(captchaId, captcha.toBase64(), TTL.getSeconds());
    }

    /**
     * 校验验证码（消费一次）。
     *
     * @param captchaId 验证码 ID
     * @param userInput 用户输入
     * @return true 校验通过（并已从 Redis 删除）
     */
    public boolean verifyAndConsume(final String captchaId, final String userInput) {
        if (captchaId == null || userInput == null) {
            return false;
        }
        // E2E bypass: when an explicit bypass token is configured AND the user input
        // matches it, accept without touching Redis. Default disabled (empty string).
        if (bypassToken != null && !bypassToken.isEmpty()
                && bypassToken.equalsIgnoreCase(userInput)) {
            return true;
        }
        String key = RedisKeyConstants.CAPTCHA_PREFIX + captchaId;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        redisTemplate.delete(key);
        return stored.equalsIgnoreCase(userInput);
    }
}
