# Modulo de API Keys — Bellory-Admin-Front

> Guia de implementacao da tela e do servico de API Keys no painel admin
> (front-end). Consome o modulo `/api/v1/admin/api-keys` da `Bellory-Admin-API`.

**Data de criacao:** 2026-05-09
**Projeto alvo:** `Bellory-Admin-Front` (painel administrativo, React/Next/Vite — adaptar conforme stack)
**Backend:** `Bellory-Admin-API` (ver `API_KEYS_ADMIN_MODULO.md`)

---

## Sumario

1. [Visao geral](#visao-geral)
2. [Pre-requisitos](#pre-requisitos)
3. [Estrutura de arquivos sugerida](#estrutura-de-arquivos-sugerida)
4. [Tipos / DTOs](#tipos--dtos)
5. [Service HTTP](#service-http)
6. [Hook de dados (React Query)](#hook-de-dados-react-query)
7. [Componentes de UI](#componentes-de-ui)
   - [Pagina principal `ApiKeysPage`](#pagina-principal-apikeyspage)
   - [Modal `ApiKeyCreateModal`](#modal-apikeycreatemodal)
   - [Modal `ApiKeyRevealModal`](#modal-apikeyrevealmodal)
   - [Modal `ApiKeyRevokeConfirmModal`](#modal-apikeyrevokeconfirmmodal)
8. [Roteamento e menu](#roteamento-e-menu)
9. [Permissoes e visibilidade](#permissoes-e-visibilidade)
10. [UX — pontos criticos](#ux--pontos-criticos)
11. [Acessibilidade](#acessibilidade)
12. [Checklist de implementacao](#checklist-de-implementacao)
13. [Contrato HTTP de referencia](#contrato-http-de-referencia)

---

## Visao geral

Tela de gerenciamento de API Keys da plataforma Bellory. Cada `UsuarioAdmin`
pode emitir, listar e revogar suas proprias chaves, alem de ver (somente
leitura) todas as chaves emitidas na plataforma para auditoria.

**Fluxo principal:**

1. Admin abre `/admin/api-keys` -> ve lista das proprias chaves
2. Clica em "Nova chave" -> preenche nome, descricao, expiracao
3. Backend retorna a chave bruta UMA UNICA VEZ -> modal mostra com botao "Copiar"
4. Apos fechar o modal, a chave nao pode mais ser recuperada (so o hash fica salvo)
5. Pode revogar qualquer chave propria (soft delete)
6. Aba "Auditoria" -> lista global (somente leitura)

**Caracteristicas tecnicas:**

- Autenticacao: JWT (mesmo da sessao do admin) — header `Authorization: Bearer <token>`
- Endpoints: `/api/v1/admin/api-keys`
- Wrapper de resposta: depende do endpoint — ver [Contrato HTTP](#contrato-http-de-referencia)

---

## Pre-requisitos

| Item | Estado esperado |
|---|---|
| Backend `V76` aplicado | Tabela `admin.api_keys` no schema admin |
| `JwtAuthFilter` ja roteia `X-API-Key` | OK (configurado no backend) |
| Sessao admin com JWT valido | Ja existe — login em `/api/v1/admin/auth/login` |
| Cliente HTTP do admin | Ja existe (axios/fetch) — reaproveitar |
| Toast / notificacao | Ja existe — reaproveitar |
| Modal / dialog | Ja existe — reaproveitar |
| Lib de tabela | Ja existe — reaproveitar |

> Se o stack ainda usa fetch nativo + Context, adapte os exemplos abaixo de
> React Query para o que ja for padrao do projeto. **Nao introduza biblioteca
> nova so pra essa tela.**

---

## Estrutura de arquivos sugerida

```
src/
├── pages/admin/api-keys/
│   ├── ApiKeysPage.tsx              # Tela principal (rotas: minhas / auditoria)
│   ├── components/
│   │   ├── ApiKeysTable.tsx         # Tabela reutilizavel
│   │   ├── ApiKeyCreateModal.tsx    # Form de criacao
│   │   ├── ApiKeyRevealModal.tsx    # Mostra a chave bruta UMA VEZ
│   │   └── ApiKeyRevokeConfirmModal.tsx
│   └── index.ts                      # Exporta default ApiKeysPage
├── services/admin/
│   └── apiKeyAdminService.ts        # Funcoes HTTP
├── hooks/admin/
│   └── useApiKeys.ts                # Hooks de dados (React Query / SWR / etc.)
└── types/admin/
    └── apiKey.ts                    # Tipos TS
```

---

## Tipos / DTOs

`src/types/admin/apiKey.ts`:

```ts
export interface ApiKeyListItem {
  id: number;
  name: string;
  description: string;
  username: string;
  lastUsedAt: string | null;     // ISO LocalDateTime (sem TZ)
  expiresAt: string | null;
  createdAt: string;
}

export interface CreateApiKeyRequest {
  name: string;                  // 1..100 chars
  description?: string;          // 0..500 chars
  expiresInDays?: number;        // > 0; ausente = chave nao expira
}

export interface CreateApiKeyResponse {
  success: true;
  message: string;
  apiKey: string;                // bly_adm_xxx... — UMA VEZ SO
  id: number;
  name: string;
  expiresAt: string | "Sem expiracao";
}

export interface ListApiKeysResponse {
  success: true;
  apiKeys: ApiKeyListItem[];
}

export interface RevokeApiKeyResponse {
  success: true;
  message: string;
}
```

> **Atencao:** o backend nao envelopa todas as respostas em `ResponseAPI<T>`
> — esse controller usa `Map.of(...)` direto. Se um dia for padronizado, os
> tipos acima viram `ResponseAPI<...>` e os componentes leem `.dados`.

---

## Service HTTP

`src/services/admin/apiKeyAdminService.ts`:

```ts
import { httpClient } from "@/services/httpClient"; // axios pre-configurado
import {
  ApiKeyListItem,
  CreateApiKeyRequest,
  CreateApiKeyResponse,
  ListApiKeysResponse,
  RevokeApiKeyResponse,
} from "@/types/admin/apiKey";

const BASE = "/api/v1/admin/api-keys";

export const apiKeyAdminService = {
  async listMine(): Promise<ApiKeyListItem[]> {
    const { data } = await httpClient.get<ListApiKeysResponse>(BASE);
    return data.apiKeys;
  },

  async listAll(): Promise<ApiKeyListItem[]> {
    const { data } = await httpClient.get<ListApiKeysResponse>(`${BASE}/all`);
    return data.apiKeys;
  },

  async create(payload: CreateApiKeyRequest): Promise<CreateApiKeyResponse> {
    const { data } = await httpClient.post<CreateApiKeyResponse>(BASE, payload);
    return data;
  },

  async revoke(id: number): Promise<RevokeApiKeyResponse> {
    const { data } = await httpClient.delete<RevokeApiKeyResponse>(`${BASE}/${id}`);
    return data;
  },
};
```

> O `httpClient` ja deve injetar o `Authorization: Bearer <jwt>` via interceptor.

---

## Hook de dados (React Query)

`src/hooks/admin/useApiKeys.ts`:

```ts
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiKeyAdminService } from "@/services/admin/apiKeyAdminService";
import type {
  CreateApiKeyRequest,
  CreateApiKeyResponse,
} from "@/types/admin/apiKey";

const KEYS = {
  mine: ["apiKeys", "mine"] as const,
  all: ["apiKeys", "all"] as const,
};

export function useMyApiKeys() {
  return useQuery({
    queryKey: KEYS.mine,
    queryFn: apiKeyAdminService.listMine,
    staleTime: 30_000,
  });
}

export function useAllApiKeys(enabled: boolean) {
  return useQuery({
    queryKey: KEYS.all,
    queryFn: apiKeyAdminService.listAll,
    enabled,
    staleTime: 30_000,
  });
}

export function useCreateApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateApiKeyRequest) => apiKeyAdminService.create(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.mine });
      qc.invalidateQueries({ queryKey: KEYS.all });
    },
  });
}

export function useRevokeApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiKeyAdminService.revoke(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEYS.mine });
      qc.invalidateQueries({ queryKey: KEYS.all });
    },
  });
}
```

---

## Componentes de UI

### Pagina principal `ApiKeysPage`

**Layout:**

```
+-----------------------------------------------------------+
| Chaves de API                       [ + Nova chave ]      |
+-----------------------------------------------------------+
| Tabs:  [ Minhas chaves ]  [ Auditoria (todas) ]           |
+-----------------------------------------------------------+
| Nome     | Username | Ultimo uso | Expira em | Acoes      |
| ---      | ---      | ---        | ---       | ---        |
| N8N sync | matheus  | ha 2h      | 364d      | [Revogar]  |
| ...      | ...      | ...        | ...       | ...        |
+-----------------------------------------------------------+
```

**Comportamento:**

- Tab "Minhas chaves" -> `useMyApiKeys()` (default ativo)
- Tab "Auditoria" -> `useAllApiKeys(true)` quando ativada
- Coluna "Ultimo uso": format relativo (`date-fns formatDistance`); se `null`, mostrar "Nunca usada"
- Coluna "Expira em":
  - `null` -> badge "Sem expiracao"
  - `< now` -> badge vermelho "Expirada" (e ainda assim listada se backend retornar)
  - `< 7 dias` -> badge amarelo "Expira em 5d"
  - resto -> texto neutro "DD/MM/AAAA"
- Acao "Revogar": so aparece em "Minhas chaves" (revogar de outro admin nao e suportado)
- Empty state: "Voce ainda nao criou nenhuma chave. Clique em 'Nova chave' para comecar."

**Esqueleto:**

```tsx
// src/pages/admin/api-keys/ApiKeysPage.tsx
import { useState } from "react";
import { useMyApiKeys, useAllApiKeys } from "@/hooks/admin/useApiKeys";
import { ApiKeysTable } from "./components/ApiKeysTable";
import { ApiKeyCreateModal } from "./components/ApiKeyCreateModal";
import { ApiKeyRevealModal } from "./components/ApiKeyRevealModal";
import { ApiKeyRevokeConfirmModal } from "./components/ApiKeyRevokeConfirmModal";
import type { CreateApiKeyResponse } from "@/types/admin/apiKey";

export default function ApiKeysPage() {
  const [tab, setTab] = useState<"mine" | "all">("mine");
  const [createOpen, setCreateOpen] = useState(false);
  const [revealed, setRevealed] = useState<CreateApiKeyResponse | null>(null);
  const [revokingId, setRevokingId] = useState<number | null>(null);

  const mine = useMyApiKeys();
  const all = useAllApiKeys(tab === "all");

  const data = tab === "mine" ? mine.data : all.data;
  const isLoading = tab === "mine" ? mine.isLoading : all.isLoading;

  return (
    <section className="p-6">
      <header className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-2xl font-semibold">Chaves de API</h1>
          <p className="text-sm text-muted">
            Use chaves para autenticar integracoes externas (N8N, scripts, jobs).
          </p>
        </div>
        <button onClick={() => setCreateOpen(true)} className="btn btn-primary">
          + Nova chave
        </button>
      </header>

      <nav className="tabs mb-4">
        <button onClick={() => setTab("mine")} aria-current={tab === "mine"}>
          Minhas chaves
        </button>
        <button onClick={() => setTab("all")} aria-current={tab === "all"}>
          Auditoria (todas)
        </button>
      </nav>

      <ApiKeysTable
        data={data ?? []}
        loading={isLoading}
        showActions={tab === "mine"}
        onRevoke={(id) => setRevokingId(id)}
      />

      {createOpen && (
        <ApiKeyCreateModal
          onClose={() => setCreateOpen(false)}
          onCreated={(resp) => {
            setCreateOpen(false);
            setRevealed(resp);
          }}
        />
      )}

      {revealed && (
        <ApiKeyRevealModal
          payload={revealed}
          onClose={() => setRevealed(null)}
        />
      )}

      {revokingId !== null && (
        <ApiKeyRevokeConfirmModal
          id={revokingId}
          onDone={() => setRevokingId(null)}
        />
      )}
    </section>
  );
}
```

---

### Modal `ApiKeyCreateModal`

**Form:**

| Campo | Tipo | Validacao |
|---|---|---|
| `name` | text | required, 1..100 |
| `description` | textarea | optional, max 500 |
| `expiresInDays` | select / number | optional, > 0 |

**Sugerir presets de expiracao:** `30`, `90`, `180`, `365`, `Nunca`.
Default: `365`.

**Disable submit enquanto `useCreateApiKey().isPending`.**

```tsx
// src/pages/admin/api-keys/components/ApiKeyCreateModal.tsx
import { useState } from "react";
import { useCreateApiKey } from "@/hooks/admin/useApiKeys";
import type { CreateApiKeyResponse } from "@/types/admin/apiKey";

const PRESETS = [
  { label: "30 dias",  value: 30 },
  { label: "90 dias",  value: 90 },
  { label: "180 dias", value: 180 },
  { label: "1 ano",    value: 365 },
  { label: "Nunca",    value: null },
];

interface Props {
  onClose: () => void;
  onCreated: (resp: CreateApiKeyResponse) => void;
}

export function ApiKeyCreateModal({ onClose, onCreated }: Props) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [expiresInDays, setExpiresInDays] = useState<number | null>(365);
  const create = useCreateApiKey();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    const resp = await create.mutateAsync({
      name: name.trim(),
      description: description.trim() || undefined,
      expiresInDays: expiresInDays ?? undefined,
    });

    onCreated(resp);
  };

  return (
    <Modal onClose={onClose} title="Criar nova API Key">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Nome *" required>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={100}
            autoFocus
            placeholder="Ex.: N8N - Sync diario"
          />
        </Field>

        <Field label="Descricao">
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={500}
            rows={3}
            placeholder="Para que essa chave sera usada?"
          />
        </Field>

        <Field label="Expiracao">
          <div className="flex gap-2 flex-wrap">
            {PRESETS.map((p) => (
              <button
                type="button"
                key={p.label}
                onClick={() => setExpiresInDays(p.value)}
                aria-pressed={expiresInDays === p.value}
                className="chip"
              >
                {p.label}
              </button>
            ))}
          </div>
        </Field>

        <footer className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className="btn">Cancelar</button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={create.isPending || !name.trim()}
          >
            {create.isPending ? "Criando..." : "Criar chave"}
          </button>
        </footer>

        {create.isError && (
          <p className="text-error text-sm">
            Falha ao criar a chave. Tente novamente.
          </p>
        )}
      </form>
    </Modal>
  );
}
```

---

### Modal `ApiKeyRevealModal`

**Critico:** essa e a unica vez que a chave bruta aparece. Backend so guarda hash.

**UX obrigatoria:**

- Banner amarelo: **"Copie agora. Nao sera possivel recuperar essa chave depois."**
- Campo readonly grande, monospace, com a chave selecionavel
- Botao "Copiar" que aciona `navigator.clipboard.writeText` + feedback visual ("Copiado!")
- Botao secundario "Mostrar/ocultar" (default: oculto com `••••••`)
- Footer: "Entendi e copiei a chave" (botao vermelho/destaque) — so esse fecha o modal
- **Sem `onClose` no overlay/ESC** — exige confirmacao explicita

```tsx
// src/pages/admin/api-keys/components/ApiKeyRevealModal.tsx
import { useState } from "react";
import type { CreateApiKeyResponse } from "@/types/admin/apiKey";

interface Props {
  payload: CreateApiKeyResponse;
  onClose: () => void;
}

export function ApiKeyRevealModal({ payload, onClose }: Props) {
  const [shown, setShown] = useState(false);
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    await navigator.clipboard.writeText(payload.apiKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Modal title="API Key criada" disableEscape disableBackdropClose>
      <div className="space-y-4">
        <div className="alert alert-warning">
          <strong>Copie agora.</strong> Esta e a unica vez que a chave sera
          exibida. Apos fechar este aviso, ela nao podera mais ser recuperada.
        </div>

        <Field label="Nome">
          <code>{payload.name}</code>
        </Field>

        <Field label="Sua chave">
          <div className="flex gap-2">
            <input
              readOnly
              value={shown ? payload.apiKey : "•".repeat(payload.apiKey.length)}
              className="font-mono text-sm flex-1"
              onFocus={(e) => e.currentTarget.select()}
            />
            <button type="button" onClick={() => setShown((s) => !s)} className="btn">
              {shown ? "Ocultar" : "Mostrar"}
            </button>
            <button type="button" onClick={copy} className="btn btn-primary">
              {copied ? "Copiado!" : "Copiar"}
            </button>
          </div>
        </Field>

        <Field label="Expira em">
          <span>{String(payload.expiresAt)}</span>
        </Field>

        <footer className="flex justify-end pt-2">
          <button onClick={onClose} className="btn btn-danger">
            Entendi e copiei a chave
          </button>
        </footer>
      </div>
    </Modal>
  );
}
```

---

### Modal `ApiKeyRevokeConfirmModal`

```tsx
// src/pages/admin/api-keys/components/ApiKeyRevokeConfirmModal.tsx
import { useRevokeApiKey } from "@/hooks/admin/useApiKeys";
import { toast } from "@/lib/toast";

interface Props {
  id: number;
  onDone: () => void;
}

export function ApiKeyRevokeConfirmModal({ id, onDone }: Props) {
  const revoke = useRevokeApiKey();

  const confirm = async () => {
    try {
      await revoke.mutateAsync(id);
      toast.success("Chave revogada");
      onDone();
    } catch {
      toast.error("Nao foi possivel revogar a chave");
    }
  };

  return (
    <Modal title="Revogar API Key" onClose={onDone}>
      <p>
        Apos revogada, esta chave deixa de autenticar imediatamente em qualquer
        integracao. <strong>Esta acao nao pode ser desfeita.</strong>
      </p>
      <footer className="flex justify-end gap-2 pt-4">
        <button onClick={onDone} className="btn">Cancelar</button>
        <button
          onClick={confirm}
          disabled={revoke.isPending}
          className="btn btn-danger"
        >
          {revoke.isPending ? "Revogando..." : "Sim, revogar"}
        </button>
      </footer>
    </Modal>
  );
}
```

---

## Roteamento e menu

**Rota:**

```ts
// src/routes/adminRoutes.tsx
{
  path: "/admin/api-keys",
  element: <ApiKeysPage />,
  // proteger por requireAuth + requireRole("PLATFORM_ADMIN")
}
```

**Item de menu lateral** (sob o grupo "Configuracoes" ou "Plataforma"):

```
- Configuracoes
  - Usuarios admin
  - Templates
  - Webhooks
  - Chaves de API   <-- novo, icone "key"
```

---

## Permissoes e visibilidade

| Acao | Quem pode |
|---|---|
| Ver minhas chaves | Qualquer admin autenticado |
| Criar chave | Qualquer admin autenticado |
| Revogar minha chave | Apenas o dono da chave (validado tambem no backend) |
| Aba "Auditoria (todas)" | Qualquer admin (mas considerar restringir a `SUPERADMIN` se a role existir no front) |

> O backend ja valida ownership na revogacao. Mesmo assim, **nao mostre o
> botao "Revogar" para chaves de outros admins na aba auditoria** — evita
> 401 visivel pro usuario.

---

## UX — pontos criticos

1. **A chave bruta volta UMA UNICA VEZ.** Toda a UI de criacao precisa deixar
   isso obvio antes do usuario submeter o form (subtitulo do modal create).
2. **O modal de reveal nao fecha por ESC nem clique no backdrop.** Forca
   confirmacao explicita.
3. **Apos copiar, mantenha o feedback "Copiado!" por ~2s** — o usuario duvida
   quando o clique nao reage visualmente.
4. **Nao mostre a chave em logs do front** (Sentry, console.log de dev), apenas
   o `id` e o `name`.
5. **Coluna "Ultimo uso" e o melhor sinal de chave esquecida** — destacar
   chaves nao usadas ha > 90 dias com uma cor neutra ("Inativa ha 4m").
6. **Nao confundir chave revogada com chave expirada** — backend ja filtra por
   `ativo=true`, logo a tela so mostra ativas. Se quiser mostrar expiradas
   ainda nao revogadas, exigir endpoint extra (nao existe hoje).

---

## Acessibilidade

- Botao "Copiar" precisa de `aria-live="polite"` no feedback "Copiado!"
- Modal de reveal: `role="alertdialog"` (forca confirmacao)
- Tabela: usar `<th scope="col">` e `<caption>` invisivel ("Lista de API Keys")
- Form de criacao: `aria-required="true"` no nome; mensagens de erro com `role="alert"`
- Foco: ao abrir modal de reveal, focar no botao "Copiar" (nao no campo da chave — evita selecao acidental)

---

## Checklist de implementacao

No `Bellory-Admin-Front`:

- [ ] Criar tipos em `types/admin/apiKey.ts`
- [ ] Criar `apiKeyAdminService.ts`
- [ ] Criar hooks `useMyApiKeys`, `useAllApiKeys`, `useCreateApiKey`, `useRevokeApiKey`
- [ ] Criar `ApiKeysPage` + componentes filhos
- [ ] Adicionar rota `/admin/api-keys`
- [ ] Adicionar item de menu "Chaves de API" (icone `key`)
- [ ] Implementar:
  - [ ] Listagem das proprias chaves
  - [ ] Listagem global (auditoria)
  - [ ] Modal de criacao
  - [ ] Modal de reveal (chave so mostrada uma vez)
  - [ ] Modal de revogacao com confirmacao
- [ ] Empty state ("Nenhuma chave criada ainda")
- [ ] Tratamento de erro (toast + fallback)
- [ ] Smoke test E2E:
  - [ ] Criar chave -> reveal aparece -> botao "Copiar" funciona
  - [ ] Apos fechar reveal, chave aparece na tabela com `lastUsedAt=null`
  - [ ] Usar a chave em outro endpoint (curl/postman) -> `lastUsedAt` atualiza
  - [ ] Revogar -> some da listagem
  - [ ] Aba auditoria mostra chaves de outros admins
- [ ] Verificar acessibilidade com leitor de tela no modal de reveal

---

## Contrato HTTP de referencia

> Detalhamento dos endpoints. Para o codigo back-end, ver
> [`API_KEYS_ADMIN_MODULO.md`](./API_KEYS_ADMIN_MODULO.md).

### `POST /api/v1/admin/api-keys`

Cria uma nova API Key. **A chave bruta e retornada apenas nesta resposta.**

**Auth:** `Authorization: Bearer <jwt-admin>`

**Body:**

```json
{
  "name": "N8N - Sync diario",
  "description": "Job das 03:00",
  "expiresInDays": 365
}
```

**200 OK:**

```json
{
  "success": true,
  "message": "API Key criada com sucesso. Copie agora, nao sera exibida novamente!",
  "apiKey": "bly_adm_xQ5z...",
  "id": 1,
  "name": "N8N - Sync diario",
  "expiresAt": "2027-05-09T15:30:00"
}
```

> Quando `expiresInDays` e omitido, o campo `expiresAt` na resposta vira a string literal `"Sem expiracao"`. Tratar os dois casos no front.

---

### `GET /api/v1/admin/api-keys`

Lista as chaves do admin autenticado (apenas ativas).

**200 OK:**

```json
{
  "success": true,
  "apiKeys": [
    {
      "id": 1,
      "name": "N8N - Sync diario",
      "description": "Job das 03:00",
      "username": "matheus.admin",
      "lastUsedAt": "2026-05-09T03:00:01",
      "expiresAt": "2027-05-09T15:30:00",
      "createdAt": "2026-05-09T15:30:00"
    }
  ]
}
```

---

### `GET /api/v1/admin/api-keys/all`

Lista TODAS as chaves ativas da plataforma (auditoria). Mesmo shape de `GET /`.

---

### `DELETE /api/v1/admin/api-keys/{id}`

Revoga uma chave (soft delete via `ativo=false`). Apenas o dono pode revogar.

**200 OK:**

```json
{
  "success": true,
  "message": "API Key revogada"
}
```

**400 Bad Request** (se nao for o dono ou ID nao existir): mensagem em
`message` — tratar como toast de erro generico no front.

---

### Uso da chave em outras integracoes

```bash
curl https://api-admin.bellory.com.br/api/v1/admin/dashboard \
  -H "X-API-Key: bly_adm_xQ5z..."
```

A chave funciona em qualquer endpoint sob `/api/v1/admin/**` que normalmente
exigiria JWT, ja que o `JwtAuthFilter` aceita os dois mecanismos.

---

## Historico

| Data | Versao | Alteracao |
|---|---|---|
| 2026-05-09 | 1.0 | Documento inicial |
