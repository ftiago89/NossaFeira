# NossaFeira — Release Notes

---

## v1.2.2 — 2026-03-28

### Correções

- **Listas duplicadas ao reinstalar**: desabilitado o auto backup do Android (`allowBackup=false`) para evitar que o banco Room restaurado gere cópias locais de listas já compartilhadas.

---

## v1.2.1 — 2026-03-28

### Novidades

- **Categoria pré-selecionada**: ao adicionar um novo item, a categoria "Outros" já vem selecionada por padrão.
- **Ordenação alfabética**: itens na lista agora são exibidos em ordem alfabética (case-insensitive).
- **Compartilhar/sincronizar na tela de itens**: botão na top bar da tela de itens — compartilha a lista se ainda for local, ou sincroniza se já for compartilhada. Mesmo padrão visual da tela de listas (animação de rotação durante sync).
- **Feedback visual de compartilhamento**: spinner substitui o ícone de share enquanto a operação está em andamento (tanto na tela de listas quanto na de itens). Bloqueia cliques concorrentes.
- **Feedback visual de deleção**: spinner substitui o ícone de lixeira no ListaCard enquanto a deleção está em andamento.

### Interno

- `ItensViewModel` expandido com `compartilharLista()`, `sincronizarLista()`, `isSyncing`, `isSharing` e `syncEvento`.
- `ListasViewModel` expandido com `sharingIds` e `deletingIds` para feedback visual granular por lista.
- `ItensViewModelTest` expandido para 27 casos (sync, share, ordenação).
- `ListasViewModelTest` expandido para 21 casos (share, delete, feedback visual).
- CLAUDE.md atualizado com novas features e padrão de teste para `WhileSubscribed` StateFlow.

---

## v1.2.0 — 2026-03-28

### Novidades

- **Preenchimento de preço por foto**: botão de câmera ao lado do campo de preço no formulário de item. Tira foto da etiqueta, ML Kit extrai o texto e os preços candidatos são sugeridos automaticamente (1 candidato preenche direto, 2+ exibem chips de escolha).
- **Entrada por voz para nome do item**: botão de microfone ao lado do campo de nome. Lança o reconhecimento de fala do sistema (pt-BR) e preenche o campo automaticamente.
- **Quantidade pré-preenchida**: campo quantidade agora inicia com "1" ao adicionar um novo item.
- **Merge three-way na sincronização**: sincronização de listas compartilhadas agora usa snapshots por item (`sync*` fields) para merge three-way, evitando perda de edições concorrentes entre dispositivos.

### Correções

- Swipe-to-delete removido de ItemCard e ListaCard — deleção agora apenas pelo botão de lixeira, evitando exclusões acidentais.

### Interno

- `PrecoOcrExtractor.kt` adicionado: função pura para extrair preços de texto OCR (19 testes unitários).
- `NossaFeiraRepositoryTest` expandido para 21 casos (10 cenários de merge three-way + 5 de pullStartup).
- Migration `MIGRATION_5_6` adicionada. Banco na versão 6.

---

## v1.1.0 — 2026-03-12

### Novidades

- **Compartilhamento familiar de listas**: listas podem ser compartilhadas com membros da família via backend (AWS Lambda + MongoDB Atlas). O usuário decide quais listas compartilhar pelo botão no ListaCard.
- **Sincronização manual global**: botão Refresh (↺) na TopBar sincroniza todas as listas compartilhadas de uma vez, com animação de loading e feedback via Toast.
- **Sincronização por lista**: ícone de sync individual em cada ListaCard compartilhado.
- **Editar lista**: long press no ListaCard abre o formulário de edição com nome e valor estimado pré-preenchidos.
- **Data de criação no ListaCard**: exibida abaixo do nome da lista para facilitar identificação visual.

### Correções

- Bugs de sincronização no `pullStartup` corrigidos: listas removidas remotamente voltam a ser locais corretamente; `syncedAt` passa a usar o timestamp do servidor para evitar clock skew entre dispositivos.
- Bugs na sincronização manual global corrigidos.

### Interno

- Testes unitários adicionados: `ListasViewModelTest` (14 casos), `ItensViewModelTest` (17 casos), `NossaFeiraRepositoryTest` (18 casos), `PrecoUtilsTest` (18 casos).
- Testes de instrumentação adicionados: `ListaFeiraDaoTest` (10 casos), `ItemFeiraDaoTest` (5 casos), `NossaFeiraDatabaseMigrationTest` (2 casos).
- Injeção de dependência dos ViewModels refatorada para suportar testabilidade.
- Migrations `MIGRATION_3_4` e `MIGRATION_4_5` adicionadas. Banco na versão 5.

---

## v1.0.0 — lançamento inicial

### Funcionalidades

- Criação e exclusão de listas de compras.
- Adição, edição e exclusão de itens por lista.
- Categorias de itens: Hortifruti, Laticínios, Limpeza, Outros, Proteínas, Padaria.
- Preço individual por item; total gasto calculado automaticamente com base nos itens comprados.
- Valor estimado por lista.
- Filtro de itens por categoria.
- Marcar itens como comprados com feedback tátil e progress bar.
- Swipe para deletar listas. Itens deletados pelo botão de lixeira.
- Valores monetários armazenados em centavos (inteiros).
- Tema dark com paleta de cores customizada (Plus Jakarta Sans).
