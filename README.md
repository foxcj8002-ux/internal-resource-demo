# internal-resource-demo

受 `zero-trust-rgw` 保护的可控内网资源服务，用于验证 Policy → RGW → Resource 访问链路。

本项目不实现用户认证、JWT 签发或校验、策略决策、RBAC、ABAC 或任何用户权限判断。

## 本地运行

需要 Java 17 和 Maven：

```powershell
mvn test
mvn spring-boot:run
```

默认监听 `8080`。`TRUSTED_GATEWAY_IPS` 只匹配资源服务器实际看到的 `request.getRemoteAddr()`；`X-Forwarded-For` 仅作为审计信息。

## Docker 运行

```powershell
docker compose build
docker compose up -d
curl http://localhost:8080/actuator/health
docker compose down
```

也可以使用环境变量调整配置：

```text
SERVER_PORT
HOST_PORT
SPRING_PROFILES_ACTIVE
DIRECT_ACCESS_ENABLED
TRUSTED_GATEWAY_IPS
AUDIT_ENABLED
APPLICATION_VERSION
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

默认使用 H2 文件数据库，数据保存在 Compose volume `internal-resource-data` 中。未配置真实密码、JWT Secret 或生产密钥。

容器应用使用非 root 用户 `appuser` 运行。

Docker API 验证脚本：

```bash
bash scripts/test-docker.sh
```

```powershell
.\scripts\test-docker.ps1
```

## API

```text
GET/POST       /api/files
GET/PUT/DELETE /api/files/{id}
GET/POST       /api/devices
GET/PUT/DELETE /api/devices/{id}
GET            /api/test-resources/read
POST           /api/test-resources/create
PUT            /api/test-resources/update/{id}
DELETE         /api/test-resources/delete/{id}
GET            /api/access-logs
GET            /api/access-logs/{id}
POST           /api/access-logs/test
GET            /api/system/info
GET            /api/system/network-info
GET            /actuator/health
```

## 内网电脑部署

### Windows

1. 安装 Java 17、Docker Desktop 或直接安装 Maven 运行方式。
2. 将项目复制到内网电脑，不把 `.env`、数据库文件或生产密钥提交到 Git。
3. 确认服务监听地址。容器默认监听所有接口的 `8080`；本地运行时需要确保 Spring Boot 绑定到可达网卡。
4. 执行 `docker compose up -d`，或执行 `mvn spring-boot:run`。
5. Windows Defender Firewall 只放行内网/VPN 网段到 `8080`，不要对公网开放。

示例地址只使用占位形式：

```text
内网资源服务器：192.168.x.x:8080
RGW 服务器：由实际部署环境提供
```

### Linux

1. 安装 Java 17、Docker Engine 和 Docker Compose Plugin。
2. 将项目部署到内网主机目录。
3. 执行 `docker compose build && docker compose up -d`。
4. 使用 `ufw`、`firewalld` 或云主机安全组仅允许 RGW/VPN 网段访问 `8080`。
5. 不将 `8080` 绑定到公网入口或公网反向代理。

### RGW 网络连通性

RGW 服务器必须能够通过 VPN 或内网路由访问资源服务器：

```text
RGW 服务器
    ↓ VPN / 内网路由
192.168.x.x:8080
```

验证方式：

```bash
curl -i http://192.168.x.x:8080/actuator/health
curl -i http://192.168.x.x:8080/api/system/info
```

实际部署时将 `192.168.x.x` 替换为资源服务器的真实内网地址，不要写入代码或模板。

### trusted-gateway-ips

`TRUSTED_GATEWAY_IPS` 必须填写资源服务器在网络层实际看到的 RGW 来源 IP，也就是 `request.getRemoteAddr()` 的值。

不要填写：

- 客户端 IP
- `X-Forwarded-For` 中的地址
- 未验证的公网地址
- 示例地址 `192.168.x.x`

如果中间存在反向代理，应先确认资源服务实际看到的 TCP 对端地址，并据此配置可信来源。

### 防火墙与公网暴露检查

Windows：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-NetFirewallRule -Enabled True | Select-Object DisplayName,Direction,Action
```

Linux：

```bash
ss -ltnp | grep 8080
sudo ufw status
sudo firewall-cmd --list-all
```

从公网或不可信网络确认 `8080` 没有入站路径；只允许 VPN/内网网段和必要的 RGW 来源访问。资源服务自身不执行网络扫描。

### Direct Access

演示/生产环境建议：

```text
DIRECT_ACCESS_ENABLED=false
TRUSTED_GATEWAY_IPS=<资源服务器实际看到的 RGW IP>
```

非可信 RGW 请求返回 `403 DIRECT_ACCESS_DISABLED`；可信 RGW 请求继续进入业务 Controller。该入口保护不等同于用户权限决策。

## 零信任职责边界

```text
Policy       负责决策
zero-trust-rgw 负责执行 JWT/Session/Resource/Action 校验并转发
本项目       只提供资源、记录请求事实并返回业务结果
```
