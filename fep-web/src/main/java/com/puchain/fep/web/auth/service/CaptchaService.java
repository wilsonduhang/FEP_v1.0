package com.puchain.fep.web.auth.service;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.auth.RedisKeyConstants;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.wf.captcha.SpecCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(StringRedisTemplate.class)
public class CaptchaService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int CAPTCHA_WIDTH = 130;
    private static final int CAPTCHA_HEIGHT = 48;
    private static final int CAPTCHA_LENGTH = 4;

    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 CaptchaService。
     *
     * @param redisTemplate Spring Redis 模板
     */
    @Autowired
    public CaptchaService(final StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成一个新的验证码，返回 captchaId + base64 图片。
     *
     * @return 包含 captchaId、base64 图片和有效期的响应
     */
    public CaptchaResponse generate() {
        SpecCaptcha captcha = new SpecCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_LENGTH);
        captcha.setCharType(SpecCaptcha.TYPE_DEFAULT); // 字母数字混合
        String code = captcha.text().toLowerCase(java.util.Locale.ROOT);
        String captchaId = IdGenerator.uuid32();
        redisTemplate.opsForValue().set(RedisKeyConstants.CAPTCHA_PREFIX + captchaId, code, TTL);
        return new CaptchaResponse(
                captchaId, "data:image/png;base64," + captcha.toBase64(), TTL.getSeconds());
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
        String key = RedisKeyConstants.CAPTCHA_PREFIX + captchaId;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        redisTemplate.delete(key);
        return stored.equalsIgnoreCase(userInput);
    }
}
