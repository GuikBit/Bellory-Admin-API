# Bellory-Admin-API

Painel administrativo da plataforma Bellory. Projeto Spring Boot independente do `Bellory-API`, compartilhando o mesmo banco PostgreSQL.

## Status

**Em construcao** — split do `Bellory-API` em andamento. Acompanhar progresso em [`../Bellory-API/docs/PLANO_SEPARACAO_ADMIN_API.md`](../Bellory-API/docs/PLANO_SEPARACAO_ADMIN_API.md).

## Arquitetura

- **Schemas que este projeto controla**: `admin`, `site`
- **Schema gerenciado pelo `Bellory-API`**: `app`
- **Banco**: PostgreSQL (mesmo do `Bellory-API`)
- **Cache**: Redis (compartilhado com `Bellory-API`, admin e produtor das chaves `payment:status:*`)
- **JWT**: mesmo segredo do `Bellory-API` (tokens compativeis)

## Ambientes

- **Dev local**: roda em `localhost:8085` (Bellory-API roda em `localhost:8081`). **Nao ha ambiente dev hospedado.**
- **Prod**: `api-admin.bellory.com.br`

## Pre-requisitos

- Java 21
- Maven 3.9+
- PostgreSQL rodando localmente (mesma instancia do Bellory-API, banco `bellory_dev`)
- Redis rodando localmente

## Como rodar (dev)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Ou via Docker:

```bash
docker-compose -f docker-compose-dev.yml up --build
```

## Convencoes de migration

- Toda migration nova **DEVE** mexer apenas em schemas `admin` ou `site`
- Migrations que mexem no schema `app` vao para o `Bellory-API`
- Tabela de historico Flyway: `flyway_schema_history_admin`

## Endpoints principais (apos FASE 3)

- `/api/v1/admin/**` - painel administrativo
- `/api/v1/admin/auth/**` - autenticacao admin
- `/api/v1/admin/analytics/**` - analytics do site bellory.com.br
- `/api/v1/tracking` - ingestao publica de eventos do site
- `/api/v1/webhook/payment` - webhook da Payment API
- `/swagger-ui.html` - documentacao OpenAPI
