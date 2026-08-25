# internal-resource-demo

受 zero-trust-rgw 保护的可控内网资源服务，用于验证 Policy → RGW → Resource 访问链路。

当前为第二阶段项目骨架，包含 Java 17、Spring Boot 3、Maven、H2、JPA、Actuator、实体、Repository、TraceId 过滤器、Gateway Access 判断组件和基础 Controller。

本项目不实现认证、JWT 签发或校验、策略决策、RBAC、ABAC 或用户权限判断。

需要 Java 17 和 Maven：

    mvn test
    mvn spring-boot:run

TRUSTED_GATEWAY_IPS 只匹配服务器实际看到的 request.getRemoteAddr()；X-Forwarded-For 仅作为审计或展示信息。
