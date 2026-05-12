# Dashboard Administrativo — Contrato para o Front-end

> Endpoint consolidado do painel admin. Estrutura **v2**: payload quebrado em
> blocos por domínio. Substitui o formato antigo (flat). **Breaking change** —
> o front precisa ser remodelado.

---

## Endpoints

| Método | Path | Descrição |
|---|---|---|
| `GET`  | `/api/v1/admin/dashboard`         | Retorna o dashboard. Servido de cache Redis (TTL ~10 min). |
| `POST` | `/api/v1/admin/dashboard/refresh` | Força o recálculo, ignorando e sobrescrevendo o cache. Mesma resposta do GET. |

**Auth:** mesma autenticação dos demais endpoints `/api/v1/admin/**` (sessão/JWT admin). Nenhum parâmetro de query/body.

**Observações de comportamento:**

- O 1º acesso após o deploy (cache frio) é mais lento (~30 queries + chamada à Payment API). Os seguintes vêm do cache.
- O bloco `planos` depende da Payment API externa. Se ela estiver indisponível, o bloco volta com `disponivel: false` e os campos dependentes nulos — **o restante do dashboard responde normalmente**. O front deve tratar esse caso (ex.: skeleton/“indisponível” só no card de planos).
- Campos podem vir `null` quando a fonte não tem dado. O front não deve assumir presença de todos os campos.
- `geradoEm` indica quando o snapshot foi calculado (use para mostrar “atualizado há X min” e como gatilho do botão de refresh).

---

## Estrutura geral

```jsonc
{
  "geradoEm": "2026-05-12T14:03:55",
  "organizacoes":   { ... },
  "agendamentos":   { ... },
  "clientes":       { ... },
  "funcionarios":   { ... },
  "servicos":       { ... },
  "instancias":     { ... },
  "financeiro":     { ... },
  "planos":         { ... },
  "leads":          { ... },
  "topOrganizacoes":{ ... },
  "localizacoes":   [ ... ]
}
```

Tipos: inteiros são `number` (Long no back); valores monetários são `number` decimal (BigDecimal serializado, ex. `1234.50`); percentuais são `number` com 2 casas representando **porcentagem** (ex. `12.34` = 12,34%); datas/horas são string ISO `yyyy-MM-dd'T'HH:mm:ss` (sem timezone — horário do servidor).

---

## Bloco `organizacoes`

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Total de organizações cadastradas. |
| `ativas` | number | Organizações com `ativo = true`. |
| `inativas` | number | Organizações com `ativo = false`. |
| `novasHoje` | number | Cadastradas desde 00:00 de hoje. |
| `novasUltimos7Dias` | number | Cadastradas nos últimos 7 dias. |
| `novasMesAtual` | number | Cadastradas no mês corrente. |
| `novasMesAnterior` | number | Cadastradas no mês anterior. |
| `crescimentoMensalPercentual` | number (%) | Variação `novasMesAtual` vs `novasMesAnterior`. `100` quando o mês anterior foi 0 e o atual > 0. |
| `ativasSemAgendamentos` | number | Orgs ativas que ainda não têm **nenhum** agendamento (onboarding incompleto). |
| `ativasSemInstancia` | number | Orgs ativas sem nenhuma instância de WhatsApp ativa/não deletada. |
| `taxaAtivacao` | number (%) | % de orgs ativas com ≥ 1 agendamento. |
| `adocaoFeatures` | objeto | Quantas orgs ativas usam cada feature. |
| `adocaoFeatures.usaEcommerce` | number | |
| `adocaoFeatures.usaGestaoProdutos` | number | |
| `adocaoFeatures.usaPlanosParaClientes` | number | |
| `adocaoFeatures.disparaNotificacoesPush` | number | |
| `porEstado` | array | Distribuição de orgs ativas por UF, ordenado por quantidade desc. |
| `porEstado[].chave` | string | UF (ex. `"SP"`). |
| `porEstado[].quantidade` | number | |

---

