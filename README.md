# internal-resource-demo

受 `zero-trust-rgw` 保护的可控内网资源服务，用于验证 Policy → RGW → Resource 访问链路。

本项目不实现用户认证、JWT 签发或校验、策略决策、RBAC、ABAC 或任何用户权限判断。

## 本地运行

需要 Java 17 和 Maven：

```powershell
mvn test
mvn spring-boot:run
```

默认监听 `8080`。`TRUSTED_UPSTREAM_OBSERVED_ADDRESSES` 只匹配资源服务器实际看到的 `request.getRemoteAddr()`；`X-Forwarded-For` 仅作为审计信息。

真实 Windows Docker NAT 部署中，RGW 192.168.0.111 访问资源服务器 192.168.0.95:8080 后，资源容器可能看到 actualRemoteAddr=172.18.0.1。172.18.0.1 仅表示 Docker Gateway 的传输观察地址，不是 RGW IP；RGW 原始地址 192.168.0.111 应由 Windows 防火墙、VPN 和路由边界控制，不能通过 X-Forwarded-For 建立信任。

Gateway Access 第一版使用 GATEWAY_TRUST_MODE=DOCKER_HOST_NAT，并要求 actualRemoteAddr 位于 TRUSTED_UPSTREAM_OBSERVED_ADDRESSES 且 X-ZT-Gateway 正确。

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
TRUSTED_UPSTREAM_OBSERVED_ADDRESSES
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

## 网络环境边界

真实部署时必须区分三种网络环境：

### 本机 Docker 网络

```text
测试客户端
    ↓ localhost:8080
Docker Desktop 主机
    ↓ Docker bridge
internal-resource-demo 容器
```

本机 Docker 测试中，资源服务看到的 `actualRemoteAddr` 可能是 Docker 网桥地址，例如 `172.18.0.1`。该地址只代表本机 Docker 网络，**禁止直接作为生产 `TRUSTED_UPSTREAM_OBSERVED_ADDRESSES`**。

本机 Docker 的 `172.18.0.1`、Compose 网络名和容器地址不代表真实 RGW 服务器地址，也不能证明真实内网链路已经打通。

### 真实内网部署

```text
内网客户端/用户
    ↓
zero-trust-rgw 所在服务器
    ↓ VPN / 内网路由
内网资源服务器
    ↓
internal-resource-demo:8080
```

真实部署必须使用实际网络地址，例如：

```text
资源服务器：192.168.x.x:8080
RGW 服务器：实际部署地址
VPN/内网接口：实际部署接口
```

`192.168.x.x` 只是文档占位符，不得原样写入生产配置。

### RGW、VPN 与内网资源服务器

真实链路为：

```text
zero-trust-rgw
    ↓ HTTP/TCP
VPN 或内网路由
    ↓
内网资源服务器 192.168.x.x:8080
```

资源服务器不连接真实设备、不扫描网络、不执行策略决策；它只提供业务资源并记录实际收到的请求。

## 真实内网部署验证流程

以下流程应在真实 RGW 服务器、VPN/内网路由和资源服务器都准备完成后执行。命令中的 `192.168.x.x` 必须替换为真实资源服务器地址。

### 1. 资源服务器本机检查

确认服务正在监听 `8080`，并且监听地址允许 RGW 所在网络访问。

Windows：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess
```

Linux：

```bash
ss -ltnp | grep ':8080'
sudo lsof -nP -iTCP:8080 -sTCP:LISTEN
```

容器部署时，确认端口映射和容器状态：

```bash
docker compose ps
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
docker port internal-resource-demo-internal-resource-demo-1
```

期望看到类似：

```text
0.0.0.0:8080->8080/tcp
```

如果只监听 `127.0.0.1:8080`，RGW 服务器通常无法访问；应根据部署方式让应用或容器端口绑定到可达的内网接口，同时通过防火墙限制来源。

### 2. 资源服务器健康检查

在资源服务器本机执行：

```bash
curl -i http://127.0.0.1:8080/actuator/health
curl -i http://127.0.0.1:8080/api/system/info
```

Windows PowerShell：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8080/api/system/info
```

必须返回：

```json
{"status":"UP"}
```

### 3. RGW 服务器到资源服务器的网络验证

先验证路由和基础连通性。`ping` 可能被防火墙禁止，因此 ping 失败不能单独证明 TCP 不通。

Windows RGW 服务器：

```powershell
ping 192.168.x.x
Test-NetConnection 192.168.x.x -Port 8080
```

Linux RGW 服务器：

