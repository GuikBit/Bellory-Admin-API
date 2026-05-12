# Detalhe da Organização (Admin) — Contrato para o Front-end

> Tela `pages/organizacoes/OrganizacaoDetail.tsx`. Estrutura **v2**: o endpoint pesado
> (`GET /admin/organizacoes/{id}`) reorganizado em **blocos por domínio** + **3 endpoints
> "lazy"** (`uso-plano`, `cobrancas`, `trocas-plano`) + **1 mutation** (cancelar troca).
> **Breaking change** vs. v1: as `metricas` planas viraram blocos (`engajamento`, `onboarding`,
> `agendamentos`, `clientes`, `funcionarios`, `servicos`, `financeiro`). O bloco Payment API
> (`assinaturaAtiva`, `accessStatus`, `planoDetalhado`) e `configSistema` continuam, com
> `configSistema` **bem mais completo** agora.

---

## Endpoints

| Método | Path | Quando dispara | Resposta |
|---|---|---|---|
| `GET`    | `/api/v1/admin/organizacoes/{id}` | no mount da tela (sempre) | `OrganizacaoDetail` |
| `GET`    | `/api/v1/admin/organizacoes/{id}/uso-plano` | ao abrir a aba **Plano & Assinatura** | `PlanoUsoResponse` |
| `GET`    | `/api/v1/admin/organizacoes/{id}/cobrancas?page={p}&size={s}` | ao abrir a aba **Cobranças** — *só se* `paymentApiCustomerId != null` | `Page<Charge>` |
| `GET`    | `/api/v1/admin/organizacoes/{id}/trocas-plano?page={p}&size={s}` | ao abrir a aba **Trocas de Plano** — *só se* `paymentApiSubscriptionId != null` | `Page<PlanChange>` |
| `DELETE` | `/api/v1/admin/assinaturas/{subscriptionId}/trocas-plano/{changeId}` | botão "Cancelar" numa troca `PENDING`/`SCHEDULED` | `204 No Content` (ou a troca atualizada) |

**Auth:** mesma autenticação dos demais endpoints `/api/v1/admin/**`.
**`page`/`size`:** default do front = `page: 0`, `size: 20`. Resposta paginada no formato Spring.
⚠️ O cancelamento de troca usa o prefixo **`/admin/assinaturas`**, não `/admin/organizacoes`. O `subscriptionId` enviado é o `paymentApiSubscriptionId` da organização.

> **Não mudou nesta v2:** os 3 endpoints lazy (`uso-plano`, `cobrancas`, `trocas-plano`) e o `DELETE` de troca seguem com o mesmo contrato da v1. Veja as seções no fim do doc.

---

## Observações de comportamento

- **Fail-safe por bloco (Payment API):** `assinaturaAtiva`, `accessStatus`, `planoDetalhado` (em
  `OrganizacaoDetail`) e a resposta inteira de `uso-plano` dependem da Payment API externa. Se ela
  estiver indisponível, esses campos vêm **`null`** — **o resto do payload responde normalmente**.
  O front degrada só o card afetado.
- `cobrancas` e `trocas-plano` são chamados com `retry: false` no front.
- Campos podem vir `null` quando a fonte não tem dado — o front não assume presença de todos os campos opcionais.
- O endpoint pesado roda em todo carregamento da tela (~35 queries + até 3 chamadas à Payment API).
  **Não tem cache** ainda (candidato a otimização futura). `geradoEm` indica quando o snapshot foi montado.
- O **resumo de cobranças da aba Cobranças** ("X confirmadas / Y pendentes / Z atrasadas") ainda é
  calculado no front sobre a página atual. Para cobranças **avulsas/de agendamento** (entidade local
  `Cobranca`), o bloco `financeiro.cobrancasPorStatus` já dá o total global. Para cobranças de
  **assinatura** (Payment API), use `accessStatus.summary` (`overdueCharges`, `totalOverdueValue`,
  `oldestOverdueDays`, `creditBalance`).

---

## `OrganizacaoDetail` — `GET /admin/organizacoes/{id}`

### Topo / metadata

| Campo | Tipo | Descrição |
|---|---|---|
| `geradoEm` | string ISO | Momento em que o detalhe foi montado. |

