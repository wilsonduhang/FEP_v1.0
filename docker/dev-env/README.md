# FEP 本地开发 / 测试环境

> 一条命令拉起 `fep-redis`，并提供与 CI 同基线的可复现 Maven 构建容器。

本目录是 **Windows / 国内网络** 下从零搭建 FEP_v1.0 开发测试环境的产物，
解决两件事：

1. **Redis 依赖** —— `application-dev.yml` 与 CI 都要求一个 `redis:7-alpine`
   实例（容器名 `fep-redis`，端口 `6379`）。单元测试用内存 Mock
   (`TestRedisConfiguration`) 不需要真 Redis；**dev 运行时 + P7.1 E2E 冒烟** 需要。
2. **可复现构建** —— `build` 服务用 `maven:3.9-eclipse-temurin-17`，与 CI 同一
   JDK / Maven 基线，避免「本机能过 CI 不过」。

---

## 1. 前置条件

| 工具 | 版本 | 说明 |
|------|------|------|
| Docker Desktop | ≥ 20.10 | 已验证 29.x |
| JDK | 17 (Temurin) | **仅原生构建路径需要**；容器构建路径不需要宿主装 JDK |
| Maven | 用仓库自带 `./mvnw` | 首次运行自动下载 3.9.9 |

宿主原生环境（本机已配好）：

- `JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`（用户级持久化）
- `%JAVA_HOME%\bin` 已加入用户 PATH
- `~/.m2/settings.xml` 已配阿里云镜像（国内加速，不入仓库、不影响 CI）

---

## 2. 启动 Redis（最常用）

```bash
cd docker/dev-env
docker compose up -d redis
```

验证：

```bash
docker exec fep-redis redis-cli ping        # -> PONG
docker compose ps                            # fep-redis 应为 healthy
```

停止 / 清理：

```bash
docker compose stop redis                    # 停止（数据保留在卷 fep-redis-data）
docker compose down                          # 删容器+网络（卷保留）
docker compose down -v                       # 连数据卷一起删（慎用）
```

> 数据持久化在命名卷 `fep-redis-data`（appendonly AOF）。

---

## 3. 原生构建 / 测试（日常开发，IDE 友好）

宿主已装 JDK 17，直接用仓库自带 wrapper：

```bash
# 仓库根目录
cd E:\FEP_v1.0

.\mvnw.cmd -pl fep-common -am test           # 单模块（含上游依赖）
.\mvnw.cmd verify                            # 全 reactor 门禁（checkstyle+spotbugs+jacoco+test）
```

> ⚠️ 行尾：本仓库以 **LF** 为准（Checkstyle `NewlineAtEndOfFile` 强制）。
> Windows 检出已通过 `git config core.autocrlf false` + `core.eol lf` 修正；
> 若 clone 新副本，先执行同样两条 config，再 `git rm --cached -r . && git reset --hard`。

---

## 4. 容器化构建（CI 式可复现，无需宿主 JDK）

```bash
cd docker/dev-env

# 跑任意 Maven 命令（容器内 JDK17 + Maven3.9 + 阿里云镜像 + 持久化 .m2）
docker compose run --rm build mvn -version
docker compose run --rm build mvn -pl fep-common -am test
docker compose run --rm build mvn verify
```

- 仓库挂载在容器 `/workspace`；Maven 本地仓库持久化在卷 `fep-maven-repo`（首次慢、之后快）。
- 容器内连 Redis 走服务名 `redis`（已注入 `SPRING_DATA_REDIS_HOST=redis`），
  `build` 启动前会等待 `fep-redis` healthy。
- 镜像构建一致性：JDK 17.0.19 / Maven 3.9.16（与本机原生 JDK 同主版本）。

---

## 5. 文件清单

| 文件 | 作用 |
|------|------|
| `docker-compose.yml` | `redis`(fep-redis) + `build`(maven) 两服务编排 |
| `maven-settings.xml` | build 容器用阿里云镜像（只读挂载到 `/root/.m2/settings.xml`） |
| `README.md` | 本文件 |

> 注：`docker-compose.yml` 把卷定义为 compose 托管。本机因先手工 `docker run`
> 建过同名卷 `fep-redis-data`，`up` 时会有一行 "volume already exists" 告警，
> 属正常（compose 已复用该卷，数据不丢）；全新机器上不会出现。