## Bloco `agendamentos`

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Total de agendamentos (soma de `porStatus`). |
| `hoje` | number | Criados desde 00:00 de hoje (por `dtCriacao`). |
| `ultimos7Dias` | number | Criados nos últimos 7 dias. |
| `mesAtual` | number | Criados no mês corrente. |
| `mesAnterior` | number | Criados no mês anterior. |
| `crescimentoMensalPercentual` | number (%) | `mesAtual` vs `mesAnterior`. |
| `porStatus` | objeto `{ [status]: number }` | Contagem por status. Chaves possíveis: `PENDENTE`, `AGENDADO`, `CONFIRMADO`, `AGUARDANDO_CONFIRMACAO`, `EM_ESPERA`, `CONCLUIDO`, `CANCELADO`, `EM_ANDAMENTO`, `NAO_COMPARECEU`, `REAGENDADO`, `VENCIDA`, `PAGO`. Só vêm chaves com contagem > 0. |
| `taxaConclusao` | number (%) | `CONCLUIDO / total`. |
| `taxaCancelamento` | number (%) | `CANCELADO / total`. |
| `taxaNoShow` | number (%) | `NAO_COMPARECEU / total`. |
| `evolucaoMensal` | array `SerieMensal` | Últimos 12 meses (por `dtCriacao`). Ver tipo abaixo. |

**`SerieMensal`:**

| Campo | Tipo | Descrição |
|---|---|---|
| `mes` | string `YYYY-MM` | |
| `quantidade` | number | Novos no mês. |
| `acumulado` | number | Soma acumulada dentro da janela retornada (não é o total histórico). |

---

## Bloco `clientes`

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Total de clientes (todas as orgs). |
| `ativos` | number | `ativo = true`. |
| `inativos` | number | `total - ativos`. |
| `cadastroIncompleto` | number | Clientes com `isCadastroIncompleto = true`. |
| `novosHoje` / `novosUltimos7Dias` / `novosMesAtual` | number | Por `dtCriacao`. |
| `mediaPorOrganizacao` | number (decimal) | `total / nº de organizações`, 2 casas. |
| `evolucaoMensal` | array `SerieMensal` | Últimos 12 meses. |

---

## Bloco `funcionarios`

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | |
| `ativos` | number | `situacao = 'Ativo'`. |
| `inativos` | number | `total - ativos`. |
| `mediaPorOrganizacao` | number (decimal) | |

---

## Bloco `servicos`

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Serviços não deletados. |
| `ativos` | number | `ativo = true` e não deletados. |
| `inativos` | number | `total - ativos`. |
| `precoMedio` | number (decimal) | Preço médio dos serviços ativos. |
| `mediaPorOrganizacao` | number (decimal) | |

---

## Bloco `instancias` (WhatsApp)

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Instâncias não deletadas. |
| `conectadas` | number | `ativo = true`. |
| `desconectadas` | number | `total - conectadas`. |
| `porStatus` | objeto `{ [status]: number }` | Contagem pelo status real da instância. Chaves possíveis: `CONNECTED`, `CONNECTING`, `DISCONNECTED`, `QRCODE`, `ERROR`, `OPEN`. Pode vir `DESCONHECIDO` quando o status está nulo no banco. Só vêm chaves com contagem > 0. |
| `organizacoesSemInstancia` | number | Mesmo valor de `organizacoes.ativasSemInstancia`. |

---

## Bloco `financeiro`

> Faturamento = soma de `Pagamento.valor` com `status = CONFIRMADO`. Cobranças = entidade `Cobranca` (pode estar em vários estados).

| Campo | Tipo | Descrição |
|---|---|---|
| `faturamentoTotal` | number (R$) | Todo o histórico confirmado. |
| `faturamentoHoje` | number (R$) | Confirmado desde 00:00 de hoje (por `dtPagamento`). |
| `faturamentoMesAtual` | number (R$) | Confirmado no mês corrente. |
| `faturamentoMesAnterior` | number (R$) | Confirmado no mês anterior. |
| `crescimentoMensalPercentual` | number (%) | `mesAtual` vs `mesAnterior`. |
| `ticketMedio` | number (R$) | `faturamentoTotal / nº de pagamentos confirmados`. |
| `valorPendente` | number (R$) | Soma das cobranças `PENDENTE` + `PARCIALMENTE_PAGO`. |
| `valorVencido` | number (R$) | Soma das cobranças `VENCIDA`. |
| `taxaInadimplencia` | number (%) | Cobranças vencidas / total de cobranças. |
| `totalCobrancas` | number | Soma de `cobrancasPorStatus`. |
| `cobrancasPorStatus` | objeto `{ [status]: number }` | Chaves: `PENDENTE`, `PARCIALMENTE_PAGO`, `PAGO`, `VENCIDA`, `CANCELADA`, `ESTORNADA`. |
| `cobrancasPorTipo` | objeto `{ [tipo]: number }` | Chaves: `AGENDAMENTO`, `COMPRA`, `TAXA_ADICIONAL`, `MULTA`. |
| `totalPagamentos` | number | Soma de `pagamentosPorStatus`. |
| `pagamentosPorStatus` | objeto `{ [status]: number }` | Chaves: `PENDENTE`, `PROCESSANDO`, `CONFIRMADO`, `RECUSADO`, `CANCELADO`, `ESTORNADO`. |
| `evolucaoMensal` | array `SerieMensalValor` | Faturamento confirmado dos últimos 12 meses. |