### Identificação e cadastro

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | number | |
| `nomeFantasia` | string | |
| `razaoSocial` | string | |
| `cnpj` | string | |
| `inscricaoEstadual` | string \| null | |
| `slug` | string | |
| `publicoAlvo` | string \| null | |
| `logoUrl` / `bannerUrl` | string \| null | |
| `ativo` | boolean | |
| `dtCadastro` | string ISO | |
| `dtAtualizacao` | string ISO \| null | |

### Contato e responsável

| Campo | Tipo |
|---|---|
| `emailPrincipal` | string |
| `telefone1` / `telefone2` / `whatsapp` | string (pode vir vazio) |
| `responsavelNome` / `responsavelEmail` / `responsavelTelefone` | string |

### `endereco` (objeto \| null)

`{ logradouro, numero, complemento, bairro, cidade, uf, cep }` — todos `string | null`. Se tudo vazio, mostrar "Endereço não informado".

### `redesSociais` (objeto \| null)

`{ instagram, facebook, whatsapp, linkedin, youtube, site }` — todos `string | null`. Handle (`@fulano`) ou URL.

---

### `engajamento` (objeto) — **alimenta o card de saúde / aba Visão Geral**

| Campo | Tipo | Descrição |
|---|---|---|
| `diasDesdeCadastro` | number \| null | Idade da conta em dias. |
| `statusUso` | enum | `NOVO` (conta < 30 dias) \| `ATIVO` \| `EM_RISCO` (> 30 dias sem atividade) \| `INATIVO` (nunca teve atividade ou > 60 dias sem). |
| `healthScore` | number (0–100) | Pontuação composta: atividade recente (até 40), instância conectada (20), onboarding (até 25), faturamento no mês > 0 (15). Bom pra um gauge/badge. |
| `ultimaAtividadeEm` | string ISO \| null | `max(último agendamento criado, último cliente cadastrado)`. |
| `diasSemAtividade` | number \| null | Dias desde `ultimaAtividadeEm`. `null` se nunca houve atividade. |
| `ultimoAgendamentoCriadoEm` | string ISO \| null | Quando o último agendamento foi **criado** (sinal nº 1 de uso ativo). |
| `ultimoAgendamentoRealizadoEm` | string ISO \| null | Último agendamento `CONCLUIDO` com data ≤ hoje. |
| `proximoAgendamentoEm` | string ISO \| null | Próximo agendamento futuro (ignora cancelados/concluídos/no-show). |
| `ultimoClienteCadastradoEm` | string ISO \| null | |
| `agendamentosUltimos7Dias` | number | Agendamentos **criados** nos últimos 7 dias. |
| `agendamentosUltimos30Dias` | number | idem, 30 dias. |
| `clientesNovosUltimos30Dias` | number | Clientes cadastrados nos últimos 30 dias. |
| `instanciaConectada` | boolean | `true` se ≥ 1 instância está com status `CONNECTED`. |

### `onboarding` (objeto) — **checklist de ativação**

| Campo | Tipo | Descrição |
|---|---|---|
| `temFuncionario` | boolean | Org tem ≥ 1 funcionário. |
| `temServico` | boolean | ≥ 1 serviço. |
| `temCliente` | boolean | ≥ 1 cliente. |
| `temInstanciaConectada` | boolean | ≥ 1 instância `CONNECTED`. |
| `temAgendamento` | boolean | ≥ 1 agendamento (qualquer status, em qualquer época). |
| `configPreenchida` | boolean | Tem `ConfigSistema` vinculado. |
| `temAssinatura` | boolean | Tem registro de assinatura (vínculo Payment API). |
| `itensConcluidos` | number | Quantos dos itens acima são `true`. |
| `totalItens` | number | `7`. |
| `percentualConcluido` | number (0–100) | `round(itensConcluidos * 100 / totalItens)`. |

