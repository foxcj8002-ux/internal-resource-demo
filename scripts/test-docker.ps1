[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:$($env:HOST_PORT ?? '8080')",
    [string]$GatewayIp = $env:TRUSTED_GATEWAY_IPS
)
$ErrorActionPreference = 'Stop'
$trace = "docker-test-$([DateTimeOffset]::Now.ToUnixTimeSeconds())"

function Wait-Health {
    foreach ($attempt in 1..30) {
        try { if ((Invoke-RestMethod "$BaseUrl/actuator/health").status -eq 'UP') { return } } catch {}
        Start-Sleep -Seconds 2
    }
    throw 'Container health check timed out.'
}

Wait-Health
if ([string]::IsNullOrWhiteSpace($GatewayIp)) {
    $GatewayIp = docker network inspect internal-resource-demo_default --format '{{(index .IPAM.Config 0).Gateway}}'
}
if ((Invoke-RestMethod "$BaseUrl/actuator/health").status -ne 'UP') { throw 'Health is not UP.' }
$system = Invoke-RestMethod "$BaseUrl/api/system/info" -Headers @{ 'X-Trace-Id' = "$trace-system" }
if ($system.traceId -ne "$trace-system") { throw 'System TraceId was not passed through.' }
$files = Invoke-RestMethod "$BaseUrl/api/files" -Headers @{ 'X-Trace-Id' = "$trace-files" }
if ($files.data.Count -lt 5) { throw 'H2 initialization did not create five files.' }
$resource = Invoke-RestMethod "$BaseUrl/api/test-resources/read" -Headers @{ 'X-Trace-Id' = "$trace-resource" }
if ($resource.data.resourceId -ne 'test-resource-read') { throw 'Test resource failed.' }
$logs = Invoke-RestMethod "$BaseUrl/api/access-logs" -Headers @{ 'X-Trace-Id' = "$trace-logs" }
if (-not ($logs.data | Where-Object traceId -eq "$trace-resource")) { throw 'AccessLog was not generated.' }

docker compose down
docker compose up -d
Wait-Health
Invoke-RestMethod "$BaseUrl/api/test-resources/read" -Headers @{ 'X-Trace-Id' = "$trace-direct-enabled" } | Out-Null

docker compose down
$env:DIRECT_ACCESS_ENABLED = 'false'
$env:TRUSTED_GATEWAY_IPS = $GatewayIp
docker compose up -d
Wait-Health
$rejected = Invoke-WebRequest "$BaseUrl/api/test-resources/read" -Headers @{ 'X-Trace-Id' = "$trace-direct-disabled" } -SkipHttpErrorCheck
if ($rejected.StatusCode -ne 403 -or $rejected.Content -notmatch 'DIRECT_ACCESS_DISABLED') { throw 'Direct Access Disabled check failed.' }
$gateway = Invoke-RestMethod "$BaseUrl/api/test-resources/read" -Headers @{ 'X-Trace-Id' = "$trace-gateway"; 'X-ZT-Gateway' = 'zero-trust-rgw' }
if (-not $gateway.data.gatewayAccess) { throw 'Trusted gateway request was not accepted.' }
Write-Host 'Docker deployment checks passed.'