**`SerieMensalValor`:**

| Campo | Tipo | Descrição |
|---|---|---|
| `mes` | string `YYYY-MM` | |
| `valor` | number (R$) | Faturamento confirmado no mês. |
| `quantidade` | number | Nº de pagamentos confirmados no mês. |

---

## Bloco `planos` (Payment API)

> Dados de assinatura/plano vêm da Payment API externa. Em caso de indisponibilidade, `disponivel: false` e os campos dependentes vêm nulos (mas `totalAssinaturas` e `organizacoesSemAssinatura`, que são locais, ainda vêm).

| Campo | Tipo | Descrição |
|---|---|---|
| `disponivel` | boolean | `false` se a Payment API falhou — front deve degradar só este card. |
| `totalAssinaturas` | number | Assinaturas locais vinculadas à Payment API. |
| `organizacoesSemAssinatura` | number | `totalOrganizacoes - totalAssinaturas` (estimativa). |
| `assinaturasAtivas` | number \| null | Assinaturas com status `ACTIVE`. |
| `assinaturasEmTrial` | number \| null | `inTrial = true` ou status `TRIALING`. |
| `assinaturasCanceladas` | number \| null | Status `CANCELED`. |
| `mrrEstimado` | number (R$) \| null | Soma do `effectivePrice` das assinaturas `ACTIVE` com ciclo `MONTHLY` (aprox.). |
| `distribuicao` | array `PlanoDistribuicao` | Quantas assinaturas ativas/trial por plano, ordenado por `tierOrder` e depois quantidade desc. |

**`PlanoDistribuicao`:**

| Campo | Tipo | Descrição |
|---|---|---|
| `planId` | number | ID do plano na Payment API. |
| `codigo` | string \| null | Código do plano (ex. `GRATUITO`, `BASICO`, `PLUS`, `PREMIUM`). |
| `nome` | string \| null | Nome de exibição. |
| `gratuito` | boolean \| null | Plano free. |
| `precoMensal` | number (R$) \| null | Preço mensal de tabela. |
| `quantidade` | number | Assinaturas ativas/trial nesse plano. |

---

## Bloco `leads` (funil comercial)

| Campo | Tipo | Descrição |
|---|---|---|
| `total` | number | Leads não deletados. |
| `novosHoje` / `novosUltimos7Dias` / `novosMesAtual` | number | Por `dtCriacao`. |
| `emStatusFinal` | number | Leads em colunas do kanban marcadas como “status final” (ganho/perdido). |
| `porStatus` | objeto `{ [nomeDaColuna]: number }` | Distribuição pelas colunas do kanban (chave = nome da coluna, configurável). |
| `porTipoNegocio` | objeto `{ [tipo]: number }` | Chaves: `SALAO`, `BARBEARIA`, `CLINICA`, `NAIL`, `SPA`, `OUTRO`. |
| `porPrioridade` | objeto `{ [prioridade]: number }` | Chaves: `BAIXA`, `MEDIA`, `ALTA`. |
| `porOrigem` | objeto `{ [origem]: number }` | Origem livre (string). Leads sem origem caem em `"(sem valor)"`. |
| `valorEstimadoPipeline` | number (R$) | Soma de `valorEstimado` de todos os leads não deletados. |

---

## Bloco `topOrganizacoes`

Cada lista tem **no máximo 10 itens**, já ordenada (desc pela métrica).