### `agendamentos` (objeto) — **aba Métricas / Agendamentos**

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Total histórico. |
| `noMes` | number | Criados no mês corrente. |
| `futuros` | number | Agendamentos com data ≥ hoje (não cancelados/concluídos/no-show). |
| `porStatus` | objeto `{ [status]: number }` | Contagem por status. Chaves possíveis: `PENDENTE`, `AGENDADO`, `CONFIRMADO`, `AGUARDANDO_CONFIRMACAO`, `EM_ESPERA`, `CONCLUIDO`, `CANCELADO`, `EM_ANDAMENTO`, `NAO_COMPARECEU`, `REAGENDADO`, `VENCIDA`, `PAGO`. Só vêm chaves com contagem > 0. |
| `taxaConclusao` | number (%) | `CONCLUIDO / total` (2 casas). |
| `taxaCancelamento` | number (%) | `CANCELADO / total`. |
| `taxaNoShow` | number (%) | `NAO_COMPARECEU / total`. |
| `evolucaoMensal` | array | Últimos 12 meses (por `dtCriacao`). Item: `{ mes: "YYYY-MM", total: number, concluidos: number, cancelados: number }`. |

### `clientes` (objeto)

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | |
| `ativos` / `inativos` | number | |
| `cadastroIncompleto` | number | Clientes com `isCadastroIncompleto = true`. |
| `novosNoMes` | number | |
| `novosUltimos30Dias` | number | |
| `mediaAgendamentosPorCliente` | number (decimal) | `totalAgendamentos / totalClientes`. |
| `evolucaoMensal` | array | Últimos 12 meses. Item: `{ mes: "YYYY-MM", novos: number, acumulado: number }`. |

### `funcionarios` (objeto)

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | |
| `ativos` / `inativos` | number | `ativos` = `situacao = 'Ativo'`. |
| `porSituacao` | objeto `{ [situacao]: number }` | Distribuição por valor de `situacao` (string livre; ex. `"Ativo"`, `"Inativo"`, `"Afastado"`...). |

### `servicos` (objeto)

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Serviços não deletados. |
| `ativos` / `inativos` | number | |
| `precoMedio` | number (R$) | Preço médio dos serviços ativos. |
| `porCategoria` | array | `[{ categoria: string, quantidade: number }]`, ordenado por quantidade desc. |

### `financeiro` (objeto)

> Faturamento = soma de `Pagamento.valor` com status `CONFIRMADO`. Cobranças = entidade local `Cobranca` (avulsas / de agendamento / multa / taxa).

| Campo | Tipo | Descrição |
|---|---|---|
| `faturamentoTotal` | number (R$) | Histórico confirmado. |
| `faturamentoMes` | number (R$) | Mês corrente. |
| `ticketMedio` | number (R$) | `faturamentoTotal / nº pagamentos confirmados`. |
| `valorPendente` | number (R$) | Soma das cobranças `PENDENTE` + `PARCIALMENTE_PAGO`. |
| `valorVencido` | number (R$) | Soma das cobranças `VENCIDA`. |
| `totalCobrancas` | number | Soma de `cobrancasPorStatus`. |
| `cobrancasPorStatus` | objeto `{ [status]: number }` | Chaves: `PENDENTE`, `PARCIALMENTE_PAGO`, `PAGO`, `VENCIDA`, `CANCELADA`, `ESTORNADA`. |
| `cobrancasPorTipo` | objeto `{ [tipo]: number }` | Chaves: `AGENDAMENTO`, `COMPRA`, `TAXA_ADICIONAL`, `MULTA`. |
| `pagamentosPorStatus` | objeto `{ [status]: number }` | Chaves: `PENDENTE`, `PROCESSANDO`, `CONFIRMADO`, `RECUSADO`, `CANCELADO`, `ESTORNADO`. |
| `evolucaoMensal` | array | Faturamento confirmado dos últimos 12 meses. Item: `{ mes: "YYYY-MM", valor: number, quantidade: number }`. |

---

### Plano / assinatura (Payment API — fail-safe; **shape igual à v1**)

