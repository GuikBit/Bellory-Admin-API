# Autenticacao e Sessao JWT — Bellory-Admin-Front

> Guia dos endpoints de login / validacao de sessao do painel admin e da
> correcao do bug "sistema abre no dashboard com token invalido".

**Data de criacao:** 2026-05-12
**Projeto alvo:** `Bellory-Admin-Front` (painel administrativo — React/Next/Vite, adaptar conforme stack)
**Backend:** `Bellory-Admin-API` — controller `AdminAuthController` (`/api/v1/admin/auth`)

---

## Sumario

1. [O problema atual](#o-problema-atual)
2. [Endpoints de autenticacao](#endpoints-de-autenticacao)
   - [POST /api/v1/admin/auth/login](#post-apiv1adminauthlogin)
   - [POST /api/v1/admin/auth/validate](#post-apiv1adminauthvalidate)
   - [POST /api/v1/admin/auth/refresh](#post-apiv1adminauthrefresh)
   - [GET /api/v1/admin/auth/me](#get-apiv1adminauthme)
3. [Estrutura do token JWT](#estrutura-do-token-jwt)
4. [Codigos de erro](#codigos-de-erro)
5. [Como corrigir o bug no front-end](#como-corrigir-o-bug-no-front-end)
   - [Causa raiz](#causa-raiz)
   - [Fluxo de bootstrap correto](#fluxo-de-bootstrap-correto)
   - [Exemplo de service HTTP](#exemplo-de-service-http)
   - [Exemplo de guard de rota](#exemplo-de-guard-de-rota)
   - [Interceptor 401 global](#interceptor-401-global)
6. [Checklist de implementacao](#checklist-de-implementacao)

---

## O problema atual

Hoje, ao abrir o sistema:

1. O front carrega e cai direto na primeira rota (`/dashboard`).
2. Existe um `token` salvo no `localStorage`, entao o guard de rota considera o usuario "logado".
3. Mas esse token esta **expirado / invalido** — entao toda chamada de API retorna `401`.
4. O guard nao trata isso, entao a tela fica quebrada ate o usuario deslogar manualmente e logar de novo.

A correcao: **antes de renderizar qualquer rota protegida, validar o token contra o backend**; se invalido, limpar a sessao e mandar para `/login`. E adicionar um interceptor que faz o mesmo em qualquer `401`.

---

## Endpoints de autenticacao

Base: `/api/v1/admin/auth` — **rotas publicas** (nao exigem autenticacao previa; `/validate` e `/me` leem o token do header manualmente).

Datas nas respostas vem no formato `dd/MM/yyyy HH:mm:ss` (timezone `America/Sao_Paulo`).

### POST /api/v1/admin/auth/login

Autentica usuario admin e devolve o token da sessao.

**Request body:**

```json
{
  "username": "admin",
  "password": "senha-do-admin"
}
```

| Campo | Regras |
|---|---|
| `username` | obrigatorio, 3–50 chars (o backend faz `trim().toLowerCase()`) |
| `password` | obrigatorio, minimo 6 chars |

**200 OK:**

```json
{
  "success": true,
  "message": "Login realizado com sucesso",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "nomeCompleto": "Administrador",
    "email": "admin@bellory.com.br",
    "role": "PLATFORM_ADMIN",
    "ativo": true,
    "dtCriacao": "01/01/2026 10:00:00"
  },
  "expiresAt": "12/05/2026 20:00:00"
}
```

**401 Unauthorized** — credenciais invalidas (`errorCode: INVALID_CREDENTIALS`).
**403 Forbidden** — conta desativada (`errorCode: ACCOUNT_DISABLED`).
**400 Bad Request** — payload invalido (validacao de campos).

> O front deve guardar `token`, `user` e `expiresAt`. `expiresAt` permite saber, sem decodificar o JWT, quando renovar/expirar a sessao localmente.

### POST /api/v1/admin/auth/validate

**Esse e o endpoint-chave para o bootstrap.** Valida se o token ainda serve.

**Request:** sem body. Header `Authorization: Bearer <token>`.

**200 OK** — token valido:

```json
{
  "valid": true,
  "username": "admin",
  "userId": 1,
  "userType": "PLATFORM_ADMIN",
  "roles": ["PLATFORM_ADMIN"],
  "expiresAt": "12/05/2026 20:00:00"
}
```

**401 Unauthorized** — token expirado / invalido / nao e de admin da plataforma.
Possiveis `errorCode`: `TOKEN_EXPIRED`, `INVALID_TOKEN`, `INVALID_TOKEN_TYPE`.

**400 Bad Request** — header `Authorization` ausente ou sem prefixo `Bearer ` (`errorCode: MISSING_TOKEN`).

### POST /api/v1/admin/auth/refresh

Gera um token novo a partir de um token ainda valido (estende a sessao). Util para
chamar quando faltar pouco para o `expiresAt`.

**Request body:**

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

**200 OK:**

```json
{
  "success": true,
  "message": "Token renovado com sucesso",
  "newToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "12/05/2026 22:00:00"
}
```

**401 Unauthorized** — token invalido/expirado ou nao e `PLATFORM_ADMIN`
(`errorCode: REFRESH_FAILED` ou `INVALID_TOKEN_TYPE`).

> Atencao: o `refresh` exige um token **ainda valido**. Se o token ja expirou, nao da
> para renovar — o usuario precisa logar de novo. Por isso o refresh deve ser
> proativo (antes de expirar), nao reativo.

### GET /api/v1/admin/auth/me

Retorna os dados do admin logado (mesmo objeto `user` do login).

**Request:** header `Authorization: Bearer <token>`.

**200 OK:**

```json
{
  "id": 1,
  "username": "admin",
  "nomeCompleto": "Administrador",
  "email": "admin@bellory.com.br",
  "role": "PLATFORM_ADMIN",
  "ativo": true,
  "dtCriacao": "01/01/2026 10:00:00"
}
```

**401 Unauthorized** — token invalido (`errorCode: INVALID_TOKEN`).
**400 Bad Request** — token ausente (`errorCode: MISSING_TOKEN`).

> Da para usar `/me` no lugar de `/validate` no bootstrap se voce ja quiser
> hidratar o estado do usuario na mesma chamada. `/validate` e mais barato e
> devolve `expiresAt` + `roles`; `/me` devolve o perfil completo. Recomendado:
> `/validate` no bootstrap e `/me` so quando precisar do perfil.

### Logout

Nao ha endpoint de logout — a sessao e **stateless** (JWT puro). Logout = o front
descartar o token (`localStorage.removeItem(...)`) e redirecionar para `/login`.

---

## Estrutura do token JWT

Algoritmo `HS256`, issuer `bellory-api`. Claims:

| Claim | Conteudo |
|---|---|
| `sub` | `username` do admin |
| `userId` | id numerico |
| `role` | role do admin (ex.: `PLATFORM_ADMIN`) |
| `nomeCompleto` | nome completo |
| `userType` | sempre `PLATFORM_ADMIN` nos tokens emitidos por essa API |
| `exp` | expiracao (epoch seconds) — default 36000s = **10h** |
| `iss` | `bellory-api` |

O front **pode** decodificar o payload (base64) para ler `exp` sem chamar o backend,
mas isso so serve para decidir *quando* validar/renovar — a validade real e sempre
confirmada pelo backend (assinatura, blacklist futura, etc.).

---

## Codigos de erro

Formato padrao de erro (`ErrorResponseDTO`):

```json
{
  "success": false,
  "message": "Token expirado",
  "errorCode": "TOKEN_EXPIRED",
  "timestamp": "12/05/2026 19:00:00",
  "path": "/api/v1/admin/auth/validate"
}
```

| `errorCode` | Quando | Acao no front |
|---|---|---|
| `INVALID_CREDENTIALS` | login com user/senha errados | mostrar erro no form de login |
| `ACCOUNT_DISABLED` | conta desativada | mostrar mensagem, manter no login |
| `MISSING_TOKEN` | header `Authorization` ausente/malformado | tratar como nao-logado -> `/login` |
| `TOKEN_EXPIRED` | token expirou | limpar sessao -> `/login` |
| `INVALID_TOKEN` | token corrompido/assinatura invalida | limpar sessao -> `/login` |
| `INVALID_TOKEN_TYPE` | token nao e `PLATFORM_ADMIN` | limpar sessao -> `/login` |
| `REFRESH_FAILED` | refresh de token ja invalido | limpar sessao -> `/login` |
| `VALIDATION_ERROR` / `USER_INFO_ERROR` | erro 500 inesperado | mostrar erro generico, permitir retry |

Regra geral no front: **qualquer 401 vindo de rota protegida -> limpar sessao e ir para `/login`.**

---

## Como corrigir o bug no front-end

> Os arquivos abaixo ficam no repositorio `Bellory-Admin-Front` (este workspace
> e a API). Os exemplos sao em React + axios; adaptar para a stack real.

### Causa raiz

O guard de rota hoje provavelmente faz algo como `if (localStorage.getItem('token')) -> renderiza`.
Isso so checa **existencia** do token, nao **validade**. Falta:

1. Um passo de **bootstrap** que valida o token no backend antes de liberar rotas protegidas.
2. Um **interceptor 401** que derruba a sessao quando o backend rejeita o token.

### Fluxo de bootstrap correto

```
App monta
  -> existe token salvo?
       nao  -> rota /login
       sim  -> estado = "validando..."  (renderiza um <Splash/> , NAO o dashboard)
               POST /api/v1/admin/auth/validate
                  200 -> grava user/roles/expiresAt no estado -> libera rotas protegidas
                  401 -> limpa token -> rota /login
                  erro de rede -> mostra "tente novamente" (nao desloga a toa)
```

O ponto critico: **nao renderizar `/dashboard` enquanto o estado for "validando"**.
Hoje o dashboard aparece e *so depois* as chamadas falham — invertendo a ordem o
problema some.

### Exemplo de service HTTP

```ts
// src/services/authService.ts
import axios from 'axios';

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL });

export interface AdminUser {
  id: number; username: string; nomeCompleto: string;
  email: string; role: string; ativo: boolean; dtCriacao: string;
}

export interface LoginResponse {
  success: boolean; message: string; token: string;
  user: AdminUser; expiresAt: string;
}

export interface ValidateResponse {
  valid: boolean; username: string; userId: number;
  userType: string; roles: string[]; expiresAt: string;
}

export const authService = {
  login: (username: string, password: string) =>
    api.post<LoginResponse>('/api/v1/admin/auth/login', { username, password })
       .then(r => r.data),

  validate: (token: string) =>
    api.post<ValidateResponse>('/api/v1/admin/auth/validate', null, {
      headers: { Authorization: `Bearer ${token}` },
    }).then(r => r.data),

  refresh: (token: string) =>
    api.post<{ newToken: string; expiresAt: string }>(
      '/api/v1/admin/auth/refresh', { token },
    ).then(r => r.data),

  me: (token: string) =>
    api.get<AdminUser>('/api/v1/admin/auth/me', {
      headers: { Authorization: `Bearer ${token}` },
    }).then(r => r.data),
};
```

### Exemplo de guard de rota

```tsx
// src/auth/AuthProvider.tsx
import { createContext, useContext, useEffect, useState } from 'react';
import { authService, AdminUser } from '../services/authService';

type Status = 'loading' | 'authenticated' | 'unauthenticated';

interface AuthCtx {
  status: Status;
  user: AdminUser | null;
  login: (u: string, p: string) => Promise<void>;
  logout: () => void;
}

const Ctx = createContext<AuthCtx>(null!);
export const useAuth = () => useContext(Ctx);

const TOKEN_KEY = 'bellory_admin_token';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<Status>('loading');
  const [user, setUser] = useState<AdminUser | null>(null);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) { setStatus('unauthenticated'); return; }

    authService.validate(token)
      .then(async () => {
        const me = await authService.me(token);
        setUser(me);
        setStatus('authenticated');
      })
      .catch((err) => {
        // 401/400 -> token nao serve. Erro de rede -> tambem desloga para nao travar a UI,
        // mas voce pode optar por um estado 'offline' e permitir retry.
        localStorage.removeItem(TOKEN_KEY);
        setStatus('unauthenticated');
      });
  }, []);

  const login = async (u: string, p: string) => {
    const res = await authService.login(u, p);
    localStorage.setItem(TOKEN_KEY, res.token);
    setUser(res.user);
    setStatus('authenticated');
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
    setStatus('unauthenticated');
  };

  return <Ctx.Provider value={{ status, user, login, logout }}>{children}</Ctx.Provider>;
}

// Componente que protege rotas:
export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  if (status === 'loading') return <SplashScreen />;       // <- NAO renderiza o dashboard ainda
  if (status === 'unauthenticated') return <Navigate to="/login" replace />;
  return <>{children}</>;
}
```

### Interceptor 401 global

Garante que, se o token expirar **durante** o uso (qualquer chamada de qualquer
tela), a sessao cai de forma limpa em vez de deixar telas quebradas:

```ts
// src/services/api.ts
import axios from 'axios';

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL });
const TOKEN_KEY = 'bellory_admin_token';

// injeta o token em toda request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// trata 401 globalmente
api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      // evita loop se ja estiver no login
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);
```

### (Opcional) Refresh proativo

Para nao derrubar o usuario no meio do trabalho quando o token estiver perto de
expirar:

- Guardar `expiresAt` (do login/validate) ou ler o claim `exp` do JWT.
- Quando faltar, por ex., < 5 min, chamar `POST /api/v1/admin/auth/refresh` e
  substituir o token salvo pelo `newToken`.
- Pode ser um `setInterval` no `AuthProvider` ou um check antes de cada request.
- Se o `refresh` falhar (401), cair no fluxo de logout normal.

---

## Checklist de implementacao

- [ ] `AuthProvider` com estados `loading | authenticated | unauthenticated`.
- [ ] No mount: se ha token, chama `POST /auth/validate` antes de liberar rotas.
- [ ] Enquanto `loading`, renderiza `<SplashScreen/>` — **nunca** o dashboard.
- [ ] `validate` falhou (401/400) -> `localStorage.removeItem` + redireciona `/login`.
- [ ] `RequireAuth` envolvendo todas as rotas protegidas (incl. `/dashboard`).
- [ ] Interceptor de request que injeta `Authorization: Bearer <token>`.
- [ ] Interceptor de response que, em `401`, limpa o token e vai para `/login` (sem loop).
- [ ] Tela de login usa `POST /auth/login`, trata `INVALID_CREDENTIALS` e `ACCOUNT_DISABLED`.
- [ ] Botao "Sair" so limpa o token local e redireciona (nao ha endpoint de logout).
- [ ] (Opcional) Refresh proativo via `POST /auth/refresh` antes do `expiresAt`.
- [ ] Padronizar a chave do `localStorage` (ex.: `bellory_admin_token`) e usar em um so lugar.
