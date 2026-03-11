# NossaFeira — Feature de Compartilhamento Familiar: Implementação no App

## Status

**Implementado.** Esta documentação reflete o estado atual do código no branch `development`.

---

## Contexto

Compartilhamento de listas entre membros de uma mesma família via backend (AWS Lambda + MongoDB Atlas).
O backend está documentado em `docs/planejamento-compartilhamento-familia.md` e `docs/api-contract.md`.

Estratégia: **compartilhamento explícito por lista** — o usuário decide quais listas compartilhar.
Sync é **sob demanda** (manual ou no startup), sem WorkManager ou sync automático em background.

---

## Decisões de Implementação

- **Sem Hilt**: app simples, sem features complexas planejadas. Dependências instanciadas manualmente nos ViewModels via `run { ... }`.
- **RemoteDataSource instanciado no Repository**: recebido como parâmetro com default `RemoteDataSource()`.
- **AuthInterceptor dentro de `RemoteDataSource.kt`**: sem módulo separado.
- **BASE_URL por build type**: `debug` aponta para `10.0.2.2:3000` (emulador → localhost), `release` para URL da AWS.

---

## Migrations Realizadas

O banco já estava na v3 (conversão de valores para centavos) antes desta feature. As migrations desta feature são:

### MIGRATION_3_4 — Campos de compartilhamento em `ListaFeira`
```kotlin
db.execSQL("ALTER TABLE listas_feira ADD COLUMN remoteId TEXT")
db.execSQL("ALTER TABLE listas_feira ADD COLUMN isShared INTEGER NOT NULL DEFAULT 0")
db.execSQL("ALTER TABLE listas_feira ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
db.execSQL("ALTER TABLE listas_feira ADD COLUMN syncedAt INTEGER NOT NULL DEFAULT 0")
```

### MIGRATION_4_5 — UUID estável para itens
```kotlin
db.execSQL("ALTER TABLE itens_feira ADD COLUMN remoteItemId TEXT NOT NULL DEFAULT ''")
db.execSQL("""
    UPDATE itens_feira SET remoteItemId =
        lower(hex(randomblob(4))) || '-' ||
        lower(hex(randomblob(2))) || '-4' ||
        substr(lower(hex(randomblob(2))), 2) || '-' ||
        substr('89ab', abs(random()) % 4 + 1, 1) ||
        substr(lower(hex(randomblob(2))), 2) || '-' ||
        lower(hex(randomblob(6)))
""")
```

**Banco atualmente na versão 5.**

---

## Entidades Atualizadas

### ListaFeira
```kotlin
data class ListaFeira(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val valorEstimado: Int = 0,
    val criadaEm: Long = System.currentTimeMillis(),
    val remoteId: String? = null,       // ID no MongoDB; null = lista local
    val isShared: Boolean = false,      // controla UI: Share vs Refresh
    val updatedAt: Long = System.currentTimeMillis(), // atualizado a cada modificação local
    val syncedAt: Long = 0L            // timestamp do último sync bem-sucedido
)
```

### ItemFeira
```kotlin
data class ItemFeira(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listaId: Int,
    val nome: String,
    val quantidade: String,
    val categoria: Categoria,
    val preco: Int = 0,
    val comprado: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis(),
    val remoteItemId: String = UUID.randomUUID().toString() // UUID para identificar item no backend
)
```

---

## updatedAt — Quando é Atualizado

O `NossaFeiraRepository` atualiza `updatedAt` da lista pai via `listaDao.atualizarUpdatedAt(listaId, now)` após:
- `adicionarItem()`
- `atualizarItem()`
- `deletarItem()`
- `toggleComprado()`

`deletarItem()` recebe `ItemFeira` inteiro (não só o id) para ter acesso ao `listaId`.

---

## Estrutura de Pastas Implementada

```
app/src/main/java/com/example/nossafeira/
├── data/
│   ├── db/          → NossaFeiraDatabase.kt (v5, MIGRATION_1_2 até MIGRATION_4_5)
│   ├── model/       → ListaFeira.kt, ItemFeira.kt, ListaComItens.kt
│   ├── dao/         → ListaFeiraDao.kt, ItemFeiraDao.kt
│   ├── remote/
│   │   ├── api/     → NossaFeiraApi.kt
│   │   ├── dto/     → ListaDto.kt (+ ListaPageDto, PostListaRequest, PutListaRequest), ItemDto.kt
│   │   └── RemoteDataSource.kt (AuthInterceptor embutido)
│   └── repository/  → NossaFeiraRepository.kt, SyncResult.kt
├── ui/
│   ├── components/  → ListaCard.kt (botões share/sync/delete)
│   └── screens/listas/ → ListasScreen.kt (observa syncEvento, exibe Toasts)
└── viewmodel/
    ├── ListasViewModel.kt
    └── SyncEvento.kt
```

---

## Camada de Rede

### build.gradle.kts
```kotlin
// defaultConfig
buildConfigField("String", "FAMILY_ID", "\"familia-silva-2024\"")
buildConfigField("String", "API_KEY", "\"chave-secreta-aqui\"")

// buildTypes
debug {
    buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/dev/\"")
}
release {
    buildConfigField("String", "BASE_URL", "\"https://sua-url-producao.amazonaws.com/dev/\"")
}
```

### AndroidManifest
- `INTERNET` permission no manifest principal
- `android:usesCleartextTraffic="true"` apenas no `src/debug/AndroidManifest.xml`