| Campo | Tipo | Descrição |
|---|---|---|
| `paymentApiCustomerId` | number \| null | Se `null`, abas Cobranças e (sem `paymentApiSubscriptionId`) Trocas mostram "sem vínculo Payment API" e **não chamam os endpoints**. |
| `paymentApiSubscriptionId` | number \| null | ID da assinatura na Payment API. Usado no `DELETE` de troca. |
| `assinaturaAtiva` | `Subscription` \| null | Assinatura corrente (objeto completo, igual à v1: `id, customerId, companyId, planId, planName, asaasId, billingType, effectivePrice, cycle, currentPeriodStart, currentPeriodEnd, nextDueDate, status, trialEndDate, trialDaysAtCreation, inTrial, couponCode, couponDiscountAmount, couponUsesRemaining, createdAt, updatedAt`). `null` se sem assinatura ou Payment API fora. |
| `accessStatus` | `AccessStatus` \| null | `{ customerId, customerName, allowed, reasons[], customBlockMessage, summary: { activeSubscriptions, suspendedSubscriptions, overdueCharges, totalOverdueValue, oldestOverdueDays, creditBalance }, checkedAt }`. |
| `planoDetalhado` | `Plan` \| null | Plano completo com `limits[]` e `features[]` (mesmo shape da v1). |

> O legado `plano` / `limites` / `limitesPersonalizados` da v1 **foi removido** (planos migraram para a Payment API). Use `planoDetalhado` + a aba `uso-plano`.

### `configSistema` (objeto \| null) — **agora completo**

`null` se a org não tem `ConfigSistema`. Booleans podem vir `null` se o sub-bloco não foi inicializado.

```jsonc
"configSistema": {
  // Módulos
  "usaEcommerce": true, "usaGestaoProdutos": true, "usaPlanosParaClientes": false,
  "disparaNotificacoesPush": true, "urlAcesso": "https://studiox.bellory.app",

  // Agendamento
  "toleranciaAgendamento": 15, "minDiasAgendamento": 1, "maxDiasAgendamento": 90,
  "cancelamentoCliente": false, "mostrarAgendamentoCancelado": false, "tempoCancelamentoCliente": null,
  "aprovarAgendamento": true, "aprovarAgendamentoAgente": false,
  "ocultarFimSemana": false, "ocultarDomingo": false,
  "cobrarSinal": false, "porcentSinal": null, "cobrarSinalAgente": true, "porcentSinalAgente": 50,
  "modoVizualizacao": "calendar",

  // Fila de espera
  "usarFilaEspera": false, "filaMaxCascata": 5, "filaTimeoutMinutos": 30, "filaAntecedenciaHoras": 3,

  // Serviço
  "mostrarValorAgendamento": false, "unicoServicoAgendamento": false, "mostrarAvaliacao": false,

  // Cliente
  "precisaCadastroAgendar": false, "programaFidelidade": false, "valorGastoUmPonto": null,

  // Colaborador
  "selecionarColaboradorAgendamento": false, "mostrarNotasComentarioColaborador": false, "comissaoPadrao": false,

  // Notificação
  "enviarConfirmacaoWhatsapp": false, "enviarLembreteWhatsapp": false,
  "enviarLembreteSms": false, "enviarLembreteEmail": false,
  "enviarConfirmacaoForaHorario": false,
  "tempoParaConfirmacao": 24, "tempoLembretePosConfirmacao": 2,
  "mensagemTemplateConfirmacao": "Olá *{{nome_cliente}}*! ...",
  "mensagemTemplateLembrete": "Olá *{{nome_cliente}}*! ..."
}
```

> ⚠️ Renomeei para o nome real do campo no back: `modoVizualizacao` (não `modoVisualizacao`). Os templates de notificação são strings longas com placeholders `{{...}}` — renderize num `<textarea readonly>` ou colapsável.

### `instancias` (array) — **aba Instâncias**

`[{ id: number, instanceName: string, instanceId: string, status: string, ativo: boolean }]`. O front trata `status === "CONNECTED"` como conectada. (O `engajamento.instanciaConectada` já resume isso.)

---

## Exemplo de resposta — `GET /admin/organizacoes/{id}` (resumido)

