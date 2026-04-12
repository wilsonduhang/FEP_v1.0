<!-- 注意：属性换行是为满足 eslint-plugin-vue 默认规则 vue/max-attributes-per-line（单属性以上须分行）。 -->
<template>
  <div class="login-card">
    <h2>FEP 综合前置平台</h2>
    <form
      novalidate
      @submit.prevent="onSubmit"
    >
      <div class="row">
        <label>账号</label>
        <input
          v-model="form.account"
          name="account"
          maxlength="20"
          autocomplete="username"
        >
      </div>
      <div class="row">
        <label>密码</label>
        <input
          v-model="form.password"
          name="password"
          type="password"
          maxlength="20"
          autocomplete="current-password"
        >
      </div>
      <div class="row captcha-row">
        <label>验证码</label>
        <input
          v-model="form.captchaCode"
          name="captchaCode"
          maxlength="4"
        >
        <img
          v-if="captchaImage"
          :src="captchaImage"
          class="captcha-img"
          alt="captcha"
          @click="loadCaptcha"
        >
      </div>
      <p
        v-if="errorMessage"
        class="error"
      >
        {{ errorMessage }}
      </p>
      <button
        type="submit"
        :disabled="submitting"
      >
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '@/features/auth/api/auth-api';
import { useAuthStore } from '@/stores/auth';

const form = reactive({ account: '', password: '', captchaCode: '' });
const captchaImage = ref<string>('');
const captchaId = ref<string>('');
const errorMessage = ref<string>('');
const submitting = ref(false);

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const ACCOUNT_RE = /^[A-Za-z][A-Za-z0-9_]{5,19}$/;

function validatePasswordStrength(pw: string): boolean {
  if (pw.length < 8 || pw.length > 20) return false;
  let categories = 0;
  if (/[A-Z]/.test(pw)) categories += 1;
  if (/[a-z]/.test(pw)) categories += 1;
  if (/[0-9]/.test(pw)) categories += 1;
  return categories >= 2;
}

async function loadCaptcha() {
  try {
    const res = await authApi.captcha();
    captchaId.value = res.captchaId;
    // imageBase64 后端已含 "data:image/png;base64," 前缀，直接赋值
    captchaImage.value = res.imageBase64;
  } catch {
    errorMessage.value = '验证码加载失败，请刷新页面重试';
  }
}

async function onSubmit() {
  errorMessage.value = '';
  if (!ACCOUNT_RE.test(form.account)) {
    errorMessage.value = '账号格式错误，请检查';
    return;
  }
  if (!validatePasswordStrength(form.password)) {
    errorMessage.value = '密码强度不足，需包含大小写字母和数字';
    return;
  }
  if (form.captchaCode.length !== 4) {
    errorMessage.value = '验证码错误或已失效';
    return;
  }
  submitting.value = true;
  try {
    await authStore.login({
      account: form.account,
      password: form.password,
      captchaId: captchaId.value,
      captchaCode: form.captchaCode,
    });
    const redirect = (route.query.redirect as string) || '/home';
    router.push(redirect);
  } catch {
    // 错误 toast 由 httpClient 拦截器统一处理；此处仅刷新验证码 + 清空输入
    await loadCaptcha();
    form.captchaCode = '';
  } finally {
    submitting.value = false;
  }
}

onMounted(loadCaptcha);
</script>

<style scoped>
.login-card {
  background: #fff;
  padding: 32px 40px;
  border-radius: 8px;
  width: 360px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}
h2 { margin: 0 0 24px; text-align: center; color: #1f2d5a; }
.row { display: flex; flex-direction: column; margin-bottom: 16px; }
.row label { font-size: 13px; color: #555; margin-bottom: 4px; }
.row input { padding: 8px 12px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
.captcha-row { flex-direction: row; align-items: flex-end; gap: 8px; }
.captcha-row label { width: 100%; margin-bottom: 4px; }
.captcha-img { height: 36px; cursor: pointer; border: 1px solid #ccc; }
.error { color: #f56c6c; font-size: 13px; margin: 0 0 12px; }
button { width: 100%; padding: 10px; background: #409eff; color: #fff; border: 0; border-radius: 4px; cursor: pointer; }
button:disabled { background: #a0cfff; cursor: not-allowed; }
</style>
