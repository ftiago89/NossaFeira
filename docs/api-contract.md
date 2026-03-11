# NossaFeira Server — Contrato de API

## Informações Gerais

- **Base URL produção:** definida após deploy na AWS
- **Base URL local:** `http://localhost:3000/dev`
- **Formato:** JSON
- **Autenticação:** obrigatória em todos os endpoints via headers

---

## Autenticação

Todos os endpoints exigem os seguintes headers:

| Header | Descrição |
|--------|-----------|
| `x-api-key` | Chave estática de autenticação |
| `x-family-id` | Identificador da família |

Erros de autenticação:
- `x-api-key` ausente ou inválida → `401 Unauthorized`
- `x-family-id` ausente → `400 Bad Request`

---

## Valores Monetários

Todos os valores monetários (`valorEstimado`, `valorCalculado`, `preco`) são **inteiros em centavos**.

Exemplos:
- R$ 150,00 → `15000`
- R$ 8,50 → `850`

---

## Timestamps

Todos os timestamps (`criadaEm`, `updatedAt`, `criadoEm`) são **Unix timestamp em milissegundos** (Long).

---

## Endpoints

---

### POST /listas
Compartilha uma lista pela primeira vez.

**Request body:**
```json
{
  "_id": "string (uuid, obrigatório)",
  "nome": "string (obrigatório, minLength: 1)",
  "valorEstimado": "integer >= 0 (opcional, centavos)",
  "valorCalculado": "integer >= 0 (opcional, centavos)",
  "criadaEm": "number (obrigatório, timestamp ms)",
  "itens": [
    {
      "id": "string (uuid, obrigatório)",
      "nome": "string (obrigatório, minLength: 1)",
      "quantidade": "string (opcional)",
      "categoria": "string (opcional)",
      "preco": "integer >= 0 (opcional, centavos)",
      "comprado": "boolean (opcional)",
      "criadoEm": "number (obrigatório, timestamp ms)"
    }
  ]
}
```

**Campos adicionais no body são rejeitados (400).**

**Response de sucesso — 201 Created:**
```json
{
  "_id": "uuid-gerado-pelo-app",
  "familyId": "familia-silva-2024",
  "nome": "Feira da semana",
  "valorEstimado": 15000,
  "valorCalculado": 850,
  "criadaEm": 1741564800000,
  "updatedAt": 1741564800000,
  "itens": []
}
```

**Erros possíveis:**

| Status | Motivo |
|--------|--------|
| 400 | Body inválido (campo obrigatório ausente, tipo incorreto, valor negativo) |
| 401 | API key inválida ou ausente |
| 500 | Erro interno |

---

### GET /listas
Pull de todas as listas da família. Suporta paginação.

**Query params (opcionais):**

| Param | Tipo | Default | Descrição |
|-------|------|---------|-----------|
| `page` | integer | `0` | Página (base 0) |
| `pageSize` | integer | `50` | Itens por página |

**Response de sucesso — 200 OK:**
```json
{
  "content": [
    {
      "_id": "uuid",
      "nome": "Feira da semana",
      "valorEstimado": 15000,
      "valorCalculado": 850,
      "criadaEm": 1741564800000,
      "updatedAt": 1741564800000,
      "itens": []
    }
  ],
  "page": 0,
  "pageSize": 50,
  "totalElements": 3
}
```

**Erros possíveis:**

| Status | Motivo |
|--------|--------|
| 401 | API key inválida ou ausente |
| 500 | Erro interno |

---

### GET /listas/{id}
Pull de uma lista específica (sync manual).

**Path params:**

| Param | Descrição |
|-------|-----------|
| `id` | UUID da lista (`_id` no banco) |

**Response de sucesso — 200 OK:**
```json
{
  "_id": "uuid",
  "familyId": "familia-silva-2024",
  "nome": "Feira da semana",
  "valorEstimado": 15000,
  "valorCalculado": 850,
  "criadaEm": 1741564800000,
  "updatedAt": 1741564800000,
  "itens": [
    {
      "id": "uuid-item",
      "nome": "Arroz",
      "quantidade": "2kg",
      "categoria": "OUTROS",
      "preco": 850,
      "comprado": false,
      "criadoEm": 1741564800000
    }
  ]
}
```

**Erros possíveis:**

| Status | Motivo |
|--------|--------|
| 401 | API key inválida ou ausente |
| 404 | Lista não encontrada |
| 500 | Erro interno |

---

### PUT /listas/{id}
Push de mudanças locais. Substitui a lista inteira (lista + itens).

**Path params:**

| Param | Descrição |
|-------|-----------|
| `id` | UUID da lista (`_id` no banco) |

**Request body:**
```json
{
  "nome": "string (opcional, minLength: 1)",
  "valorEstimado": "integer >= 0 (opcional, centavos)",
  "valorCalculado": "integer >= 0 (opcional, centavos)",
  "itens": [
    {
      "id": "string (uuid, obrigatório)",
      "nome": "string (obrigatório, minLength: 1)",
      "quantidade": "string (opcional)",
      "categoria": "string (opcional)",
      "preco": "integer >= 0 (opcional, centavos)",
      "comprado": "boolean (opcional)",
      "criadoEm": "number (obrigatório, timestamp ms)"
    }
  ]
}
```

**Campos adicionais no body são rejeitados (400).**

O `updatedAt` é atualizado automaticamente pelo backend.

**Response de sucesso — 200 OK:**
```json
{
  "_id": "uuid",
  "familyId": "familia-silva-2024",
  "nome": "Feira da semana atualizada",
  "valorEstimado": 20000,
  "valorCalculado": 1700,
  "criadaEm": 1741564800000,
  "updatedAt": 1741568400000,
  "itens": []
}
```

**Erros possíveis:**

| Status | Motivo |
|--------|--------|
| 400 | Body inválido |
| 401 | API key inválida ou ausente |
| 404 | Lista não encontrada |
| 500 | Erro interno |

---

### DELETE /listas/{id}
Deleta uma lista compartilhada.

**Path params:**

| Param | Descrição |
|-------|-----------|
| `id` | UUID da lista (`_id` no banco) |

**Response de sucesso — 200 OK** (sem body)

**Erros possíveis:**

| Status | Motivo |
|--------|--------|
| 401 | API key inválida ou ausente |
| 404 | Lista não encontrada |
| 500 | Erro interno |

---

## Formato de Erros

Todos os erros retornam o seguinte formato:

```json
{
  "timeStamp": "2026-03-10T12:00:00.000Z",
  "status": 400,
  "statusDescription": "Bad Request",
  "type": "General Error",
  "errors": [
    {
      "instancePath": "/itens/0/preco",
      "message": "must be >= 0"
    }
  ]
}
```
