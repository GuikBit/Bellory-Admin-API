# Bellory Admin API — Contrato do Modulo de Leads

Documento para o frontend do painel admin (kanban + gestao de leads) e para
a landing page (`bellory.com.br`) consumirem.

Base URLs:

- Dev: `http://localhost:8085`
- Prod: `https://api-admin.bellory.com.br`

Autenticacao:

- Endpoints sob `/api/v1/admin/**` exigem JWT (header `Authorization: Bearer <token>`),
  com claim `userType=PLATFORM_ADMIN`.
- O endpoint `/api/contato` e PUBLICO (sem token, com Turnstile).

Wrapper padrao das respostas admin (`ResponseAPI<T>`):

```json
{
  "success": true,
  "message": "...",
  "dados": { /* T */ },
  "errorCode": null,
  "errors": null
}
```

---

## 1. Endpoint publico — formulario de contato

### `POST /api/contato`

Recebe um lead da landing page. Implementa origin check, rate-limit (3/min,
10/h, 30/dia por IP), honeypot, time-trap, validacao Bean-Validation e
verificacao Cloudflare Turnstile.

**Request body** (`ContactPayloadDTO`):

```json
{
  "name": "Maria Souza",
  "email": "maria@salaobelo.com.br",
  "phone": "(11) 99999-1234",
  "businessType": "SALAO",
  "message": "Quero saber se voces integram com meu sistema atual...",
  "website": "",
  "turnstileToken": "0.abc123...",
  "fillTimeMs": 24500,
  "source": "landing/contato",
  "policyVersion": "v1.0"
}
```

`businessType`: `SALAO` | `BARBEARIA` | `CLINICA` | `NAIL` | `SPA` | `OUTRO`.

**Sucesso** — `200 OK`:

```json
{ "ok": true, "id": "lead_a1b2c3d4e5f6" }
```

> O `id` e um identificador opaco para o frontend mostrar/logar. Nao e o
> UUID interno do lead. Honeypot/time-trap retornam `id: "drop_honeypot"` ou
> `"drop_too_fast"` (mesmo HTTP 200, para nao revelar ao bot).

**Erros** (sempre JSON `{ ok: false, code, message }`):

| HTTP | `code`               | Quando                            |
| ---- | -------------------- | --------------------------------- |
| 400  | `validation`         | Falha no Bean Validation / JSON   |
| 400  | `turnstile_failed`   | Token Turnstile invalido          |
| 403  | `forbidden_origin`   | Origin/Referer nao autorizado     |
| 429  | `rate_limited`       | Estourou o rate-limit             |
| 500  | `server`             | Erro interno                      |

Frontend deve mapear `code` para mensagem local (nao usar `message`).

---

## 2. Endpoints admin — Status do kanban

Base: `/api/v1/admin/leads/status`

### `GET /api/v1/admin/leads/status`

Lista colunas configuraveis do kanban.

Query params:

- `ativosApenas` (bool, default `false`) — quando `true`, filtra `ativo=true`.

Resposta `dados`:

```json
[
  {
    "id": 1,
    "codigo": "NOVO",
    "nome": "Novo",
    "cor": "#3B82F6",
    "ordem": 10,
    "ativo": true,
    "ehStatusInicial": true,
    "ehStatusFinal": false
  }
]
```

### `POST /api/v1/admin/leads/status`

Cria nova coluna.

Request:

```json
{
  "codigo": "AGUARDANDO_RETORNO",
  "nome": "Aguardando retorno",
  "cor": "#FB923C",
  "ordem": 25,
  "ehStatusInicial": false,
  "ehStatusFinal": false
}
```

`cor` regex `^#[0-9A-Fa-f]{6}$`. `ordem` opcional (vai pro fim se omitido).

### `PUT /api/v1/admin/leads/status/{id}`

Atualiza nome/cor/ordem/ativo/flags. Todos os campos sao opcionais (PATCH semantics
mesmo sendo PUT).

> Nao eh possivel desmarcar `ehStatusInicial` sem definir outra coluna como inicial
> (sempre precisa existir uma).

### `PATCH /api/v1/admin/leads/status/reorder`

Bulk reorder.

Request:

