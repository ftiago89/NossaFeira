# NossaFeira — Backlog de Features Futuras

Planejamentos e análises de features ainda não implementadas.

---

## Preenchimento de preço por foto da etiqueta

**Ideia:** ao cadastrar/editar um item sem valor, o usuário pode tocar em um ícone de câmera ao lado do campo de preço. A câmera abre, ele fotografa a etiqueta de preço na prateleira e o valor é extraído automaticamente via OCR e preenchido no campo.

**Status:** ✅ implementado em março/2026 (branch `feature/camera-preco`).

### Faz sentido para o app?

**Sim, com ressalvas.**

**A favor:**
- O campo de preço já existe e é usado frequentemente (o app calcula total gasto)
- No supermercado, fotografar `R$ 127,49` é mais rápido que digitar
- ML Kit on-device é gratuito e sem custo recorrente

**Contra:**
- Aumento de ~4-6MB no APK (impacto de 30-50% no tamanho atual) — relevante porque o app é distribuído via APK/WhatsApp
- Preços simples como `R$ 12,99` levam ~3s para digitar vs 5-10s pelo fluxo câmera

**Escopo V1:** aceitar apenas padrões `R$ X,XX` e `R$ X.XXX,XX`. Iterar conforme uso real. Ignorar parcelas (`3x R$ 4,33`) e moedas estrangeiras (`$12.99`, `EUR 5,99`).

---

### Componentes

| Parte | Complexidade | Detalhe |
|---|---|---|
| Captura de foto | Baixa | `ActivityResultLauncher` com `TakePicture` — intent do sistema, sem CameraX |
| FileProvider | Baixa | `TakePicture` exige URI `content://` — requer `file_paths.xml` + provider no manifest |
| OCR | Baixa | ML Kit Text Recognition — on-device, gratuito, sem rede |
| Extração do preço | Média-Alta | Regex + heurísticas para desambiguar múltiplos preços na etiqueta |
| Permissão de câmera | Baixa | `Manifest.permission.CAMERA` com tratamento de negação e negação permanente |
| UI | Média | Ícone no campo de preço + loading + chips de candidatos + orquestração entre screen e sheet |
| Redimensionamento de imagem | Baixa | Reduzir para ~1-2MP antes do OCR para evitar OOM em celulares com pouca RAM |
| Testes | Média | `PrecoOcrExtractorTest` — 15-20 casos unitários |

### Dependência nova
```toml
# libs.versions.toml
mlkitTextRecognition = "16.0.1"
```
```kotlin
// build.gradle.kts
implementation(libs.mlkit.text.recognition) // ~4-6MB no APK
```

---

### Arquitetura

#### Decisão crítica: onde fica o `ActivityResultLauncher`

O `ModalBottomSheet` é composição condicional — quando a câmera abre, o Android pode dismissar o sheet e destruir o estado. **O launcher deve ficar no `ItensScreen`**, que está sempre na composição.

```
ItensScreen (launcher registrado aqui, sempre na composição)
  └── AddItemSheet (recebe callbacks e estado)
        └── Campo preço + ícone câmera → chama onCameraRequest()
```

#### Decisão crítica: ViewModel NÃO tem lógica de OCR

O ViewModel não deve depender de `android.graphics.Bitmap` nem classes ML Kit — isso quebraria os testes unitários JVM (padrão do projeto). A lógica se divide em:

- **`PrecoOcrExtractor.kt`** (`ui/utils/`) — função pura `extrairPrecosDaEtiqueta(texto: String): List<Int>` (testável como `PrecoUtils`)
- **`ItensScreen.kt`** — chamada ML Kit via coroutine na camada composable, orquestração câmera→OCR→resultado
- **ViewModel** — **não muda**

#### Decisão: ícone de câmera no campo de preço

O composable `CampoTexto` (em `AddListaSheet.kt`) é um `BasicTextField` sem slot para ícone. Em vez de alterar o componente compartilhado, envolver numa `Row`:

```kotlin
Row {
    CampoTexto(modifier = Modifier.weight(1f), ...)
    IconButton(onClick = onCameraRequest) { Icon(Icons.Default.CameraAlt, ...) }
}
```

---

### Fluxo revisado

```
1.  Usuário toca ícone câmera no AddItemSheet
2.  AddItemSheet chama onCameraRequest() (callback hoisted para ItensScreen)
3.  ItensScreen verifica permissão CAMERA
    - Concedida → passo 5
    - Não concedida → solicita permissão
4.  Resultado da permissão:
    - Concedida → passo 5
    - Negada → Toast "Permissão de câmera necessária"
    - Negada permanentemente → Toast com instrução para Settings
5.  Cria arquivo temporário + URI via FileProvider
6.  Lança TakePicture com o URI
7.  Resultado da câmera:
    - Sucesso → carrega bitmap (redimensionado para ~1-2MP), roda ML Kit OCR
    - Falha/cancelamento → nada acontece
8.  Texto OCR → PrecoOcrExtractor.extrairPrecosDaEtiqueta(texto)
9.  Resultado:
    - 0 candidatos → Toast "Nenhum preço encontrado na imagem"
    - 1 candidato → preenche campo precoTexto diretamente (usuário pode alterar antes de confirmar)
    - 2+ candidatos → exibe chips clicáveis abaixo do campo; toque preenche o campo
10. Deleta arquivo temporário da foto
```

---

### Principal risco