### AuthInterceptor (em RemoteDataSource.kt)
```kotlin
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .addHeader("x-api-key", BuildConfig.API_KEY)
                .addHeader("x-family-id", BuildConfig.FAMILY_ID)
                .build()
        )
}
```

### DTOs
- `ItemDto`: id (remoteItemId), nome, quantidade, categoria, preco, comprado, criadoEm
- `ListaDto`: id (_id MongoDB), nome, valorEstimado, valorCalculado, criadaEm, updatedAt, itens
- `ListaPageDto`: content, page, pageSize, totalElements
- `PostListaRequest`: id, nome, valorEstimado, valorCalculado, criadaEm, itens
- `PutListaRequest`: nome, valorEstimado, valorCalculado, itens

**Nota:** `valorCalculado` = soma dos preços dos itens comprados (mesmo que `totalGasto`). Calculado via `calcularTotalGasto(itens)` de `PrecoUtils.kt`.

---

## Repository — Métodos de Sync

```kotlin
suspend fun compartilharLista(listaComItens: ListaComItens)
// POST /listas → salva remoteId, isShared=true, syncedAt=now

suspend fun sincronizarLista(listaComItens: ListaComItens): SyncResult
// GET /listas/{remoteId}
//   404 → marcarComoLocal() → SyncResult.ListaDeletada
//   localTemMudancas && remoteTemMudancas → vence o updatedAt mais recente
//   só localTemMudancas → PUT com versão local → SyncResult.Sucesso
//   só remoteTemMudancas → aplica versão do backend → SyncResult.Conflito
//   nenhum mudou → SyncResult.Sucesso

suspend fun pullStartup()
// GET /listas → para cada lista remota:
//   - não existe localmente → insere no Room
//   - existe e backend.updatedAt > syncedAt → sobrescreve local

suspend fun deletarListaCompartilhada(lista: ListaFeira)
// DELETE /listas/{remoteId} → deleta do Room
```

### SyncResult
```kotlin
enum class SyncResult { Sucesso, Conflito, Erro, ListaDeletada }
```

### Mappers (privados no Repository)
- `ItemFeira.toDto()` — usa `remoteItemId` como `id`
- `ListaDto.toLista()` — converte para `ListaFeira` com `isShared=true`
- `ItemDto.toItem(listaId)` — preserva `remoteItemId` vindo do backend

---

## ViewModel — ListasViewModel

```kotlin
// Emite eventos para a UI exibir Toasts
val syncEvento: SharedFlow<SyncEvento>

// Estado de loading para o botão de sync global na TopBar
val isSyncing: StateFlow<Boolean>

// Executado no init{} — silencioso
private fun pullStartup()

// Pull manual global (TopBar): mesmo que pullStartup(), mas com feedback via isSyncing e SyncEvento
fun sincronizarTodas()

fun compartilharLista(listaComItens: ListaComItens)
fun sincronizarLista(listaComItens: ListaComItens)
fun deletarLista(listaComItens: ListaComItens) // DELETE no backend se isShared
```

### SyncEvento
```kotlin
enum class SyncEvento { Compartilhada, Sincronizada, Conflito, ErroRede, ListaDeletada, PullConcluido }
```

### Mensagens de Toast
| Evento | Mensagem |
|--------|----------|
| Compartilhada | "Lista compartilhada com sucesso." |
| Sincronizada | "Lista sincronizada com sucesso." |
| Conflito | "Lista atualizada. Suas alterações locais foram substituídas." |
| ErroRede | "Falha na sincronização. Verifique sua conexão." |
| ListaDeletada | "A lista foi removida pelo outro membro e voltou a ser local." |
| PullConcluido | "Listas sincronizadas com sucesso." |

---

## UI — ListasTopBar

Botão Refresh (↺) no canto direito (40×40dp, background Surface, radius 12dp):
- Ícone `Icons.Default.Refresh`, cor TextSecondary em repouso / Primary durante sync
- Enquanto sincronizando: ícone gira (animação infinita 800ms/volta), clique desabilitado
- Chama `viewModel.sincronizarTodas()`
- Recebe `isSyncing: Boolean` e `onSync: () -> Unit` como parâmetros

## UI — ListaCard

Coluna de botões no canto direito:
- `!isShared` → `Icons.Default.Share` (Primary) → chama `onCompartilhar`
- `isShared` → `Icons.Default.Refresh` (Primary) → chama `onSincronizar`
- Sempre: `Icons.Default.Delete` (Pink) → chama `onDelete`

```kotlin
fun ListaCard(
    listaComItens: ListaComItens,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCompartilhar: () -> Unit = {},
    onSincronizar: () -> Unit = {},
    modifier: Modifier = Modifier
)
```

---

## Pendências / Limitações Conhecidas

- **`updatedAt` da lista**: não é atualizado ao editar nome/valor estimado da lista (apenas operações de item). O `atualizarLista()` no repository não chama `atualizarUpdatedAt()`.
- **pullStartup não reverte listas deletadas remotamente**: se outro membro deletar uma lista e o dispositivo local fizer apenas o pull de startup (sem clicar em sync no card), a lista local com `isShared=true` permanece sem ser revertida para local. A reversão só ocorre quando o usuário clica em sync no card individual (detecta 404) ou na próxima vez que o `GET /listas` não retornar aquela lista — mas esta segunda checagem ainda não está implementada.
