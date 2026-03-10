package com.example.nossafeira.ui.screens.itens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.ui.components.AddItemSheet
import com.example.nossafeira.ui.components.FilterChips
import com.example.nossafeira.ui.components.ItemCard
import com.example.nossafeira.ui.components.SummaryCard
import com.example.nossafeira.ui.screens.listas.NossaFeiraFab
import com.example.nossafeira.ui.theme.Background
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.Surface
import com.example.nossafeira.ui.theme.TextPrimary
import com.example.nossafeira.ui.theme.TextSecondary
import com.example.nossafeira.ui.theme.TextTertiary
import com.example.nossafeira.ui.utils.calcularTotalGasto
import com.example.nossafeira.ui.utils.extrairQuantidadeNumerica
import com.example.nossafeira.viewmodel.ItensViewModel

@Composable
fun ItensScreen(
    listaId: Int,
    onBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ItensViewModel = viewModel(
        key = "itens_$listaId",
        factory = ItensViewModel.factory(application, listaId)
    )

    val listaComItens by viewModel.listaComItens.collectAsStateWithLifecycle()
    val itensFiltrados by viewModel.itensFiltrados.collectAsStateWithLifecycle()
    val filtroCategoria by viewModel.filtroCategoria.collectAsStateWithLifecycle()
    var mostrarAddSheet by remember { mutableStateOf(false) }
    var itemParaEditar by remember { mutableStateOf<ItemFeira?>(null) }

    val totalItens = listaComItens?.itens?.size ?: 0
    val itensComprados = listaComItens?.itens?.count { it.comprado } ?: 0
    val totalGasto = calcularTotalGasto(listaComItens?.itens ?: emptyList())

    Scaffold(
        containerColor = Background,
        topBar = {
            ItensTopBar(
                titulo = listaComItens?.lista?.nome ?: "",
                onBack = onBack
            )
        },
        floatingActionButton = {
            NossaFeiraFab(onClick = { mostrarAddSheet = true })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                SummaryCard(
                    totalItens = totalItens,
                    itensComprados = itensComprados,
                    totalGasto = totalGasto,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                FilterChips(
                    filtroAtivo = filtroCategoria,
                    onFiltroChange = { viewModel.setFiltro(it) }
                )
                Spacer(Modifier.height(16.dp))
            }

            if (itensFiltrados.isEmpty()) {
                item {
                    ItensEstadoVazio(
                        temFiltro = filtroCategoria != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp)
                    )
                }
            } else {
                items(
                    items = itensFiltrados,
                    key = { it.id }
                ) { item ->
                    ItemCard(
                        item = item,
                        onToggleComprado = { viewModel.toggleComprado(item) },
                        onDelete = { viewModel.deletarItem(item) },
                        onLongClick = { itemParaEditar = item },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem()
                    )
                }
            }
        }
    }

    if (mostrarAddSheet) {
        AddItemSheet(
            onDismiss = { mostrarAddSheet = false },
            onConfirm = { nome, quantidade, categoria, preco ->
                viewModel.adicionarItem(nome, quantidade, categoria, preco)
                mostrarAddSheet = false
            }
        )
    }

    itemParaEditar?.let { item ->
        AddItemSheet(
            onDismiss = { itemParaEditar = null },
            onConfirm = { nome, quantidade, categoria, preco ->
                viewModel.editarItem(item, nome, quantidade, categoria, preco)
                itemParaEditar = null
            },
            itemParaEditar = item
        )
    }
}

// ── TopBar ────────────────────────────────────────────────────────────────────

@Composable
private fun ItensTopBar(
    titulo: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onBack) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TextSecondary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Lista de compras",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

// ── Estado vazio ──────────────────────────────────────────────────────────────

@Composable
private fun ItensEstadoVazio(
    temFiltro: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (temFiltro) "\uD83D\uDD0D" else "\uD83D\uDED2",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(
                        40f, androidx.compose.ui.unit.TextUnitType.Sp
                    )
                )
            )
            Text(
                text = if (temFiltro) "Nenhum item nessa categoria" else "Nenhum item ainda",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (temFiltro) "Selecione outro filtro ou adicione um item" else "Toque no + para adicionar o primeiro item",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun ItensTopBarPreview() {
    NossaFeiraTheme {
        ItensTopBar(titulo = "Feira da semana", onBack = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun ItensEstadoVazioPreview() {
    NossaFeiraTheme {
        ItensEstadoVazio(
            temFiltro = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "Estado vazio - filtro ativo")
@Composable
private fun ItensEstadoVazioFiltroPreview() {
    NossaFeiraTheme {
        ItensEstadoVazio(
            temFiltro = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "SummaryCard inline")
@Composable
private fun ItensSummaryPreview() {
    NossaFeiraTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryCard(totalItens = 10, itensComprados = 3, totalGasto = 3250)
            Spacer(Modifier.height(12.dp))
            FilterChips(
                filtroAtivo = Categoria.LATICINIOS,
                onFiltroChange = {}
            )
        }
    }
}
