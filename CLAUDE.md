# NossaFeira — Contexto do Projeto

## Stack
- Kotlin + Jetpack Compose (Material Design 3)
- Arquitetura: MVVM
- Banco de dados: Room
- Reatividade: StateFlow + Coroutines
- Navegação: Navigation Compose
- Rede: Retrofit + OkHttp + Gson
- DI: sem framework — dependências instanciadas manualmente nos ViewModels
- Min SDK: 24 | Target SDK: 36

## Estrutura de Pastas

```
app/src/main/java/com/example/nossafeira/
├── data/
│   ├── db/          → NossaFeiraDatabase.kt (Room v5, com MIGRATION_1_2 até MIGRATION_4_5)
│   ├── model/       → ListaFeira.kt, ItemFeira.kt, ListaComItens.kt
│   ├── dao/         → ListaFeiraDao.kt, ItemFeiraDao.kt
│   ├── remote/
│   │   ├── api/     → NossaFeiraApi.kt (interface Retrofit)
│   │   ├── dto/     → ListaDto.kt, ItemDto.kt
│   │   └── RemoteDataSource.kt (AuthInterceptor + Retrofit)
│   └── repository/  → NossaFeiraRepository.kt, SyncResult.kt
├── ui/
│   ├── screens/
│   │   ├── listas/  → ListasScreen.kt (tela inicial com cards de listas)
│   │   └── itens/   → ItensScreen.kt (tela de itens de uma lista)
│   ├── components/
│   │   ├── ListaCard.kt
│   │   ├── ItemCard.kt
│   │   ├── SummaryCard.kt
│   │   ├── AddItemSheet.kt
│   │   ├── AddListaSheet.kt
│   │   └── FilterChips.kt
│   ├── utils/       → PrecoUtils.kt (extrairQuantidadeNumerica, calcularTotalGasto)
│   └── theme/       → Color.kt, Type.kt, Theme.kt
├── viewmodel/
│   ├── ListasViewModel.kt
│   ├── ItensViewModel.kt
│   └── SyncEvento.kt
├── navigation/      → NossaFeiraNavGraph.kt
└── MainActivity.kt
```

## Navegação

```
ListasScreen  ──(toque no card)──▶  ItensScreen(listaId)
     ▲                                      │
     └──────────────(back)──────────────────┘
```

- `ListasScreen` é a tela inicial (start destination)
- `ItensScreen` recebe `listaId: Int` como argumento de navegação
- Back press na `ItensScreen` volta para `ListasScreen`

---

## Entidades

```kotlin
// ListaFeira.kt
@Entity(tableName = "listas_feira")
data class ListaFeira(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val valorEstimado: Int = 0,          // em centavos (ex: R$ 9,99 → 999)
    val criadaEm: Long = System.currentTimeMillis(),
    val remoteId: String? = null,        // ID no MongoDB; null = lista local (não compartilhada)
    val isShared: Boolean = false,       // controla UI: botão compartilhar vs ícone de sync
    val updatedAt: Long = System.currentTimeMillis(), // atualizado em toda modificação local
    val syncedAt: Long = 0L             // timestamp do último sync bem-sucedido com o backend
)

// ItemFeira.kt
@Entity(
    tableName = "itens_feira",
    foreignKeys = [ForeignKey(
        entity = ListaFeira::class,
        parentColumns = ["id"],
        childColumns = ["listaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("listaId")]
)
data class ItemFeira(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listaId: Int,
    val nome: String,
    val quantidade: String,
    val categoria: Categoria,
    val preco: Int = 0,             // em centavos (ex: R$ 9,99 → 999); adicionado na v2, convertido na v3
    val comprado: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis(),
    val remoteItemId: String = UUID.randomUUID().toString() // UUID estável para identificar o item no backend
)

enum class Categoria { HORTIFRUTI, LATICINIOS, LIMPEZA, OUTROS, PROTEINAS, PADARIA }

// Relação usada nas queries
data class ListaComItens(
    @Embedded val lista: ListaFeira,
    @Relation(parentColumn = "id", entityColumn = "listaId")
    val itens: List<ItemFeira>
)
```

---

## Tema Visual — SEGUIR RIGOROSAMENTE

### Paleta de Cores (Dark Theme)

```kotlin
// Color.kt
val Background = Color(0xFF0F1117)
val Surface = Color(0xFF1A1D27)
val Surface2 = Color(0xFF222535)
val Surface3 = Color(0xFF2A2E42)
val Border = Color(0xFF2A2E42)

val Primary = Color(0xFF7C9EFF)
val PrimaryDim = Color(0xFF3D5499)
val PrimaryContainer = Color(0xFF1E2D5A)
val OnPrimary = Color(0xFF0A1433)

val Green = Color(0xFF4CAF82)
val GreenDim = Color(0xFF1E3D2F)
val Orange = Color(0xFFFFB547)
val OrangeDim = Color(0xFF3D2E10)
val Pink = Color(0xFFFF7DAA)
val PinkDim = Color(0xFF3D1525)
val Purple = Color(0xFFB47DFF)
val PurpleDim = Color(0xFF2A1A3D)
val Yellow = Color(0xFFFFD166)
val YellowDim = Color(0xFF3D300F)

val TextPrimary = Color(0xFFE8EAF6)
val TextSecondary = Color(0xFF9DA3C4)
val TextTertiary = Color(0xFF5A6080)
```

