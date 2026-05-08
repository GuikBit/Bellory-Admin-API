# Bellory — Especificação do Backend do Formulário de Contato

## Visão geral

O formulário público de contato do site (`components/contact.tsx`, seção `#contato`) recebe leads do segmento beauty/wellness pedindo informações, orçamento personalizado, ou contato pelos cards de "Rede de salões" e "Mais de 10 usuários no time" (em `components/pricing.tsx`).

Este documento descreve **o que o backend precisa fornecer** para receber e processar esses envios de forma segura. O frontend já faz validação, honeypot, time-trap, cooldown local e Cloudflare Turnstile — mas **isso é só UX e barreira contra bots simples**. A camada autoritativa de segurança é o backend.

---

## 1. Endpoint

### `POST /api/contato`

Recebe um lead do formulário público.

**Headers esperados:**

```
Content-Type: application/json
Origin: https://bellory.com.br        (validar)
```

**Body — `ContactPayload`** (schema canônico em `lib/contact-schema.ts`, importável pelo backend se for Node/TS no mesmo monorepo, ou replicado em outra stack):

```json
{
  "name": "Maria Souza",
  "email": "maria@salaobelo.com.br",
  "phone": "(11) 99999-1234",
  "businessType": "Salão",
  "message": "Quero saber se vocês integram com meu sistema atual...",
  "website": "",
  "turnstileToken": "0.abc123...",
  "fillTimeMs": 24500,
  "source": "landing/contato"
}
```

**Limites (em `CONTACT_LIMITS` do schema):**

| Campo            | Mín    | Máx    | Notas                                      |
| ---------------- | ------ | ------ | ------------------------------------------ |
| `name`           | 2      | 100    | trim                                       |
| `email`          | —      | 160    | trim, lowercase, formato válido            |
| `phone`          | 10     | 20     | regex `/^[\d\s()+-]+$/`                    |
| `businessType`   | enum   | enum   | Salão / Barbearia / Clínica / Nail / Spa / Outro |
| `message`        | 10     | 2000   | trim                                       |
| `website`        | 0      | 0      | **honeypot** — qualquer valor = bot        |
| `turnstileToken` | 1      | —      | string opaca da Cloudflare                 |
| `fillTimeMs`     | ≥ 1500 | —      | tempo gasto preenchendo (anti-bot)         |
| `source`         | —      | 120    | de onde veio                               |

---

## 2. Camadas de segurança obrigatórias

Em ordem de execução. **Sempre nessa ordem** — a validação barata (honeypot) vem antes da cara (Turnstile remoto).

### 2.1. Origin / Referer check

Rejeitar com `403` qualquer request cujo `Origin` (ou `Referer`) não bata com:

- `https://bellory.com.br`
- `https://www.bellory.com.br`
- `http://localhost:3000` (apenas em `NODE_ENV !== 'production'`)

Não substitui CSRF token mas filtra script-kiddie.

### 2.2. Rate limiting por IP

Janela deslizante. Respondem `429 Too Many Requests` com body `{ "code": "rate_limited" }`.

- **3 envios / minuto** por IP
- **10 envios / hora** por IP
- **30 envios / dia** por IP

Stack recomendada (já nivel free tier):

- **Upstash Redis** + `@upstash/ratelimit`
- ou Redis self-hosted com `rate-limiter-flexible`

IP deve vir de `x-forwarded-for` (primeiro elemento) quando atrás de proxy/CDN. Nunca confiar no `req.socket.remoteAddress` em ambiente Vercel/Cloudflare.

### 2.3. Honeypot

Se `body.website.trim().length > 0` → responder **`200 OK` com sucesso fake**. Não revelar ao bot que foi descartado. Logar como `dropped:honeypot`.

### 2.4. Time-trap

Se `body.fillTimeMs < 1500` → mesmo tratamento (200 OK fake, log `dropped:too_fast`).

### 2.5. Validação Zod

Re-parsear o body com o schema completo (`contactPayloadSchema.safeParse`). Em falha responder `400` com `{ "code": "validation", "details": [...] }`. **Não** enviar a mensagem do Zod cru — pode vazar shape interno.

### 2.6. Verificação do Cloudflare Turnstile

Chamar a API da Cloudflare:

```http
POST https://challenges.cloudflare.com/turnstile/v0/siteverify
Content-Type: application/x-www-form-urlencoded

secret={TURNSTILE_SECRET_KEY}&response={turnstileToken}&remoteip={ip}
```

Resposta esperada:

```json
{ "success": true, "challenge_ts": "...", "hostname": "bellory.com.br" }
```

Se `success === false` ou `hostname` não bater → `400` com `{ "code": "turnstile_failed" }`.

A `TURNSTILE_SECRET_KEY` **fica apenas no backend** (env var, nunca exposta).

### 2.7. Sanitização

Antes de armazenar ou repassar:

- `name`, `phone`, `email`: trim, normaliza unicode (NFC).
- `message`: stripar tags HTML (DOMPurify server-side ou regex `/<[^>]*>/g`). Quebras de linha podem ficar.
- Tudo escapado ao renderizar em painel admin / e-mail HTML (proteção XSS no consumidor).

