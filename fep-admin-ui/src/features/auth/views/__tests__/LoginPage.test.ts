// src/features/auth/views/__tests__/LoginPage.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
import ElementPlus from 'element-plus';
import LoginPage from '../LoginPage.vue';

vi.mock('@/features/auth/api/auth-api', () => ({
  authApi: {
    captcha: vi.fn().mockResolvedValue({
      captchaId: 'cap-1',
      imageBase64: 'data:image/png;base64,xxx',
      ttlSeconds: 300,
    }),
    login: vi.fn().mockResolvedValue({
      accessToken: 'AT',
      refreshToken: 'RT',
      userId: 'u1',
      userAccount: 'alice',
      userName: '张三',
      roleCodes: ['ADMIN'],
      passwordChangeRequired: false,
    }),
    logout: vi.fn(),
    refresh: vi.fn(),
  },
}));

import { authApi } from '@/features/auth/api/auth-api';

function buildWrapper() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', component: LoginPage },
      { path: '/home', component: { template: '<div>home</div>' } },
    ],
  });
  return mount(LoginPage, {
    global: { plugins: [createPinia(), ElementPlus, router] },
  });
}

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('LoginPage validation', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('rejects account not starting with letter', async () => {
    const w = buildWrapper();
    await flush();
    await w.find('input[name="account"]').setValue('1abc');
    await w.find('input[name="password"]').setValue('Abc12345');
    await w.find('input[name="captchaCode"]').setValue('1234');
    await w.find('form').trigger('submit');
    expect(w.text()).toContain('账号格式错误，请检查');
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('rejects weak password (only lowercase letters)', async () => {
    const w = buildWrapper();
    await flush();
    await w.find('input[name="account"]').setValue('alicex');
    await w.find('input[name="password"]').setValue('abcdefgh');
    await w.find('input[name="captchaCode"]').setValue('1234');
    await w.find('form').trigger('submit');
    expect(w.text()).toContain('密码强度不足');
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('rejects captcha not exactly 4 chars', async () => {
    const w = buildWrapper();
    await flush();
    await w.find('input[name="account"]').setValue('alicex');
    await w.find('input[name="password"]').setValue('Abc12345');
    await w.find('input[name="captchaCode"]').setValue('12');
    await w.find('form').trigger('submit');
    expect(w.text()).toContain('验证码错误或已失效');
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('calls authApi.login with flat payload on valid input', async () => {
    const w = buildWrapper();
    await flush();
    await w.find('input[name="account"]').setValue('alicex');
    await w.find('input[name="password"]').setValue('Abc12345');
    await w.find('input[name="captchaCode"]').setValue('1234');
    await w.find('form').trigger('submit');
    await flush();
    expect(authApi.login).toHaveBeenCalledWith({
      account: 'alicex',
      password: 'Abc12345',
      captchaId: 'cap-1',
      captchaCode: '1234',
    });
  });

  it('loads captcha on mount and binds imageBase64 to <img src>', async () => {
    const w = buildWrapper();
    await flush();
    expect(authApi.captcha).toHaveBeenCalledOnce();
    const img = w.find('img.captcha-img');
    expect(img.exists()).toBe(true);
    expect(img.attributes('src')).toBe('data:image/png;base64,xxx');
  });
});