```jsonc
{
  "geradoEm": "2026-05-12T14:03:55",
  "id": 12, "nomeFantasia": "Studio X", "razaoSocial": "Studio X Beleza Ltda", "cnpj": "12345678000190",
  "inscricaoEstadual": null, "slug": "studio-x", "publicoAlvo": "Mulheres 25-45",
  "logoUrl": "https://cdn.bellory.app/orgs/12/logo.png", "bannerUrl": null,
  "ativo": true, "dtCadastro": "2025-02-10T09:00:00", "dtAtualizacao": "2026-05-01T14:30:00",
  "emailPrincipal": "contato@studiox.com.br", "telefone1": "11988887777", "telefone2": "", "whatsapp": "11988887777",
  "responsavelNome": "Maria Souza", "responsavelEmail": "maria@studiox.com.br", "responsavelTelefone": "11999990000",
  "endereco": { "logradouro": "Rua das Flores", "numero": "100", "complemento": "Sala 2", "bairro": "Centro", "cidade": "São Paulo", "uf": "SP", "cep": "01000000" },
  "redesSociais": { "instagram": "studiox", "facebook": null, "whatsapp": "11988887777", "linkedin": null, "youtube": null, "site": "https://studiox.com.br" },

  "engajamento": {
    "diasDesdeCadastro": 456, "statusUso": "ATIVO", "healthScore": 88,
    "ultimaAtividadeEm": "2026-05-12T11:20:00", "diasSemAtividade": 0,
    "ultimoAgendamentoCriadoEm": "2026-05-12T11:20:00", "ultimoAgendamentoRealizadoEm": "2026-05-11T16:00:00",
    "proximoAgendamentoEm": "2026-05-13T09:30:00", "ultimoClienteCadastradoEm": "2026-05-10T18:42:00",
    "agendamentosUltimos7Dias": 38, "agendamentosUltimos30Dias": 142, "clientesNovosUltimos30Dias": 12,
    "instanciaConectada": true
  },
  "onboarding": {
    "temFuncionario": true, "temServico": true, "temCliente": true, "temInstanciaConectada": true,
    "temAgendamento": true, "configPreenchida": true, "temAssinatura": true,
    "itensConcluidos": 7, "totalItens": 7, "percentualConcluido": 100
  },
  "agendamentos": {
    "total": 1820, "noMes": 142, "futuros": 31,
    "porStatus": { "CONCLUIDO": 1500, "CANCELADO": 180, "AGENDADO": 95, "PENDENTE": 30, "NAO_COMPARECEU": 15 },
    "taxaConclusao": 82.42, "taxaCancelamento": 9.89, "taxaNoShow": 0.82,
    "evolucaoMensal": [ { "mes": "2025-06", "total": 120, "concluidos": 100, "cancelados": 12 } ]
  },
  "clientes": {
    "total": 940, "ativos": 880, "inativos": 60, "cadastroIncompleto": 22,
    "novosNoMes": 12, "novosUltimos30Dias": 12, "mediaAgendamentosPorCliente": 1.94,
    "evolucaoMensal": [ { "mes": "2025-06", "novos": 30, "acumulado": 30 } ]
  },
  "funcionarios": { "total": 8, "ativos": 7, "inativos": 1, "porSituacao": { "Ativo": 7, "Inativo": 1 } },
  "servicos": {
    "total": 32, "ativos": 30, "inativos": 2, "precoMedio": 87.40,
    "porCategoria": [ { "categoria": "Cabelo", "quantidade": 14 }, { "categoria": "Unhas", "quantidade": 10 } ]
  },
  "financeiro": {
    "faturamentoTotal": 41200.00, "faturamentoMes": 3800.00, "ticketMedio": 119.30,
    "valorPendente": 240.00, "valorVencido": 90.00, "totalCobrancas": 56,
    "cobrancasPorStatus": { "PAGO": 52, "PENDENTE": 3, "VENCIDA": 1 },
    "cobrancasPorTipo": { "AGENDAMENTO": 54, "COMPRA": 2 },
    "pagamentosPorStatus": { "CONFIRMADO": 50, "RECUSADO": 4, "PENDENTE": 2 },
    "evolucaoMensal": [ { "mes": "2025-06", "valor": 3200.00, "quantidade": 28 } ]
  },

  "paymentApiCustomerId": 301, "paymentApiSubscriptionId": 410,
  "assinaturaAtiva": {
    "id": 410, "customerId": 301, "planId": 3, "planName": "Plus", "billingType": "PIX", "effectivePrice": 89.91, "cycle": "MONTHLY",
    "currentPeriodStart": "2026-05-01", "currentPeriodEnd": "2026-05-31", "nextDueDate": "2026-06-01", "status": "ACTIVE",
    "inTrial": false, "couponCode": "BEMVINDO10", "couponDiscountAmount": 9.99, "couponUsesRemaining": 2,
    "createdAt": "2025-02-10T09:05:00", "updatedAt": "2026-05-01T00:00:00"
  },
  "accessStatus": {
    "customerId": 301, "customerName": "Studio X", "allowed": true, "reasons": [], "customBlockMessage": null,
    "summary": { "activeSubscriptions": 1, "suspendedSubscriptions": 0, "overdueCharges": 0, "totalOverdueValue": 0.00, "oldestOverdueDays": 0, "creditBalance": 0.00 },
    "checkedAt": "2026-05-12T14:03:55"
  },
  "planoDetalhado": {
    "id": 3, "name": "Plus", "codigo": "PLUS", "precoMensal": 99.90, "precoAnual": 999.00, "precoSemestral": 539.40,
    "active": true, "isFree": false, "trialDays": 14, "tierOrder": 3,
    "limits":   [ { "key": "agendamento", "label": "Agendamentos/mês", "type": "NUMBER", "value": 500 }, { "key": "cliente", "label": "Clientes", "type": "UNLIMITED" } ],
    "features": [ { "key": "whatsapp", "label": "WhatsApp", "type": "BOOLEAN", "enabled": true }, { "key": "api", "label": "API", "type": "BOOLEAN", "enabled": false } ],
    "createdAt": "2024-01-01T00:00:00", "updatedAt": null
  },

  "configSistema": {
    "usaEcommerce": true, "usaGestaoProdutos": true, "usaPlanosParaClientes": false, "disparaNotificacoesPush": true, "urlAcesso": "https://studiox.bellory.app",
    "toleranciaAgendamento": 15, "minDiasAgendamento": 1, "maxDiasAgendamento": 90,
    "cancelamentoCliente": false, "mostrarAgendamentoCancelado": false, "tempoCancelamentoCliente": null,
    "aprovarAgendamento": true, "aprovarAgendamentoAgente": false, "ocultarFimSemana": false, "ocultarDomingo": false,
    "cobrarSinal": false, "porcentSinal": null, "cobrarSinalAgente": true, "porcentSinalAgente": 50, "modoVizualizacao": "calendar",
    "usarFilaEspera": false, "filaMaxCascata": 5, "filaTimeoutMinutos": 30, "filaAntecedenciaHoras": 3,
    "mostrarValorAgendamento": false, "unicoServicoAgendamento": false, "mostrarAvaliacao": false,
    "precisaCadastroAgendar": false, "programaFidelidade": false, "valorGastoUmPonto": null,
    "selecionarColaboradorAgendamento": false, "mostrarNotasComentarioColaborador": false, "comissaoPadrao": false,
    "enviarConfirmacaoWhatsapp": false, "enviarLembreteWhatsapp": false, "enviarLembreteSms": false, "enviarLembreteEmail": false,
    "enviarConfirmacaoForaHorario": false, "tempoParaConfirmacao": 24, "tempoLembretePosConfirmacao": 2,
    "mensagemTemplateConfirmacao": "Olá *{{nome_cliente}}*! ...", "mensagemTemplateLembrete": "Olá *{{nome_cliente}}*! ..."
  },

  "instancias": [ { "id": 5, "instanceName": "Atendimento", "instanceId": "studiox-wpp-1", "status": "CONNECTED", "ativo": true } ]
}
```

