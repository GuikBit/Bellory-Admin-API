# Smoke Test D — Roteiro de reprodução

**Objetivo:** identificar qual endpoint da família "Payment API proxy / webhook" está retornando erro de JSON na FASE 4.

> Use este doc para reproduzir o erro D pendente do STATUS_EXECUCAO.md.

---

## Pré-requisitos

1. **Bellory-Admin-API** rodando em `:8085`:
   ```powershell
   cd "C:\Users\Pluri Sistemas\Desktop\Guikbit\GitHubRepository\Bellory-Admin-API"
   .\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```
2. Banco `bellory_dev` em `localhost:5432` populado (mesmo que já está em uso pelo app).
3. Redis local em `:6379` (opcional — admin tem fail-open).
4. Console em outra janela com PowerShell pra executar os curls.

---

## Step 0 — Login admin (capturar token)

```powershell
$resp = Invoke-RestMethod -Method POST -Uri "http://localhost:8085/api/v1/admin/auth/login" `
  -Body (@{ username = "<admin_user>"; password = "<admin_pass>" } | ConvertTo-Json) `
  -ContentType "application/json"
$token = $resp.token
Write-Output "Token capturado: $($token.Substring(0,30))..."
$headers = @{ Authorization = "Bearer $token" }
```

> Se o login falhar (4xx), o problema **não é** o de smoke D. Resolver auth primeiro.

---

## Step 1 — Proxies Payment API (READ-ONLY, seguros)

> **Use apenas GET. Para cada chamada, anote o status HTTP e qualquer log de erro do admin.**

### Assinaturas

```powershell
# Listar
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/assinaturas?page=0&size=5" | ConvertTo-Json -Depth 5

# Listar customers
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/assinaturas/customers?page=0&size=5" | ConvertTo-Json -Depth 5

# Buscar customer por ID (pegar 1 ID da listagem acima)
$customerId = 1   # ajustar
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/assinaturas/customers/$customerId" | ConvertTo-Json -Depth 5

# Access-status do customer
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/assinaturas/customers/$customerId/access-status" | ConvertTo-Json -Depth 5

# Buscar assinatura por ID (pegar 1 da listagem)
$subId = 1   # ajustar
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/assinaturas/$subId" | ConvertTo-Json -Depth 5

# Cobrancas da assinatura
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/assinaturas/$subId/cobrancas" | ConvertTo-Json -Depth 5

# Trocas de plano
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/assinaturas/$subId/trocas-plano" | ConvertTo-Json -Depth 5
```

### Planos

```powershell
# Listar
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/planos" | ConvertTo-Json -Depth 5

# Por ID (pegar 1)
$planId = 1   # ajustar
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/planos/$planId" | ConvertTo-Json -Depth 5

# Por código
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/planos/codigo/STARTER" | ConvertTo-Json -Depth 5

# Pricing / limites / features
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/planos/$planId/pricing" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/planos/$planId/limites" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/planos/$planId/features" | ConvertTo-Json -Depth 5

# Verificar limite específico
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/planos/$planId/limites/MAX_USUARIOS?usage=3" | ConvertTo-Json -Depth 5
```

### Cupons

```powershell
# Listar todos
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/cupons?page=0&size=5" | ConvertTo-Json -Depth 5

# Listar ativos
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/cupons/ativos?page=0&size=5" | ConvertTo-Json -Depth 5

# Por ID e por código (pegar 1 da listagem)
$cupomId = 1   # ajustar
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/cupons/$cupomId" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/cupons/code/PROMO10" | ConvertTo-Json -Depth 5

# Histórico de uso
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/cupons/$cupomId/usos" | ConvertTo-Json -Depth 5
```

### Webhook (admin endpoints — leitura de config/log)

```powershell
# Config token
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/webhook/config" | ConvertTo-Json -Depth 5

# Lista de event configs
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/webhook/eventos/config" | ConvertTo-Json -Depth 5

# Lista de event logs (paginado) — possível ponto de falha de Jackson serialization de Page<WebhookEventLog>
Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/webhook/eventos/log?page=0&size=5" | ConvertTo-Json -Depth 5
```

---

## Step 2 — Webhook receptor (simulação)

> Endpoint público que recebe POST da Payment API. Token é validado pelo `WebhookConfig` ativo no banco. Pegue o token via:
> ```powershell
> Invoke-RestMethod -Headers $headers -Uri "http://localhost:8085/api/v1/admin/webhook/config"
> ```

```powershell
$webhookToken = "<COLAR_AQUI>"
$payload = @{
  id          = "evt_test_001"
  type        = "ChargePaidEvent"
  occurredAt  = "2026-05-08T10:00:00"
  companyId   = 4
  resource    = @{ type = "charge"; id = "1" }
  data        = @{ value = 99.90; effectivePrice = 99.90 }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method POST -Uri "http://localhost:8085/api/v1/webhook/payment" `
  -Headers @{
    Authorization   = "Bearer $webhookToken"
    "X-Event-Id"    = "evt_test_001"
    "X-Event-Type"  = "ChargePaidEvent"
    "X-Delivery-Id" = "del_test_001"
  } `
  -ContentType "application/json" `
  -Body $payload
```

Verificar se o `webhook_event_log` registrou o evento:
```sql
SELECT id, event_id, event_type, status, error_message, dt_recebido, dt_processado
FROM admin.webhook_event_log
ORDER BY dt_recebido DESC
LIMIT 5;
```

---

## O que reportar de volta

Para cada endpoint que falhar:

1. **Endpoint exato** (URL + método).
2. **Status HTTP** retornado.
3. **Body de erro** (o admin retorna `{success: false, message, details}` quando o erro vem da Payment API).
4. **Stacktrace do log** do admin (procure por `ERROR` ou `WARN` no console do `mvnw spring-boot:run`).
5. Se for o webhook: o `error_message` registrado em `webhook_event_log`.

Com isso eu identifico se é:
- DTO desincronizado com schema da Payment API atual (precisaria adicionar campo + `@JsonProperty`)
- Erro de serialização de entity Jackson (proxy lazy / circular)
- Bug pré-existente no código que veio do Bellory-API
- Indisponibilidade da Payment API externa (`502` no `buildErrorResponse`)