```bash
ping -c 4 192.168.x.x
nc -vz 192.168.x.x 8080
curl -i --connect-timeout 10 http://192.168.x.x:8080/actuator/health
```

Windows 也应执行：

```powershell
Invoke-WebRequest http://192.168.x.x:8080/actuator/health
Invoke-WebRequest http://192.168.x.x:8080/api/system/info
```

只有 RGW 服务器能够实际访问资源服务器的 `8080`，才进入 RGW 转发验收。若 TCP 8080 不通，应检查 VPN、路由表、监听地址和防火墙。

### 4. 资源服务器确认实际来源 IP

通过 RGW 或模拟 RGW 请求访问：

```bash
curl -i \
  -H 'X-Trace-Id: network-observe-001' \
  -H 'X-ZT-Gateway: zero-trust-rgw' \
  http://192.168.x.x:8080/api/system/network-info
```

Windows PowerShell：

```powershell
Invoke-RestMethod http://192.168.x.x:8080/api/system/network-info `
  -Headers @{ 'X-Trace-Id' = 'network-observe-001'; 'X-ZT-Gateway' = 'zero-trust-rgw' }
```

查看响应中的：

```text
actualRemoteAddr
forwardedFor
forwardedProto
traceId
gatewayAccess
```

`actualRemoteAddr` 是资源系统通过 `request.getRemoteAddr()` 实际看到的 TCP 对端地址。将这个真实 `actualRemoteAddr` 作为候选值配置到资源服务器的 `TRUSTED_UPSTREAM_OBSERVED_ADDRESSES`，而不是使用：

- Docker 本机测试得到的 `172.18.0.1`
- 客户端 IP
- `X-Forwarded-For` 中的地址
- 文档占位符 `192.168.x.x`
- 未经验证的公网地址

如果存在 VPN、NAT 或反向代理，必须以资源服务器实际看到的 `actualRemoteAddr` 为准，并确认该地址在网络拓扑中确实属于 RGW 或可信中间层。

### 5. 配置真实可信来源

开发/演示环境可以使用：

```text
DIRECT_ACCESS_ENABLED=true
GATEWAY_TRUST_MODE=DOCKER_HOST_NAT
GATEWAY_HEADER_NAME=X-ZT-Gateway
GATEWAY_HEADER_VALUE=zero-trust-rgw
TRUSTED_UPSTREAM_OBSERVED_ADDRESSES=172.18.0.1
```

真实内网演示或生产环境建议：

```text
DIRECT_ACCESS_ENABLED=false
TRUSTED_UPSTREAM_OBSERVED_ADDRESSES=<network-info 返回的真实 actualRemoteAddr>
```

多个可信来源使用项目配置支持的列表格式；不要把 `X-Forwarded-For` 当作可信来源判断依据。修改后重启容器或应用，并重新执行 `/api/system/network-info` 验证。

### 6. Direct Access 验证

当 `DIRECT_ACCESS_ENABLED=false` 时，从普通客户端直接访问资源 API：

```bash
curl -i -H 'X-Trace-Id: direct-deny-001' \
  http://192.168.x.x:8080/api/test-resources/read
```

期望：

```text
HTTP/1.1 403
error: DIRECT_ACCESS_DISABLED
traceId: direct-deny-001
```

再通过真实 RGW 转发同一资源请求，期望：

```text
HTTP/1.1 200
resourceId: test-resource-read
action: GET
gatewayAccess: true
```

拒绝请求不得执行实际业务 Controller，但应在 AccessLog 中留下 `DIRECT_ACCESS_DISABLED` 记录。

## Windows 防火墙与监听检查

### Windows 防火墙

查看当前启用规则：

```powershell
Get-NetFirewallProfile | Format-Table Name,Enabled,DefaultInboundAction,DefaultOutboundAction
Get-NetFirewallRule -Enabled True | Select-Object DisplayName,Direction,Action,Profile
```

查看是否存在 8080 入站规则：

```powershell
Get-NetFirewallPortFilter | Where-Object LocalPort -eq 8080
```

添加规则前应遵循最小范围原则，仅允许实际 RGW/VPN 网段；不要无条件对所有公网地址开放：

```powershell
New-NetFirewallRule -DisplayName 'internal-resource-demo from RGW' `
  -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 `
  -RemoteAddress <实际RGW或VPN网段>