Etiquetas de supermercado no Brasil variam muito entre redes — preço por kg, preço total, preço à vista vs parcelado, promoções com múltiplos valores. Preencher silenciosamente o valor errado seria uma experiência ruim.

**Recomendação:** nunca preencher e fechar automaticamente. Sempre mostrar o(s) candidato(s) e deixar o usuário confirmar com um toque. Validar com fotos reais de diferentes redes antes de definir o fluxo de confirmação.

---

### Edge cases

- **Process death durante câmera:** em celulares com pouca RAM, o Android mata o processo enquanto a câmera está aberta. O URI da foto e o estado do sheet devem ser restauráveis via `rememberSaveable`
- **Rotação de tela** durante captura — Activity recreation destrói estado
- **Separador de milhares brasileiro:** `R$ 1.299,00` — o ponto NÃO é decimal, é milhar. A regex deve tratar isso
- **Produtos importados:** etiquetas com `$12.99` ou `EUR 5,99` — ignorar na V1
- **Parcelas em etiqueta:** `3x R$ 4,33` — ignorar na V1
- **Latência na primeira execução:** ML Kit inicializa o modelo na primeira chamada, podendo levar 1-2s. O loading indicator deve cobrir esse cenário
- **Fotos de alta resolução:** 16MP = ~64MB de heap. Redimensionar antes do OCR é obrigatório

---

### Arquivos modificados

| Arquivo | Caminho | Mudança |
|---|---|---|
| AddItemSheet | `ui/components/AddItemSheet.kt` | Ícone câmera ao lado do campo preço, chips de candidatos, novos parâmetros (`onCameraRequest`, `precoSugeridos`, `isProcessandoOcr`) |
| ItensScreen | `ui/screens/itens/ItensScreen.kt` | `ActivityResultLauncher` (TakePicture + permissão), orquestração OCR, estado `precoSugeridos` |
| PrecoOcrExtractor | `ui/utils/PrecoOcrExtractor.kt` | **NOVO** — `extrairPrecosDaEtiqueta(texto: String): List<Int>` |
| AndroidManifest | `AndroidManifest.xml` | Permissão `CAMERA` + declaração `<provider>` FileProvider |
| file_paths.xml | `res/xml/file_paths.xml` | **NOVO** — `<cache-path>` para fotos temporárias |
| libs.versions.toml | `gradle/libs.versions.toml` | Versão ML Kit Text Recognition |
| build.gradle.kts | `app/build.gradle.kts` | `implementation(libs.mlkit.text.recognition)` |
| PrecoOcrExtractorTest | `test/.../ui/utils/PrecoOcrExtractorTest.kt` | **NOVO** — testes unitários da extração |

---

### Testes

**Testes unitários JVM (`PrecoOcrExtractorTest`):**

| Entrada | Saída esperada |
|---|---|
| `"R$ 12,99"` | `[1299]` |
| `"R$12,99"` (sem espaço) | `[1299]` |
| `"R$ 1.299,00"` (milhar) | `[129900]` |
| `"De R$ 15,99 Por R$ 12,99"` (promoção) | `[1599, 1299]` |
| `"12,99"` (sem R$) | `[1299]` |
| `"Arroz tipo 1 5kg"` (sem preço) | `[]` |
| `"R$ 0,99"` | `[99]` |
| `"R$ 1.234.567,89"` (múltiplos milhares) | `[123456789]` |
| `""` (vazio) | `[]` |
| `"3x R$ 4,33"` (parcela — V1 ignora contexto, extrai valor) | `[433]` |

**Testes de ViewModel:** não precisam mudar (OCR fica fora do ViewModel).

---

### Checklist de verificação

- [x] Ícone de câmera visível ao lado do campo de preço no AddItemSheet
- [x] Permissão de câmera solicitada corretamente na primeira vez
- [x] Câmera do sistema abre e captura foto
- [x] OCR extrai preço de foto com `R$ X,XX` visível
- [x] Campo de preço é preenchido com valor extraído
- [x] Toast exibido quando nenhum preço é encontrado
- [x] Múltiplos candidatos exibidos como chips clicáveis
- [x] Foto temporária deletada após processamento
- [x] Testes unitários de `PrecoOcrExtractor` passam
- [x] App não crasha ao negar permissão de câmera
- [x] App funciona em celulares com pouca RAM (bitmap redimensionado)
- [x] Loading indicator visível durante processamento OCR

### Decisões tomadas na implementação

- **Ícone câmera**: drawable vetorial `ic_camera_alt.xml` — Material Icons Extended foi descartado para não aumentar o APK
- **OCR com ponto decimal**: ML Kit às vezes retorna `99.99` (padrão americano) em vez de `99,99`. `PrecoOcrExtractor` trata ambos os formatos
- **Launchers em `ItensScreen`**: `ModalBottomSheet` pode ser destruído enquanto a câmera está aberta; os launchers `TakePicture` e `RequestPermission` precisam estar em um composable sempre na composição
- **Estado em `rememberSaveable`**: `fotoUri`, `precoSugeridos` e `isProcessandoOcr` sobrevivem a process death (celulares com pouca RAM matam o processo enquanto a câmera está aberta)
- **ViewModel não muda**: toda lógica de OCR fica em `ui/utils/PrecoOcrExtractor.kt` (função pura, testável na JVM) e a orquestração fica em `ItensScreen` — padrão testável do projeto é preservado