### Tipografia
- Fonte principal: **Plus Jakarta Sans** (Google Fonts)
- Títulos: 22sp bold
- Subtítulos de seção: 13sp semibold, uppercase, letter-spacing 0.5
- Corpo: 15sp semibold (nomes de item)
- Meta: 12sp regular (categoria, quantidade)

### Forma e Raios
- Cards de item: `RoundedCornerShape(16.dp)`
- Chips: `RoundedCornerShape(20.dp)`
- Campos de input: `RoundedCornerShape(10.dp)`
- FAB: `RoundedCornerShape(18.dp)`
- Bottom sheet: `RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`
- Emoji container: `RoundedCornerShape(12.dp)`

---

## Componentes — Especificação Detalhada

### 1. TopBar
- Título: "NossaFeira 🛒" (22sp bold, cor TextPrimary)
- Subtítulo: "Feira da semana" (13sp, cor TextSecondary)
- Botão de menu (⋮) no canto direito: 40x40dp, background Surface, radius 12dp

### 2. SearchBar
- Background: Surface
- Border: 1dp, cor Border
- Radius: 10dp
- Padding: 10dp vertical, 14dp horizontal
- Ícone de lupa + placeholder "Buscar item..."

### 3. FilterChips (LazyRow horizontal)
- Chips: "Todos", "🥬 Hortifruti", "🥛 Laticínios", "🧹 Limpeza", "📦 Outros", "🥩 Proteínas", "🍞 Padaria"
- Estado inativo: border 1.5dp Border, background transparente, texto TextSecondary
- Estado ativo: background PrimaryContainer, border Primary, texto Primary
- Padding: 6dp vertical, 14dp horizontal

### 4. SummaryCard
- Background: gradient LinearGradient(PrimaryContainer → Color(0xFF111827)), ângulo 135°
- Border: 1dp PrimaryDim
- Radius: 16dp
- Padding: 16dp
- Conteúdo:
  - Linha superior: label "Itens na lista" + badge verde "X comprados"
  - Valor: quantidade total de itens (20sp bold)
  - Badge comprados: background GreenDim, texto Green, radius 20dp
  - Badge totalGasto: exibido quando > 0, mostra "R$ X,XX" (background GreenDim, texto Green, radius 20dp)
    - Calculado somando `preco` dos itens onde `comprado == true && preco > 0`
  - ProgressBar: altura 4dp, background Surface3, fill gradient Primary→Green
  - Progresso = itens comprados / total

### 5. ListaCard (tela inicial)
- Background: Surface
- Border: 1dp Border
- Radius: 16dp
- Padding: 16dp
- Conteúdo:
  - Área principal (clicável): nome, progresso, valores, barra
  - Coluna de botões no canto direito (empilhados):
    - `!isShared` → ícone `Share` (Primary, 20dp) — chama `onCompartilhar`
    - `isShared` → ícone `Refresh` (Primary, 20dp) — chama `onSincronizar`
    - Ícone `Delete` (Pink, 20dp) — chama `onDelete`
  - Linha de progresso: "X de Y itens comprados" (13sp, TextSecondary)
  - Valor estimado: se > 0, exibir "Estimado: R$ X,XX" (13sp, TextTertiary)
  - Valor gasto real: se > 0, exibir "Gasto: R$ X,XX" (13sp, Green) — calculado via `calcularTotalGasto(itens)`
  - ProgressBar fina (4dp) mostrando % de itens comprados
  - Cor da barra varia: 0-33% → Orange, 34-66% → Primary, 67-100% → Green
- Toque no card → navega para ItensScreen passando o listaId

### 6. ItemCard
- Background: Surface
- Border: 1dp Border
- Radius: 16dp
- Padding: 14dp
- Barra lateral esquerda: 3dp de largura, cor varia por categoria:
  - HORTIFRUTI → Green
  - LATICINIOS → Primary
  - LIMPEZA → Orange
  - OUTROS → Purple
  - PROTEINAS → Pink
  - PADARIA → Yellow
- Itens comprados: opacity 0.5, nome com strikethrough
- **Long press** no card → abre `AddItemSheet` em modo edição (haptic feedback + `onLongClick`)

#### Assinatura:
```kotlin
fun ItemCard(
    item: ItemFeira,
    onToggleComprado: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
)
```

