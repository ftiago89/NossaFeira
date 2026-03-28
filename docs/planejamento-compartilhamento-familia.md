# NossaFeira — Planejamento: Compartilhamento de Listas por Família

## Contexto

O NossaFeira é um app Android de lista de compras (Kotlin + Jetpack Compose + Room).
A feature permite que membros de uma mesma família compartilhem listas de compras, sem disponibilização pública — o app será instalado via Android Studio para pessoas específicas.

Estratégia: **compartilhamento explícito por lista** — o usuário decide quais listas compartilhar via botão no ListaCard.

---

## Decisões Tomadas

### Distribuição do App
- Instalação manual via Android Studio para cada pessoa da família
- Sem publicação na Play Store

### Identificação da Família (familyId)
- O `familyId` será definido antes do build e hardcoded via `BuildConfig`
- Todos os membros da mesma família recebem o app compilado com o mesmo `familyId`

```kotlin
// build.gradle
buildConfigField("String", "FAMILY_ID", '"familia-silva-2024"')
buildConfigField("String", "API_KEY", '"chave-secreta-aqui"')
```

### Autenticação
- **Sem Cognito** — considerado complexo demais para o contexto de uso privado
- Autenticação via **API Key estática** no header das requisições
- A API Key será hardcoded no `BuildConfig` junto com o `familyId`
- Protege o endpoint sem adicionar complexidade de tokens JWT, refresh, etc.

### Backend
- **AWS Lambda** com Node.js 20 — **Serverless Framework v3**
- **AWS API Gateway** expondo os endpoints REST
- Custo praticamente zero para uso familiar

### Banco de Dados
- **MongoDB Atlas** — free tier (500MB)
- 500MB é mais que suficiente para listas de compras familiares
- Cluster gratuito, sem custo adicional

### Valores Monetários
- Todos os valores (`valorEstimado`, `valorCalculado`, `preco`) são armazenados em **centavos** (inteiros)
- Evita problemas de arredondamento de ponto flutuante
- O app Android é responsável por converter na exibição

### Valor Calculado
- `valorCalculado` representa a soma dos preços de todos os itens com preço cadastrado
- Calculado pelo app Android e enviado no body — o backend apenas armazena

---

## Arquitetura

```
App Android
  └── BuildConfig.FAMILY_ID + API_KEY (header)
        │
        ▼
  API Gateway ──▶ Lambda (Node.js 20)
                      │
                      ▼
                  MongoDB Atlas (free tier)
```

---

## Modelagem do Banco de Dados

Collection: `listas`

```json
{
  "_id": "uuid-gerado-pelo-app",
  "familyId": "familia-silva-2024",
  "nome": "Feira da semana",
  "valorEstimado": 15000,
  "valorCalculado": 850,
  "criadaEm": 1741564800000,
  "updatedAt": 1741564800000,
  "itens": [
    {
      "id": "uuid",
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

- `_id` é um UUID gerado pelo app Android e enviado no body do POST — o backend não gera o `_id`
- Itens são **embedded** dentro do documento da lista (padrão MongoDB, evita joins)
- `updatedAt` é atualizado automaticamente pelo Mongoose a cada PUT
- Índice obrigatório: `{ familyId: 1 }` para queries eficientes
- Valores monetários em centavos (inteiros)

---

## Endpoints REST

```
POST   /listas                       → compartilhar lista (primeira vez)
GET    /listas                       → pull de todas as listas da família (startup) — paginado
GET    /listas/{id}                  → pull de uma lista específica (sync manual)
PUT    /listas/{id}                  → push de mudanças locais (lista + itens embutidos)
DELETE /listas/{id}                  → deletar lista compartilhada
```

O `familyId` é extraído do header da requisição e usado para filtrar todos os dados.
Os itens são sempre enviados embutidos no corpo da lista — não existem endpoints separados para itens.

### Paginação (GET /listas)

Query params opcionais: `page` (default: 0) e `pageSize` (default: 50)

Resposta:
```json
{
  "content": [...],
  "page": 0,
  "pageSize": 50,
  "totalElements": 10
}
```

---

## Conexão Lambda → MongoDB Atlas

Padrão de cache com Mongoose para evitar overhead de cold start — implementado em `src/database/db.js`:

```javascript
const mongoose = require('mongoose');