| Campo | Tipo | Descrição |
|---|---|---|
| `porFaturamento` | array `OrgRanking` | Maiores faturamentos confirmados. `valor` preenchido, `quantidade` null. |
| `porAgendamentos` | array `OrgRanking` | Mais agendamentos. `quantidade` preenchido, `valor` null. |
| `porClientes` | array `OrgRanking` | Mais clientes. `quantidade` preenchido, `valor` null. |

**`OrgRanking`:**

| Campo | Tipo | Descrição |
|---|---|---|
| `organizacaoId` | number | |
| `nomeFantasia` | string | |
| `valor` | number (R$) \| null | Preenchido só em `porFaturamento`. |
| `quantidade` | number \| null | Preenchido em `porAgendamentos` / `porClientes`. |

---

## `localizacoes` (mapa)

Array de pontos agregados por cidade/UF/coordenada (só orgs ativas com lat/long preenchidos).

| Campo | Tipo | Descrição |
|---|---|---|
| `cidade` | string \| null | |
| `estado` | string \| null | UF em maiúsculas. |
| `latitude` | number | |
| `longitude` | number | |
| `quantidade` | number | Quantas orgs nessa cidade/coordenada. |

---

## Exemplo de resposta (resumido)

```json
{
  "geradoEm": "2026-05-12T14:03:55",
  "organizacoes": {
    "total": 128, "ativas": 119, "inativas": 9,
    "novasHoje": 1, "novasUltimos7Dias": 6, "novasMesAtual": 14, "novasMesAnterior": 11,
    "crescimentoMensalPercentual": 27.27,
    "ativasSemAgendamentos": 8, "ativasSemInstancia": 15, "taxaAtivacao": 93.28,
    "adocaoFeatures": { "usaEcommerce": 90, "usaGestaoProdutos": 88, "usaPlanosParaClientes": 21, "disparaNotificacoesPush": 110 },
    "porEstado": [ { "chave": "SP", "quantidade": 41 }, { "chave": "MG", "quantidade": 22 } ]
  },
  "agendamentos": {
    "total": 18432, "hoje": 73, "ultimos7Dias": 512, "mesAtual": 2104, "mesAnterior": 1980,
    "crescimentoMensalPercentual": 6.26,
    "porStatus": { "CONCLUIDO": 12001, "CANCELADO": 1502, "AGENDADO": 3100, "PENDENTE": 1200, "NAO_COMPARECEU": 629 },
    "taxaConclusao": 65.11, "taxaCancelamento": 8.15, "taxaNoShow": 3.41,
    "evolucaoMensal": [ { "mes": "2025-06", "quantidade": 1400, "acumulado": 1400 }, { "mes": "2025-07", "quantidade": 1520, "acumulado": 2920 } ]
  },
  "clientes": {
    "total": 9421, "ativos": 8800, "inativos": 621, "cadastroIncompleto": 312,
    "novosHoje": 14, "novosUltimos7Dias": 98, "novosMesAtual": 410, "mediaPorOrganizacao": 73.6,
    "evolucaoMensal": [ { "mes": "2025-06", "quantidade": 300, "acumulado": 300 } ]
  },
  "funcionarios": { "total": 642, "ativos": 590, "inativos": 52, "mediaPorOrganizacao": 5.02 },
  "servicos": { "total": 3120, "ativos": 2890, "inativos": 230, "precoMedio": 87.40, "mediaPorOrganizacao": 24.38 },
  "instancias": {
    "total": 130, "conectadas": 104, "desconectadas": 26,
    "porStatus": { "CONNECTED": 104, "DISCONNECTED": 18, "QRCODE": 5, "ERROR": 3 },
    "organizacoesSemInstancia": 15
  },
  "financeiro": {
    "faturamentoTotal": 482310.50, "faturamentoHoje": 1240.00, "faturamentoMesAtual": 41200.00, "faturamentoMesAnterior": 38800.00,
    "crescimentoMensalPercentual": 6.19, "ticketMedio": 119.30,
    "valorPendente": 8200.00, "valorVencido": 3100.00, "taxaInadimplencia": 4.12, "totalCobrancas": 6021,
    "cobrancasPorStatus": { "PAGO": 5400, "PENDENTE": 380, "VENCIDA": 200, "CANCELADA": 41 },
    "cobrancasPorTipo": { "AGENDAMENTO": 5800, "COMPRA": 221 },
    "totalPagamentos": 5210, "pagamentosPorStatus": { "CONFIRMADO": 4980, "RECUSADO": 120, "PENDENTE": 110 },
    "evolucaoMensal": [ { "mes": "2025-06", "valor": 36000.00, "quantidade": 410 } ]
  },
  "planos": {
    "disponivel": true, "totalAssinaturas": 119, "organizacoesSemAssinatura": 9,
    "assinaturasAtivas": 95, "assinaturasEmTrial": 12, "assinaturasCanceladas": 12, "mrrEstimado": 9870.00,
    "distribuicao": [
      { "planId": 1, "codigo": "GRATUITO", "nome": "Gratuito", "gratuito": true, "precoMensal": 0.00, "quantidade": 40 },
      { "planId": 2, "codigo": "BASICO",  "nome": "Básico",   "gratuito": false, "precoMensal": 49.90, "quantidade": 35 },
      { "planId": 3, "codigo": "PLUS",    "nome": "Plus",     "gratuito": false, "precoMensal": 99.90, "quantidade": 22 },
      { "planId": 4, "codigo": "PREMIUM", "nome": "Premium",  "gratuito": false, "precoMensal": 199.90, "quantidade": 10 }
    ]
  },
  "leads": {
    "total": 540, "novosHoje": 3, "novosUltimos7Dias": 28, "novosMesAtual": 96, "emStatusFinal": 210,
    "porStatus": { "Novo": 120, "Em contato": 90, "Proposta": 60, "Ganho": 150, "Perdido": 120 },
    "porTipoNegocio": { "SALAO": 200, "BARBEARIA": 150, "CLINICA": 90, "NAIL": 40, "SPA": 30, "OUTRO": 30 },
    "porPrioridade": { "BAIXA": 200, "MEDIA": 250, "ALTA": 90 },
    "porOrigem": { "landing-page": 300, "indicacao": 120, "(sem valor)": 120 },
    "valorEstimadoPipeline": 184500.00
  },
  "topOrganizacoes": {
    "porFaturamento": [ { "organizacaoId": 12, "nomeFantasia": "Studio X", "valor": 41200.00, "quantidade": null } ],
    "porAgendamentos": [ { "organizacaoId": 5, "nomeFantasia": "Barber Y", "valor": null, "quantidade": 1820 } ],
    "porClientes": [ { "organizacaoId": 5, "nomeFantasia": "Barber Y", "valor": null, "quantidade": 940 } ]
  },
  "localizacoes": [ { "cidade": "São Paulo", "estado": "SP", "latitude": -23.55, "longitude": -46.63, "quantidade": 28 } ]
}
```