```

删除或调整规则前先记录原规则，避免影响其他服务。

### Windows 端口连通性

资源服务器：

```powershell
Test-NetConnection 127.0.0.1 -Port 8080
Test-NetConnection <资源服务器真实内网IP> -Port 8080
```

RGW 服务器：

```powershell
Test-NetConnection <资源服务器真实内网IP> -Port 8080
Invoke-WebRequest http://<资源服务器真实内网IP>:8080/actuator/health
```

## Linux 防火墙与监听检查

### Linux 监听地址

```bash
ip addr
ip route
ss -ltnp | grep ':8080'
```

容器部署：

```bash
docker compose ps
docker port internal-resource-demo-internal-resource-demo-1
```

### Linux 防火墙

UFW：

```bash
sudo ufw status verbose
sudo ufw allow from <实际RGW或VPN网段> to any port 8080 proto tcp
```

firewalld：

```bash
sudo firewall-cmd --state
sudo firewall-cmd --list-all
sudo firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="<实际RGW或VPN网段>" port protocol="tcp" port="8080" accept'
sudo firewall-cmd --reload
```

iptables/nftables 环境应使用现有主机策略添加等价的最小范围规则，不要直接清空现有规则。

### Linux 端口连通性

资源服务器：

```bash
curl -i http://127.0.0.1:8080/actuator/health
curl -i http://<资源服务器真实内网IP>:8080/actuator/health
```

RGW 服务器：

```bash
nc -vz <资源服务器真实内网IP> 8080
curl -i --connect-timeout 10 http://<资源服务器真实内网IP>:8080/actuator/health
```

## RGW 对接验收清单

以下清单应由 RGW 和资源服务器联调人员共同确认：

- [ ] RGW 服务器能够通过 VPN/内网路由访问资源服务器 `8080`。
- [ ] RGW 配置的 `resourceId` 与资源服务约定一致：`internal-files`、`internal-devices`、`internal-access-logs`、`test-resource-read`、`test-resource-create`、`test-resource-update`、`test-resource-delete`。
- [ ] RGW `target` 地址正确，指向真实资源服务器，例如 `http://192.168.x.x:8080`，不使用 Docker 容器地址。
- [ ] RGW 的 GET/POST/PUT/DELETE 分别映射到实际 HTTP Method，不把语义动作直接当作资源服务的 Action。
- [ ] RGW 转发时保留 `X-Trace-Id`，资源服务响应和 AccessLog 能关联同一 TraceId。
- [ ] RGW 转发时按约定透传 `X-Session-Id`，资源服务只记录 `sessionIdPresent`，不返回完整 SessionId。
- [ ] RGW 转发 `X-ZT-Gateway: zero-trust-rgw`。
- [ ] 资源服务根据实际 `actualRemoteAddr` 与 `X-ZT-Gateway` 双条件正确识别 `gatewayAccess`。
- [ ] `X-Forwarded-For` 仅作为审计信息，不参与可信来源判断。
- [ ] Authorization 只记录 `authorizationPresent`，不记录完整 JWT。
- [ ] RGW Allow 请求到达资源服务并产生 AccessLog。
- [ ] RGW Deny 请求不转发到资源服务，不产生对应的业务访问日志。
- [ ] `DIRECT_ACCESS_ENABLED=false` 时，非可信 RGW 请求返回 `403 DIRECT_ACCESS_DISABLED`。
- [ ] Direct Access 拒绝响应包含原始或生成的 `X-Trace-Id`。
- [ ] 策略允许 GET、拒绝 DELETE 时，GET 到达资源服务，DELETE 不到达资源服务。
- [ ] Session 过期或撤销由 RGW 拒绝，资源服务不实现 Session 有效性判断。
- [ ] 策略撤销后再次访问被 RGW 拒绝，资源服务没有新的业务访问请求。

## 公网暴露检查

资源服务应只对必要的内网/VPN/RGW 来源开放，不应直接暴露公网。

Windows：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-NetFirewallProfile
Get-NetFirewallRule -Enabled True | Select-Object DisplayName,Direction,Action,Profile
```

Linux：

```bash
ss -ltnp | grep ':8080'
sudo ufw status verbose
sudo firewall-cmd --list-all
```

从不可信网络执行 TCP 8080 检查；如果公网可以访问，应立即收紧防火墙、路由或安全组。不要通过资源服务执行网络扫描。

## 零信任职责边界

```text
Policy       负责决策
zero-trust-rgw 负责执行 JWT/Session/Resource/Action 校验并转发
本项目       只提供资源、记录请求事实并返回业务结果
```

本项目不修改 Policy、RGW 或其他 `zerotrustbackend` 模块。