let connection = null;

async function db() {
  if (connection === null) {
    connection = mongoose.connect(process.env.DB, {
      serverSelectionTimeoutMS: 5000,
      connectTimeoutMS: 10000,
    });
    await connection;
  }
  return connection;
}
```

O `await db()` é chamado dentro de cada **service** — não na lambda.

---

## Estrutura de Pastas

```
src/
├── lambdas/
│   ├── listas/         ← recebe evento, valida auth, valida body, chama service
│   └── validators/     ← schemas AJV por operação
├── services/
│   └── listas/         ← regras de negócio e queries no MongoDB
├── models/
│   └── Lista.js
├── middleware/
│   └── auth.js
├── database/
│   └── db.js
├── utils/
└── errors/
```

---

## Sincronização no App Android

- Sem real-time (sem WebSocket/Firebase)
- Sem WorkManager — sync é sempre sob demanda ou no startup
- Room continua sendo a fonte de verdade local
- Sync ocorre em três momentos:
  1. **Startup**: pull silencioso de todas as listas compartilhadas da família
  2. **Manual global**: botão Refresh (↺) na TopBar da tela de listagem → chama `pullStartup()` com feedback visual
  3. **Manual por lista**: ícone de sync em cada ListaCard compartilhado

### Estratégia de Conflito — Merge Three-Way com Snapshot

A resolução de conflitos opera em dois níveis:

**Nível lista** — comparação via `syncedAt` (timestamp do servidor):

| `local.updatedAt > syncedAt` | `remote.updatedAt > syncedAt` | Resultado |
|---|---|---|
| Não | Não | Nada a fazer |
| Sim | Não | Só local mudou → PUT com versão local |
| Não/Sim | Sim | Remote mudou → merge three-way por item + PUT merged |

**Nível item** — merge three-way usando snapshot (`sync*` fields no `ItemFeira`):

Cada item tem campos de snapshot (`syncNome`, `syncQuantidade`, `syncPreco`, `syncComprado`, `syncCategoria`) gravados no último sync. No merge, compara-se local vs snapshot e remote vs snapshot:

| Local vs snapshot | Remote vs snapshot | Ação |
|---|---|---|
| Igual | Igual | Nada (manter local) |
| Diferente | Igual | Manter local (só local editou) |
| Igual | Diferente | Aceitar remote (só remote editou) |
| Diferente | Diferente | Aceitar remote (último push ganha) |

- Itens só no local → **preservados** (nunca deletados durante sync)
- Itens só no remote → **inseridos**
- Após push: snapshot é atualizado para refletir o estado sincronizado
- Se o backend retorna 404 ao sincronizar, a lista volta a ser local (`isShared=false`, `remoteId=null`)

---

## Conectividade Lambda → MongoDB Atlas

- Free tier M0 **não suporta VPC Peering nem PrivateLink** (disponível apenas em M10+)
- A conexão ocorre pela **internet pública com TLS** — sem necessidade de VPC
- Lambda IPs são dinâmicos, portanto o Atlas Network Access deve liberar `0.0.0.0/0`
- Segurança garantida em duas camadas:
  1. Credenciais no `DB` connection string (usuário/senha do cluster)
  2. API Key no header das requisições (`x-api-key`)
- Conclusão: **M0 + `0.0.0.0/0` no Network Access** é suficiente para o contexto familiar privado

---

## Decisões de Implementação

- **Deploy**: Serverless Framework v3 — já conhecido, suporte nativo a API Gateway + Lambda
- **Desenvolvimento local**: serverless-offline v13 com `host: 0.0.0.0` para funcionar no devcontainer
- **Secrets**: `config/{stage}.json` injetados pelo Serverless Framework; `config/prod.json` nunca commitado
- **Validação de body**: AJV v8 com schemas por operação — erro retorna 400
- **Erros**: `GeneralError` lançado nos middlewares e services; `catch` da lambda chama `errorResponse`
- **Paginação**: page baseada em 0 (offset style), pageSize default 50