---

## `PlanoUsoResponse` — `GET /admin/organizacoes/{id}/uso-plano` *(inalterado)*

Limites e features do plano cruzados com o uso real. Carregado só ao abrir a aba Plano.

| Campo | Tipo | Descrição |
|---|---|---|
| `organizacaoId` | number | |
| `planId` / `planName` | number / string \| null | |
| `limits` | `PlanoUsoItem[]` (opcional) | Recursos numéricos/ilimitados. |
| `features` | `PlanoUsoItem[]` (opcional) | Flags de funcionalidade. |

**`PlanoUsoItem`:** `{ key, label, type: 'NUMBER'|'BOOLEAN'|'UNLIMITED', value?: number|null, unlimited: boolean, enabled?: boolean|null, currentUsage?: number|null, remaining?: number|null, percentUsed?: number|null }`.
Fallback do front: `limits/features` vazios → usa `planoDetalhado.limits/features`.

---

## `Page<Charge>` — `GET /admin/organizacoes/{id}/cobrancas?page&size` *(inalterado)*

`{ content: Charge[], totalElements, totalPages, number, size }`. Só chamado se `paymentApiCustomerId != null`.

**`Charge`:** `{ id, customerId, subscriptionId?, billingType: 'PIX'|'BOLETO'|'CREDIT_CARD'|'DEBIT_CARD'|'UNDEFINED', value, dueDate, status: 'PENDING'|'CONFIRMED'|'RECEIVED'|'OVERDUE'|'CANCELED'|'REFUNDED', origin: 'API'|'RECURRING'|'PLAN_CHANGE', pixQrcode?, pixCopyPaste?, boletoUrl?, invoiceUrl?, couponCode?, discountAmount?, originalValue?, createdAt }`.