#### Estrutura interna do ItemCard (horizontal):
```
[Checkbox 24x24dp] [EmojiBox 42x42dp] [Info flex] [Quantidade badge] [Delete 32x32dp]
```
- **Checkbox**: radius 8dp, border 2dp Border; quando marcado: background Green, ícone ✓ branco
- **EmojiBox**: background Surface2, radius 12dp, emoji 26sp
  - HORTIFRUTI → 🥬 | LATICINIOS → 🥛 | LIMPEZA → 🧹 | OUTROS → 📦 | PROTEINAS → 🥩 | PADARIA → 🍞
- **Info**: nome do item (15sp semibold) + categoria (12sp, TextTertiary)
- **Badges (Column)**: quantidade e preço empilhados verticalmente (Arrangement.spacedBy 4dp)
  - **Quantidade**: background Surface2, radius 8dp, padding 4dp×10dp, texto 13sp bold TextSecondary
  - **Preço** *(condicional, só exibido quando `preco > 0`)*: background GreenDim, radius 8dp, padding 4dp×10dp, texto 13sp bold Green, formato "R$ X,XX"
- **Delete button**: IconButton 32×32dp, ícone `Icons.Default.Delete` 18dp, cor Pink

### 7. AddListaSheet (ModalBottomSheet — tela inicial)
- Campos: "Nome da lista" (obrigatório) e "Valor estimado R$" (opcional, numérico)
- Botão: "Criar lista"

### 8. AddItemSheet (ModalBottomSheet)
- Background: Surface
- Border top: 1dp Border
- Handle: 36×4dp, background Surface3, radius 2dp, centralizado
- Radius top: 28dp
- Padding: 20dp laterais, 32dp bottom
- Conteúdo com `verticalScroll` + `windowInsetsPadding(WindowInsets.ime)` — evita que o botão seja comprimido pelo teclado
- Suporta **modo edição**: parâmetro `itemParaEditar: ItemFeira? = null`
  - Quando não-nulo: pré-preenche os campos, título muda para "Editar item", botão para "Salvar alterações"
  - Quando nulo: comportamento padrão de adição

#### Campos:
- Label de campo: 12sp semibold uppercase, cor TextSecondary, letter-spacing 0.5
- Input: background Surface2, border 1.5dp (Border normal / Primary em foco), radius 10dp
- Campos: "Nome do item", "Quantidade" e "PREÇO R$ (OPCIONAL)" (teclado decimal, convertido para `Int` em centavos)

#### Grade de categorias (2×N, chunked(2)):
- Cada opção: border 1.5dp Border, radius 10dp, emoji + texto 13sp semibold
- Quando selecionada, cada categoria tem cor própria (igual às cores das barras laterais)
- Grid escala automaticamente para qualquer número de categorias via `chunked(2)` + `verticalScroll`

#### Botão Adicionar / Salvar:
- Background: Primary
- Texto: OnPrimary, 15sp bold
- Radius: 16dp, largura total, padding 15dp vertical

### 9. FAB
- Tamanho: 56×56dp
- Background: Primary
- Radius: 18dp
- Ícone: "+" branco
- Sombra: elevation 8dp com tint Primary (0xFFFFFF40 de alpha)
- Posição: bottom-end com padding 24dp

### 10. BottomNavigation
- Background: Surface
- Border top: 1dp Border
- 4 itens: Lista 🛒 | Resumo 📊 | Histórico 📜 | Config ⚙️
- Item ativo: label cor Primary + indicador pip (4×4dp, cor Primary, radius 2dp)
- Item inativo: label TextTertiary

---

## Comportamentos e Interações

- **Marcar item**: toque no checkbox → alterna `comprado`, atualiza progress bar e badge
- **Filtrar**: toque no chip → filtra lista por categoria, atualiza contador da seção
- **Adicionar**: FAB → abre BottomSheet; validar nome não vazio e categoria selecionada
- **Deletar item**: ícone lixeira (Pink) no final do ItemCard **ou** swipe horizontal (SwipeToDismissBox com background Pink)
- **Deletar lista**: ícone lixeira (Pink) no cabeçalho do ListaCard **ou** swipe horizontal (SwipeToDismissBox com background Pink)
- **Editar item**: long press no ItemCard → abre `AddItemSheet` em modo edição com campos pré-preenchidos; ao confirmar chama `ItensViewModel.editarItem`
- **Animações**: itens entram com `slideIn` + `fadeIn` ao carregar a lista
- **Feedback tátil**: `LocalHapticFeedback` ao marcar item como comprado e ao acionar long press para edição

---

## Regras Gerais de Código

- Sempre usar `collectAsStateWithLifecycle()` em vez de `collectAsState()`
- Separar previews `@Preview` para cada componente
- Cores nunca hardcoded — sempre via `MaterialTheme.colorScheme` ou as constantes acima
- Strings de UI em `strings.xml` (pt-BR)
- Nunca usar `LiveData` — apenas `StateFlow`
- Coroutines sempre em `viewModelScope`