---

## Sugestões de layout (não obrigatório)

- **Topo (KPIs):** `organizacoes.total`/`ativas`, `clientes.total`, `agendamentos.total`, `financeiro.faturamentoMesAtual` + `crescimentoMensalPercentual` (seta verde/vermelha).
- **Cards de “hoje”:** `agendamentos.hoje`, `clientes.novosHoje`, `financeiro.faturamentoHoje`, `leads.novosHoje`.
- **Gráficos de linha:** `agendamentos.evolucaoMensal` (quantidade), `clientes.evolucaoMensal` (quantidade/acumulado), `financeiro.evolucaoMensal` (valor).
- **Donuts / barras:** `agendamentos.porStatus`, `financeiro.cobrancasPorStatus`, `instancias.porStatus`, `planos.distribuicao`, `leads.porTipoNegocio`.
- **Saúde da base:** `organizacoes.taxaAtivacao`, `ativasSemAgendamentos`, `ativasSemInstancia`, `financeiro.taxaInadimplencia`, `clientes.cadastroIncompleto`.
- **Tabelas Top 10:** `topOrganizacoes.porFaturamento` / `porAgendamentos` / `porClientes`.
- **Mapa:** `localizacoes` (cluster por `quantidade`) + barra lateral com `organizacoes.porEstado`.
- **Funil comercial:** bloco `leads` inteiro (kanban resumido por `porStatus`, pipeline em R$ por `valorEstimadoPipeline`).
- **Pé / metadata:** “Atualizado em {geradoEm}” + botão que chama `POST /api/v1/admin/dashboard/refresh`.
- Trate `planos.disponivel === false` com estado de erro só no card de planos; o resto continua renderizando.