### 2.8. Anti-email-injection (se enviar e-mail)

Se o e-mail de notificação for montado com `from`, `subject`, etc. — **nunca** concatenar input do usuário em headers. Use libs (Resend, SendGrid, Nodemailer) que tratam isso. Para SMTP cru, rejeitar valores que contenham `\r` ou `\n`.

---

## 3. Respostas

Sempre JSON. Códigos de erro fixos para o frontend mapear mensagens user-friendly (`ERROR_MESSAGES` em `components/contact.tsx`).

### Sucesso

```http
HTTP/1.1 200 OK
Content-Type: application/json

{ "ok": true, "id": "lead_abc123" }
```

### Erros

| HTTP | `code`              | Quando                                 |
| ---- | ------------------- | -------------------------------------- |
| 400  | `validation`        | Falha no schema Zod                    |
| 400  | `turnstile_failed`  | Token Turnstile inválido / expirado    |
| 403  | `forbidden_origin`  | Origin/Referer não autorizado          |
| 429  | `rate_limited`      | Estourou rate-limit                    |
| 500  | `server`            | Falha interna (sempre logar contexto)  |

Body de erro:

```json
{ "ok": false, "code": "rate_limited", "message": "..." }
```

O frontend ignora `message` (mostra texto local de `ERROR_MESSAGES`); o `code` é o contrato.

---

## 4. Persistência e roteamento do lead

Duas opções (escolher uma — ou as duas):

### Opção A — Apenas e-mail (simples, sem banco)

Disparar e-mail para `contato@bellory.com.br` via:

- **Resend** (recomendado, free tier 3k/mês, DKIM automático)
- ou SendGrid / Postmark / SES

Template HTML básico, com `Reply-To: {email do lead}` para já responder direto.

### Opção B — Persistir + notificar

1. Salvar em tabela `contact_leads` (Postgres / qualquer):
   ```sql
   id            uuid pk
   created_at    timestamptz
   ip_hash       text         -- hash, não IP cru (LGPD)
   user_agent    text
   source        text
   name          text
   email         citext
   phone         text
   business_type text
   message       text
   fill_time_ms  int
   turnstile_ok  bool
   ```
2. Disparar webhook Slack/Discord para o time comercial saber em tempo real.
3. (Opcional) Mandar e-mail de confirmação automático para o lead.

**Recomendação:** começar com Opção A; subir Opção B quando fluxo de leads justificar painel admin.

---

## 5. LGPD

O formulário coleta dados pessoais (nome, e-mail, telefone, negócio). Obrigações mínimas:

- **Base legal:** consentimento explícito do titular (clique em "Enviar recado").
- **Retenção:** definir TTL para o lead (ex.: 18 meses). Job diário deleta registros expirados.
- **IP:** armazenar **hash** (`sha256(ip + salt)`), não o IP cru. Útil para detectar abuso sem manter PII.
- **Direito de exclusão:** endpoint `DELETE /api/contato/lead?email=...` autenticado para o operador deletar a pedido do titular. Logue a ação.
- **Política de privacidade:** o link `política de privacidade` no form (hoje `href="#"` em `components/contact.tsx`) precisa apontar para uma página real. Versão da política consentida deve ser registrada no lead (`policy_version`).

---

## 6. Headers de segurança

Configurar no `next.config.mjs` para a rota `/api/contato`:

```js
{
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "strict-origin-when-cross-origin",
  "X-Frame-Options": "DENY",
  "Permissions-Policy": "camera=(), microphone=(), geolocation=()"
}
```

CORS: o endpoint só responde para o próprio domínio — não habilitar `Access-Control-Allow-Origin: *`.

---

## 7. Variáveis de ambiente

Backend:

```
TURNSTILE_SECRET_KEY=...           # secret da Cloudflare (PRIVADA)
RESEND_API_KEY=...                 # se Opção A
CONTACT_NOTIFICATION_EMAIL=contato@bellory.com.br
UPSTASH_REDIS_REST_URL=...         # rate limiter
UPSTASH_REDIS_REST_TOKEN=...
DATABASE_URL=...                   # se Opção B
LEAD_IP_HASH_SALT=...              # 32 bytes random
SLACK_LEADS_WEBHOOK=...            # opcional
```

Frontend (já consumido em `components/contact.tsx`):

```
NEXT_PUBLIC_TURNSTILE_SITE_KEY=... # site key da Cloudflare (pública)
```

---

## 8. Logging e observabilidade

Logar (sem PII em texto plano):

- `request_id`, `ip_hash`, `user_agent_hash`, `result` (`ok` | `dropped:honeypot` | `dropped:too_fast` | `rejected:turnstile` | `rejected:rate_limit` | `error`)
- `business_type`, `source` (não-PII, útil pra analytics).

Métricas mínimas:

- Taxa de honeypot/time-trap dispared (sinal de pressão de bot).
- Taxa de Turnstile fail.
- p95 de latência da rota.
- Volume diário de leads válidos (vs. spam descartado).