---

## `Page<PlanChange>` — `GET /admin/organizacoes/{id}/trocas-plano?page&size` *(inalterado)*

`{ content: PlanChange[], totalElements, totalPages, number, size }`. Só chamado se `paymentApiSubscriptionId != null`.

**`PlanChange`:** `{ id, subscriptionId, previousPlanId, previousPlanName, requestedPlanId, requestedPlanName, changeType: 'UPGRADE'|'DOWNGRADE'|'SIDEGRADE', policy: 'IMMEDIATE_PRORATA'|'END_OF_CYCLE'|'IMMEDIATE_NO_PRORATA', deltaAmount?, prorationCredit?, prorationCharge?, status: 'COMPLETED'|'SCHEDULED'|'PENDING'|'FAILED'|'CANCELED', chargeId?, scheduledFor?, effectiveAt?, requestedBy, requestedAt, failureReason? }`.

Botão "Cancelar" só para `status ∈ { PENDING, SCHEDULED }`.

---

## `DELETE /admin/assinaturas/{subscriptionId}/trocas-plano/{changeId}` *(inalterado)*

Cancela uma troca pendente/agendada. `subscriptionId` = `OrganizacaoDetail.paymentApiSubscriptionId`. Esperado: `204 No Content` (o front re-busca a lista de trocas). Se não cancelável → 4xx com mensagem.

---

## Sugestões de layout (não obrigatório)

- **Header / card de saúde:** `engajamento.healthScore` (gauge) + badge `statusUso` + "última atividade há X dias" (`diasSemAtividade`) + `dtCadastro` / `diasDesdeCadastro`.
- **Checklist de onboarding:** `onboarding` (7 itens com check, barra `percentualConcluido`) — ótimo pra contas novas/em risco.
- **Aba Visão Geral:** cards rápidos — `agendamentos.noMes`, `engajamento.agendamentosUltimos30Dias`, `clientes.novosUltimos30Dias`, `financeiro.faturamentoMes`, `agendamentos.futuros`, `engajamento.proximoAgendamentoEm`.
- **Aba Métricas:** gráfico de linha `agendamentos.evolucaoMensal` (total/concluídos/cancelados), donut `agendamentos.porStatus`, taxas (`taxaConclusao`/`Cancelamento`/`NoShow`); linha `clientes.evolucaoMensal`; linha `financeiro.evolucaoMensal`; barras `servicos.porCategoria`.
- **Aba Financeiro:** `financeiro` (cards de faturamento, ticket médio, valor pendente/vencido; donuts `cobrancasPorStatus` / `cobrancasPorTipo` / `pagamentosPorStatus`).
- **Aba Configurações:** `configSistema` completo (todos os grupos agora vêm preenchidos).
- **Aba Instâncias:** `instancias[]`.
- **Aba Plano & Assinatura:** `assinaturaAtiva` + `accessStatus` + `planoDetalhado`, e lazy `uso-plano` / `cobrancas` / `trocas-plano`.
- **Footer:** "Atualizado em {geradoEm}".
