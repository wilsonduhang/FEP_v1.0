# FEP TLQ Runtime Docker 镜像

> P1c Task 1 产物 · TongLINK/Q 8.1.15.2-P3 Linux 运行时基础镜像
> 维护者：FEP Team
> 关联 Plan：`docs/plans/2026-04-26-p1c-tlq-real-sdk.md` §T1
> 决策依据：`docs/plans/2026-04-26-p1c-sdk-validation-and-decisions.md` §3.3

## 1. 目的

为 FEP 应用容器化部署提供包含 TLQ 8.1.15.2-P3 原生 6 个 `.so` 的 Linux 基础镜像，避免每个下游服务镜像重复打包原生库。

镜像内容：
- 基础：`eclipse-temurin:17-jre-jammy`（Ubuntu 22.04，glibc 2.35，Java 17 JRE）
- TLQ 原生库：`/opt/tlq/lib/*.so`（6 个）
- 环境变量：`LD_LIBRARY_PATH` + `JAVA_TOOL_OPTIONS=-Djava.library.path` 双重保险

**镜像不包含**：
- TLQ SDK 的 JAR（`tlclient.jar` / `tlq63j.jar` / `TLQRemoteApi.jar`）—— 由 Spring Boot fat jar 通过 mvn local repo 提供
- FEP 应用本身 —— 运行时通过 `-v` 挂载或下游 `FROM` 派生

## 2. 构建前置条件

| 项目 | 要求 |
|------|------|
| Docker | ≥ 20.10（支持 BuildKit） |
| 6 个 `.so` 文件 | 已从东方通 TLQ SDK 8.1.15.2-P3 安装包解压 |
| 解压目录示例 | `/path/to/TLQ_SDK_LIB/` 含 `libgmssljni.so` 等 6 文件 |

> 6 个 `.so` 不入仓库（避免二进制版本管理 + 平台兼容问题），由团队成员从 SDK 解压目录手动 stage 到 build context。

## 3. 构建命令

### 方式 A：将 SDK lib 复制为 `./tlq-sdk-lib/` 子目录（推荐）

```bash
cd docker/tlq-runtime/
cp -r "/path/to/TLQ_SDK_LIB" ./tlq-sdk-lib
ls tlq-sdk-lib/        # 应有 6 个 .so

docker build -t fep-tlq-runtime:8.1.15.2-P3 .
```

构建完成后清理临时 stage 目录：
```bash
rm -rf docker/tlq-runtime/tlq-sdk-lib
```

> `tlq-sdk-lib/` 已加入根 `.gitignore`，即使忘记清理也不会误入仓库。

### 方式 B：使用 `--build-arg` 指定 lib 路径（路径相对 build context）

```bash
cd docker/tlq-runtime/
# 假设你已有 ./libs/ 含 6 个 .so
docker build --build-arg TLQ_SDK_LIB_DIR=libs -t fep-tlq-runtime:8.1.15.2-P3 .
```

## 4. 运行示例

### 4.1 烟囱测试（验证镜像可启动）

```bash
docker run --rm fep-tlq-runtime:8.1.15.2-P3
# 期望输出：
#   openjdk version "17.x.x" ...
#   OpenJDK Runtime Environment Temurin-17.x.x+x ...
```

### 4.2 验证 6 个 `.so` 已加载到指定路径

```bash
docker run --rm fep-tlq-runtime:8.1.15.2-P3 ls -la /opt/tlq/lib/
# 期望：6 个 .so 文件
```

### 4.3 验证动态链接器能解析 TLQ 主库依赖

```bash
docker run --rm fep-tlq-runtime:8.1.15.2-P3 ldd /opt/tlq/lib/libtl_tcapi.so
# 期望：所有依赖均能解析（无 "not found"）
# 若出现 "version 'GLIBC_X.Y' not found"，参考 §6 兜底方案
```

### 4.4 挂载 FEP 应用 fat jar 运行

```bash
docker run --rm \
  -v /path/to/fep-web-1.0.0.jar:/app.jar \
  -p 8080:8080 \
  fep-tlq-runtime:8.1.15.2-P3 \
  java -jar /app.jar
```

`JAVA_TOOL_OPTIONS=-Djava.library.path=/opt/tlq/lib` 已默认注入，无需在启动命令额外指定。

## 5. `.so` 来源说明

