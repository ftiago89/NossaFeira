package com.example.nossafeira.ui.screens.listas

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nossafeira.ui.components.AddListaSheet
import com.example.nossafeira.ui.components.ListaCard
import com.example.nossafeira.ui.theme.Background
import com.example.nossafeira.ui.theme.Border
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.OnPrimary
import com.example.nossafeira.ui.theme.Primary
import com.example.nossafeira.ui.theme.Surface
import com.example.nossafeira.ui.theme.TextPrimary
import com.example.nossafeira.ui.theme.TextSecondary
import com.example.nossafeira.ui.theme.TextTertiary
import com.example.nossafeira.viewmodel.ListasViewModel
import com.example.nossafeira.viewmodel.SyncEvento

@Composable
fun ListasScreen(onListaClick: (Int) -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: ListasViewModel = viewModel(factory = ListasViewModel.factory(application))

    val listas by viewModel.listas.collectAsStateWithLifecycle()
    val busca by viewModel.busca.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    var mostrarAddSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncEvento.collect { evento ->
            val mensagem = when (evento) {
                SyncEvento.Compartilhada -> "Lista compartilhada com sucesso."
                SyncEvento.Sincronizada  -> "Lista sincronizada com sucesso."
                SyncEvento.Conflito      -> "Lista atualizada. Suas alterações locais foram substituídas."
                SyncEvento.ErroRede      -> "Falha na sincronização. Verifique sua conexão."
                SyncEvento.ListaDeletada -> "A lista foi removida pelo outro membro e voltou a ser local."
                SyncEvento.PullConcluido -> "Listas sincronizadas com sucesso."
            }
            Toast.makeText(context, mensagem, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            ListasTopBar(
                isSyncing = isSyncing,
                onSync = { viewModel.sincronizarTodas() }
            )
        },
        floatingActionButton = {
            NossaFeiraFab(onClick = { mostrarAddSheet = true })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(Modifier.height(12.dp))

            ListasBuscaBar(
                value = busca,
                onValueChange = { viewModel.atualizarBusca(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (listas.isEmpty()) {
                ListasEstadoVazio(
                    temBusca = busca.isNotBlank(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = listas,
                        key = { it.lista.id }
                    ) { listaComItens ->
                        ListaCard(
                            listaComItens = listaComItens,
                            onClick = { onListaClick(listaComItens.lista.id) },
                            onDelete = { viewModel.deletarLista(listaComItens) },
                            onCompartilhar = { viewModel.compartilharLista(listaComItens) },
                            onSincronizar = { viewModel.sincronizarLista(listaComItens) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        )
                    }
                }
            }
        }
    }

    if (mostrarAddSheet) {
        AddListaSheet(
            onDismiss = { mostrarAddSheet = false },
            onConfirm = { nome, valorEstimado ->
                viewModel.criarLista(nome, valorEstimado)
                mostrarAddSheet = false
            }
        )
    }
}

// ── TopBar ────────────────────────────────────────────────────────────────────

@Composable
private fun ListasTopBar(
    isSyncing: Boolean,
    onSync: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "NossaFeira \uD83D\uDED2",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .clickable(enabled = !isSyncing, onClick = onSync),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Sincronizar listas",
                tint = if (isSyncing) Primary else TextSecondary,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (isSyncing) rotation else 0f
                }
            )
        }
    }
}

// ── SearchBar ─────────────────────────────────────────────────────────────────

@Composable
private fun ListasBuscaBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "Buscar lista...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextTertiary
                    )
                }
                innerTextField()
            }
        )
    }
}

// ── FAB ───────────────────────────────────────────────────────────────────────

@Composable
internal fun NossaFeiraFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = Primary,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Adicionar",
            modifier = Modifier.size(24.dp)
        )
    }
}

// ── Estado vazio ──────────────────────────────────────────────────────────────

@Composable
private fun ListasEstadoVazio(
    temBusca: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (temBusca) "\uD83D\uDD0D" else "\uD83D\uDED2",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(
                        40f, androidx.compose.ui.unit.TextUnitType.Sp
                    )
                )
            )
            Text(
                text = if (temBusca) "Nenhuma lista encontrada" else "Nenhuma lista ainda",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (temBusca) "Tente outro termo de busca" else "Toque no + para criar sua primeira lista",
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
private fun ListasTopBarPreview() {
    NossaFeiraTheme {
        ListasTopBar(isSyncing = false, onSync = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "TopBar - sincronizando")
@Composable
private fun ListasTopBarSyncingPreview() {
    NossaFeiraTheme {
        ListasTopBar(isSyncing = true, onSync = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun ListasBuscaBarPreview() {
    NossaFeiraTheme {
        ListasBuscaBar(
            value = "",
            onValueChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun ListasEstadoVazioPreview() {
    NossaFeiraTheme {
        ListasEstadoVazio(
            temBusca = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "Estado vazio - sem resultado")
@Composable
private fun ListasEstadoVazioBuscaPreview() {
    NossaFeiraTheme {
        ListasEstadoVazio(
            temBusca = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "FAB")
@Composable
private fun NossaFeiraFabPreview() {
    NossaFeiraTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NossaFeiraFab(onClick = {})
        }
    }
}
