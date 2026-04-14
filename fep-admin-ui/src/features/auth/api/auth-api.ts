import { httpClient } from '@/shared/http/client';
import type { MenuTreeNode } from '@/shared/types/menu-tree-node';

/** 对应 com.puchain.fep.web.auth.domain.CaptchaResponse */
export interface CaptchaResponse {
  /** 验证码 UUID（登录时随 captchaCode 一起回传） */
  captchaId: string;
  /** base64 编码的 PNG 图片，已含 "data:image/png;base64," 前缀 */
  imageBase64: string;
  /** 有效期秒数 */
  ttlSeconds: number;
}

/** 对应 com.puchain.fep.web.auth.domain.PublicKeyResponse */
export interface PublicKeyResponse {
  /** SM2 公钥（Base64 或 MOCK_ 前缀占位） */
  publicKeyBase64: string;
  /** 公钥版本标识（登录时随 encryptedPassword 一起回传后端） */
  keyId: string;
  /** 算法标识，固定 SM2 */
  algorithm: string;
}

/** 对应 com.puchain.fep.web.auth.domain.LoginRequest */
export interface LoginRequest {
  /** 登录账号：字母开头 6-20 位字母数字下划线（后端 @Pattern 校验） */
  account: string;
  /** 明文密码（仅 plaintext 模式使用，sm2/mock 模式置空） */
  password?: string;
  /** 验证码 UUID（取自 CaptchaResponse.captchaId） */
  captchaId: string;
  /** 用户输入的验证码（4 位） */
  captchaCode: string;
  /** SM2 加密后的密码（sm2/mock 模式使用） */
  encryptedPassword?: string;
  /** 公钥版本标识（与 encryptedPassword 配套） */
  keyId?: string;
}

/** 对应 com.puchain.fep.web.auth.domain.LoginResponse（扁平结构，无嵌套 user 对象） */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  userAccount: string;
  userName: string;
  /** 角色编码列表（仅角色码；权限点通过 GET /auth/me 单独获取，见 UserInfoResponse.permissions） */
  roleCodes: string[];
  /** 是否需要修改密码（首次登录或过期） */
  passwordChangeRequired: boolean;
}

/** 对应 com.puchain.fep.web.auth.domain.RefreshRequest */
export interface RefreshRequest {
  refreshToken: string;
}

/** 对应 com.puchain.fep.web.auth.domain.UserInfoResponse（P7.1 扩展，含权限点与菜单树） */
export interface UserInfoResponse {
  /** 用户 ID */
  userId: string;
  /** 登录账号 */
  userAccount: string;
  /** 显示姓名 */
  userName: string;
  /** 联系电话（可为空） */
  phone: string | null;
  /** 邮箱（可为空） */
  email: string | null;
  /** 所属部门（可为空） */
  department: string | null;
  /** 角色编码列表 */
  roleCodes: string[];
  /** 权限点编码列表（形如 sys:user:list） */
  permissions: string[];
  /** 授权菜单树（按角色过滤后的结构） */
  menuTree: MenuTreeNode[];
}

export const authApi = {
  captcha: (): Promise<CaptchaResponse> =>
    httpClient.get('/api/v1/auth/captcha'),
  getPublicKey: (): Promise<PublicKeyResponse> =>
    httpClient.get('/api/v1/auth/public-key'),
  login: (payload: LoginRequest): Promise<LoginResponse> =>
    httpClient.post('/api/v1/auth/login', payload),
  logout: (): Promise<void> => httpClient.post('/api/v1/auth/logout'),
  refresh: (payload: RefreshRequest): Promise<LoginResponse> =>
    httpClient.post('/api/v1/auth/refresh', payload),
  getMe: (): Promise<UserInfoResponse> => httpClient.get('/api/v1/auth/me'),
};