| 文件 | 大小（实测） | 来源 |
|------|--------------|------|
| `libgmssljni.so` | ~2.5 MB | 国密 SM 算法 JNI 桥接（gmssl） |
| `libjtlq_client.so` | ~141 KB | TLQ Java 客户端 JNI 入口 |
| `libtl_public.so` | ~166 KB | TLQ 公共依赖 |
| `libtl_tcapi.so` | ~6.2 MB | TLQ TCAPI 核心 |
| `libtlqcrycomp.so` | ~61 KB | TLQ 加密压缩组件 |
| `libtlqtcapi6.so` | ~18 KB | TLQ TCAPI v6 兼容层 |

由东方通 TongLINK/Q 8.1.15.2-P3 Linux x86-64 SDK 安装包提供。详见 P1c SDK 决策文档。

## 6. glibc 兼容兜底

当前镜像选 `eclipse-temurin:17-jre-jammy`（Ubuntu 22.04，glibc 2.35）。

若运行 `ldd /opt/tlq/lib/libtl_tcapi.so` 出现：

```
./libtl_tcapi.so: /lib/x86_64-linux-gnu/libc.so.6: version `GLIBC_X.Y' not found
```

或 JVM 启动报 `UnsatisfiedLinkError` 提示 GLIBC 版本不匹配，按以下顺序尝试：

### 兜底方案 1：切换至 `centos:7`（glibc 2.17，与 .so 构建年代匹配）

> centos:7 已于 2024-06-30 EOL，仅作为兼容性兜底使用。

```dockerfile
# 替换 Dockerfile 第 32 行 FROM 指令为：
FROM centos:7

# 自行安装 OpenJDK 17（Adoptium tarball 解压方式）：
RUN yum install -y wget tar gzip && \
    wget -qO- https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jre_x64_linux_hotspot_17.0.10_7.tar.gz \
      | tar -xz -C /opt && \
    ln -s /opt/jdk-17.0.10+7-jre /opt/java
ENV JAVA_HOME=/opt/java
ENV PATH=$JAVA_HOME/bin:$PATH
```

### 兜底方案 2：切换至 `debian:bullseye-slim`（glibc 2.31，介于 jammy 和 centos:7 之间）

```dockerfile
FROM eclipse-temurin:17-jre-bullseye-slim
```

无需修改其他指令，glibc 2.31 与多数 .so 兼容性较好。

### 兜底方案 3：联系东方通获取适配当前 glibc 版本的新 .so

如以上方案均失败（罕见），通过商务渠道联系东方通技术支持索取适配版本。

## 7. 维护者注意事项

- **升级 SDK 版本时**：同步更新 `LABEL org.opencontainers.image.version`、本 README §1/§5 版本号、镜像 tag 命名规则。
- **新增/移除 `.so` 文件时**：同步更新 Dockerfile `COPY` 列表（避免 glob 是有意为之 —— 缺失文件需在 `docker build` 时立即报错，而非运行时 `UnsatisfiedLinkError`）。
- **变更基础镜像时**：必须运行 §4.3 `ldd` 验证 + 集成测试 P1c T2 `RealTlqIntegrationTest` 通过。
- **`.so` 二进制不入仓库**：根 `.gitignore` 已配置 `docker/tlq-runtime/tlq-sdk-lib/` 模式；提 commit 前请 `git status` 确认无 `.so` 进入暂存区。

## 8. 已知约束

- 仅支持 Linux x86-64（与 SDK 提供的 .so 平台一致）；ARM64 / macOS / Windows 容器需另外打包对应平台 .so（当前 SDK 不提供）。
- Java 版本固定为 17（与 FEP_v1.0 全模块 `<java.version>17</java.version>` 对齐）。
- `JAVA_TOOL_OPTIONS` 默认追加 `-Djava.library.path=/opt/tlq/lib`；若下游服务需要追加额外 JVM 参数，建议使用 `JAVA_OPTS` 或在启动命令显式拼接，避免覆盖 `JAVA_TOOL_OPTIONS`。

## 9. 相关文档

- Plan：`docs/plans/2026-04-26-p1c-tlq-real-sdk.md`（§T1 镜像构建任务）
- SDK 决策：`docs/plans/2026-04-26-p1c-sdk-validation-and-decisions.md`（§3.3 镜像选型）
- 安装脚本：`scripts/install-tlq-sdk.sh`（mvn local repo 安装 TLQ SDK 3 个 JAR）
- 架构定位：`CLAUDE.md` §3.1 TLQ 通信运行环境
