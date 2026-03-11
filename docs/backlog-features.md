# NossaFeira — Backlog de Features Futuras

Planejamentos e análises de features ainda não implementadas.

---

## Preenchimento de preço por foto da etiqueta

**Ideia:** ao cadastrar/editar um item sem valor, o usuário pode tocar em um ícone de câmera ao lado do campo de preço. A câmera abre, ele fotografa a etiqueta de preço na prateleira e o valor é extraído automaticamente via OCR e preenchido no campo.

### Fluxo proposto

1. No `AddItemSheet`, ícone de câmera ao lado do campo "PREÇO R$ (OPCIONAL)"
2. Toque → solicita permissão de câmera se necessário → abre câmera do sistema
3. Foto capturada → ML Kit processa localmente (~200–500ms) → extrai candidatos a preço
4. Se um único candidato claro → preenche o campo diretamente
5. Se múltiplos candidatos → exibe opções para o usuário escolher (bottom sheet ou dialog)
6. Se nenhum preço encontrado → exibe Toast de aviso

### Componentes

| Parte | Complexidade | Detalhe |
|---|---|---|
| Captura de foto | Baixa | `ActivityResultLauncher` com `TakePicture` — intent do sistema, sem CameraX |
| OCR | Baixa | ML Kit Text Recognition — on-device, gratuito, sem rede |
| Extração do preço | Média-Alta | Regex + heurísticas para desambiguar múltiplos preços na etiqueta |
| Permissão de câmera | Trivial | `Manifest.permission.CAMERA` |
| UI | Baixa | Ícone no campo de preço + loading + confirmação |

### Dependência nova
```toml
# libs.versions.toml
mlkitTextRecognition = "16.x.x"
```
```kotlin
// build.gradle.kts
implementation(libs.mlkit.text.recognition) // ~6MB no APK
```

### Principal risco

Etiquetas de supermercado no Brasil variam muito entre redes — preço por kg, preço total, preço à vista vs parcelado, promoções com múltiplos valores. Preencher silenciosamente o valor errado seria uma experiência ruim.

**Recomendação:** nunca preencher e fechar automaticamente. Sempre mostrar o(s) candidato(s) e deixar o usuário confirmar com um toque. Validar com fotos reais de diferentes redes antes de definir o fluxo de confirmação.

### Arquivos que seriam modificados
- `AddItemSheet.kt` — ícone de câmera, estado de loading, exibição de candidatos
- `ItensViewModel.kt` — lógica de disparo e recebimento do resultado da câmera
- `AndroidManifest.xml` — permissão `CAMERA`