```json
[
  { "id": 1, "ordem": 10 },
  { "id": 2, "ordem": 20 },
  { "id": 3, "ordem": 30 }
]
```

### `DELETE /api/v1/admin/leads/status/{id}?moveTo={outroId}`

Inativa a coluna. Se houver leads no status, a API erro pedindo para realocar
manualmente antes (mover endpoint bulk fica para fase posterior).

---

## 3. Endpoints admin — Leads (CRUD + kanban)

Base: `/api/v1/admin/leads`

### `GET /api/v1/admin/leads/kanban`

Estrutura completa do kanban: array de colunas (status ativos na ordem) com os
leads de cada coluna.

Resposta `dados`:

```json
[
  {
    "status": { "id": 1, "codigo": "NOVO", "nome": "Novo", "cor": "#3B82F6", "...": "..." },
    "leads": [
      {
        "id": "8f...uuid",
        "nome": "Maria Souza",
        "email": "maria@salaobelo.com.br",
        "telefone": "(11) 99999-1234",
        "tipoNegocio": "SALAO",
        "status": { "...": "..." },
        "prioridade": "MEDIA",
        "tags": ["site"],
        "valorEstimado": null,
        "dataPrevistaFechamento": null,
        "responsavel": null,
        "origem": "landing/contato",
        "dtCriacao": "2026-05-08T10:30:00"
      }
    ],
    "total": 1
  }
]
```

### `GET /api/v1/admin/leads`

Listagem paginada com filtros (todos opcionais):

| Query           | Tipo     | Notas                                       |
| --------------- | -------- | ------------------------------------------- |
| `statusId`      | long     |                                             |
| `responsavelId` | long     |                                             |
| `prioridade`    | enum     | `BAIXA` \| `MEDIA` \| `ALTA`                |
| `from`          | ISO date | `dtCriacao >= from`                         |
| `to`            | ISO date | `dtCriacao <= to`                           |
| `q`             | string   | busca em nome/email/telefone (LIKE)         |
| `page`          | int      | default `0`                                 |
| `size`          | int      | default `20`, max `100`                     |

Resposta `dados`: `Page<LeadListDTO>` (formato Spring Data — `content`, `totalElements`,
`totalPages`, etc).

### `GET /api/v1/admin/leads/{id}` — `id` = UUID

Detalhe completo do lead (sem atividades — busque separadamente).

### `POST /api/v1/admin/leads`

Cria lead manualmente pelo painel (sem turnstile).

Request:

```json
{
  "nome": "Joao da Silva",
  "email": "joao@studiojoao.com.br",
  "telefone": "(11) 91234-5678",
  "tipoNegocio": "BARBEARIA",
  "mensagem": "Cliente referenciado pelo Carlos.",
  "origem": "indicacao/Carlos",
  "prioridade": "ALTA",
  "tags": ["indicacao", "premium"],
  "valorEstimado": 2500.00,
  "dataPrevistaFechamento": "2026-06-01",
  "statusId": null,
  "responsavelId": 5
}
```

`statusId` opcional — se omitido, vai pro status inicial. `responsavelId` opcional.
`mensagem` opcional na criacao manual (admin pode ainda nao ter conversado com o
cliente); o endpoint publico `/api/contato` continua exigindo min 10 chars.

Atividades automaticas registradas: `LEAD_CRIADO`, e `ATRIBUICAO` se `responsavelId`
for informado.

### `PATCH /api/v1/admin/leads/{id}`

Atualiza campos do lead. Todos opcionais.

```json
{
  "nome": "Joao Silva",
  "prioridade": "MEDIA",
  "tags": ["indicacao"],
  "valorEstimado": 3000.00
}
```

> Para mudar status ou responsavel, use os endpoints `/status` e `/assign` (eles
> registram atividades automaticas no historico).

### `PATCH /api/v1/admin/leads/{id}/status`

Move o card no kanban.

```json
{
  "statusId": 3,
  "comentario": "Cliente aceitou a proposta inicial via WhatsApp."
}
```

`comentario` (opcional, max 500 chars) entra na descricao da atividade `MUDANCA_STATUS`.

### `PATCH /api/v1/admin/leads/{id}/assign`