---

## 9. Checklist de implementação

- [ ] Criar rota `POST /api/contato` (Next.js Route Handler em `app/api/contato/route.ts` ou serviço externo).
- [ ] Configurar Upstash + rate limiter.
- [ ] Configurar Cloudflare Turnstile (site key pública + secret privada). Domain: `bellory.com.br`.
- [ ] Importar/replicar `lib/contact-schema.ts` no backend.
- [ ] Implementar handler na ordem da seção 2.
- [ ] Configurar Resend (ou alternativa) e remetente verificado no domínio.
- [ ] Definir tabela `contact_leads` (se Opção B) com TTL.
- [ ] Endpoint admin para exclusão LGPD.
- [ ] Headers de segurança em `next.config.mjs`.
- [ ] Página real de política de privacidade + atualizar link no form.
- [ ] Testes: payload válido, honeypot preenchido, fillTime curto, token inválido, rate limit estourado, origin estranho.

---

## 10. Pseudo-código de referência (Next.js Route Handler)

```ts
// app/api/contato/route.ts
import { NextRequest, NextResponse } from "next/server"
import { Ratelimit } from "@upstash/ratelimit"
import { Redis } from "@upstash/redis"
import { contactPayloadSchema, CONTACT_LIMITS } from "@/lib/contact-schema"
import { Resend } from "resend"
import { createHash } from "crypto"

const redis = Redis.fromEnv()
const ratelimitMin = new Ratelimit({ redis, limiter: Ratelimit.slidingWindow(3, "1 m") })
const ratelimitHour = new Ratelimit({ redis, limiter: Ratelimit.slidingWindow(10, "1 h") })

const ALLOWED_ORIGINS = new Set([
  "https://bellory.com.br",
  "https://www.bellory.com.br",
  ...(process.env.NODE_ENV !== "production" ? ["http://localhost:3000"] : []),
])

export async function POST(req: NextRequest) {
  const origin = req.headers.get("origin") ?? ""
  if (!ALLOWED_ORIGINS.has(origin)) {
    return NextResponse.json({ ok: false, code: "forbidden_origin" }, { status: 403 })
  }

  const ip = (req.headers.get("x-forwarded-for") ?? "0.0.0.0").split(",")[0].trim()
  const minOk = await ratelimitMin.limit(`contato:min:${ip}`)
  const hourOk = await ratelimitHour.limit(`contato:hour:${ip}`)
  if (!minOk.success || !hourOk.success) {
    return NextResponse.json({ ok: false, code: "rate_limited" }, { status: 429 })
  }

  const json = await req.json().catch(() => null)
  if (!json) return NextResponse.json({ ok: false, code: "validation" }, { status: 400 })

  // Honeypot — pretend success
  if (typeof json.website === "string" && json.website.trim().length > 0) {
    return NextResponse.json({ ok: true, id: "drop_honeypot" })
  }
  // Time-trap — pretend success
  if (typeof json.fillTimeMs === "number" && json.fillTimeMs < CONTACT_LIMITS.fillTimeMs.min) {
    return NextResponse.json({ ok: true, id: "drop_too_fast" })
  }

  const parsed = contactPayloadSchema.safeParse(json)
  if (!parsed.success) {
    return NextResponse.json({ ok: false, code: "validation" }, { status: 400 })
  }

  const turnstile = await fetch(
    "https://challenges.cloudflare.com/turnstile/v0/siteverify",
    {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        secret: process.env.TURNSTILE_SECRET_KEY!,
        response: parsed.data.turnstileToken,
        remoteip: ip,
      }),
    },
  ).then((r) => r.json()).catch(() => null)

  if (!turnstile?.success) {
    return NextResponse.json({ ok: false, code: "turnstile_failed" }, { status: 400 })
  }

  // Sanitize message
  const cleanMessage = parsed.data.message.replace(/<[^>]*>/g, "").trim()
  const ipHash = createHash("sha256")
    .update(ip + process.env.LEAD_IP_HASH_SALT!)
    .digest("hex")

  // Persist + notify (simplified)
  // await db.insertContactLead({ ...parsed.data, message: cleanMessage, ipHash })
  await new Resend(process.env.RESEND_API_KEY!).emails.send({
    from: "Bellory <leads@bellory.com.br>",
    to: process.env.CONTACT_NOTIFICATION_EMAIL!,
    replyTo: parsed.data.email,
    subject: `Novo lead: ${parsed.data.name} (${parsed.data.businessType})`,
    text: `${cleanMessage}\n\n— ${parsed.data.name}\n${parsed.data.email}\n${parsed.data.phone}`,
  })

  return NextResponse.json({ ok: true })
}
```

---

## Referências

- Cloudflare Turnstile: https://developers.cloudflare.com/turnstile/
- Upstash Ratelimit: https://github.com/upstash/ratelimit
- Resend: https://resend.com/docs
- LGPD: lei 13.709/2018, art. 7º (bases legais), 18 (direitos do titular).