Atribui ou desatribui responsavel.

```json
{ "responsavelId": 5 }
```

`null` desatribui. Registra atividade `ATRIBUICAO`.

### `DELETE /api/v1/admin/leads/{id}`

Soft delete — marca `deleted_at`, lead some das listagens.

### `DELETE /api/v1/admin/leads/{id}/lgpd`

Hard delete — apaga registro + atividades (cascade). **Usar apenas a pedido do
titular dos dados (LGPD art. 18).** A operacao e logada em WARN.

---

## 4. Endpoints admin — Atividades / Historico

Base: `/api/v1/admin/leads/{leadId}/atividades`

### `GET .../atividades?tipo={tipo}`

Lista historico do lead em ordem decrescente. Filtro opcional por tipo.

Tipos:
- Auto: `LEAD_CRIADO`, `MUDANCA_STATUS`, `ATRIBUICAO`
- Manual: `COMENTARIO`, `LIGACAO`, `EMAIL`, `WHATSAPP`, `REUNIAO`, `NOTA_INTERNA`

Resposta `dados`:

```json
[
  {
    "id": 42,
    "tipo": "MUDANCA_STATUS",
    "descricao": "Status alterado: NOVO -> EM_CONTATO | Liguei agora",
    "dados": { "fromStatus": "NOVO", "toStatus": "EM_CONTATO" },
    "autor": { "id": 1, "username": "matheus", "nomeCompleto": "Matheus Lima" },
    "dtCriacao": "2026-05-08T11:15:30",
    "automatica": true
  }
]
```

### `POST .../atividades`

Adiciona atividade manual.

```json
{
  "tipo": "LIGACAO",
  "descricao": "Liguei as 14h, ficou de retornar amanha pela manha."
}
```

Tipos automaticos sao rejeitados aqui (eles vem dos endpoints de status/assign).

### `DELETE .../atividades/{atividadeId}`

Apaga atividade manual. Atividades automaticas nao podem ser apagadas (auditoria).

---

## 5. Convencoes e regras de negocio

- **Tag `site`**: leads que chegam via `POST /api/contato` recebem automaticamente
  a tag `"site"` para diferenciar dos criados manualmente. Frontend pode usar isso
  no filtro/badge do card no kanban.
- **Email**: armazenado em lowercase. Use `LOWER(email)` para buscar duplicatas.
- **Status inicial**: sempre exatamente 1 status com `ehStatusInicial=true`.
  Constraint parcial unique no banco. Lead novo sem `statusId` cai aqui.
- **Soft delete**: `deleted_at` populado = lead some das listagens. Hard delete
  e exclusivo para LGPD.
- **`policyVersion`**: registrar a versao da politica de privacidade que o
  usuario consentiu, para fins de LGPD.
- **`ip_hash`**: nunca expor o IP cru. Sempre `sha256(ip + LEAD_IP_HASH_SALT)`.

---

## 6. Variaveis de ambiente do backend

| Var                                  | Onde       | Notas                                       |
| ------------------------------------ | ---------- | ------------------------------------------- |
| `TURNSTILE_SECRET_KEY`               | prod, dev  | secret da Cloudflare (PRIVADA)              |
| `LEAD_IP_HASH_SALT`                  | prod, dev  | 32 bytes random; trocar antes de produzir   |
| `BELLORY_CONTACT_ALLOWED_ORIGINS`    | prod, dev  | CSV de origens autorizadas                  |
| `BELLORY_CONTACT_RL_MIN/HOUR/DAY`    | opcional   | overrides do rate limit (default 3/10/30)   |
| `TURNSTILE_VERIFY_URL`               | opcional   | endpoint da Cloudflare (default ja correto) |

Frontend da landing precisa apenas de `NEXT_PUBLIC_TURNSTILE_SITE_KEY` (publica).

---

## 7. Roadmap pos-fase-base

- Endpoint bulk para realocar leads em massa entre colunas.
- Notificacao (Slack/Resend) quando chega lead novo.
- Estatisticas/funil (taxa de conversao por coluna).
- Endpoint admin para agendar follow-ups.
- Job diario de soft-delete por TTL (LGPD §5).
