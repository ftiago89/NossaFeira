package com.example.nossafeira.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.model.ListaFeira
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.nossafeira.ui.utils.calcularTotalGasto
import com.example.nossafeira.ui.theme.Border
import com.example.nossafeira.ui.theme.Green
import com.example.nossafeira.ui.theme.GreenDim
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.Orange
import com.example.nossafeira.ui.theme.Pink
import com.example.nossafeira.ui.theme.Primary
import com.example.nossafeira.ui.theme.Surface
import com.example.nossafeira.ui.theme.Surface3
import com.example.nossafeira.ui.theme.TextPrimary
import com.example.nossafeira.ui.theme.TextSecondary
import com.example.nossafeira.ui.theme.TextTertiary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListaCard(
    listaComItens: ListaComItens,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCompartilhar: () -> Unit = {},
    onSincronizar: () -> Unit = {},
    onEditar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val total = listaComItens.itens.size
    val comprados = listaComItens.itens.count { it.comprado }
    val progresso = if (total > 0) comprados.toFloat() / total else 0f
    val totalGasto = calcularTotalGasto(listaComItens.itens)

    val dataCriacao = SimpleDateFormat("dd MMM. yyyy", Locale("pt", "BR"))
        .format(Date(listaComItens.lista.criadaEm))

    val corAlvo = when {
        progresso >= 0.67f -> Green
        progresso >= 0.34f -> Primary
        else -> Orange
    }
    val corBarra by animateColorAsState(targetValue = corAlvo, label = "corBarra")
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Área principal — clicável para navegar, long press para editar
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEditar()
                    }
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = listaComItens.lista.nome,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = dataCriacao,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )

            Text(
                text = "$comprados de $total itens comprados",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (listaComItens.lista.valorEstimado > 0) {
                Text(
                    text = "Estimado: R$ %.2f".format(listaComItens.lista.valorEstimado / 100.0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }

            if (totalGasto > 0) {
                Text(
                    text = "Gasto: R$ %.2f".format(totalGasto / 100.0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Green
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Surface3)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progresso.coerceIn(0f, 1f))
                        .background(corBarra)
                )
            }
        }

        // Botões de ação
        Column(
            modifier = Modifier.padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (listaComItens.lista.isShared) {
                IconButton(
                    onClick = onSincronizar,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sincronizar lista",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onCompartilhar,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartilhar lista",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Deletar lista",
                    tint = Pink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun ListaCardPreview() {
    NossaFeiraTheme {
        ListaCard(
            listaComItens = ListaComItens(
                lista = ListaFeira(id = 1, nome = "Feira da semana", valorEstimado = 15000),
                itens = listOf(
                    ItemFeira(1, 1, "Alface", "1 un", Categoria.HORTIFRUTI, preco = 299, comprado = true),
                    ItemFeira(2, 1, "Leite", "2 L", Categoria.LATICINIOS, preco = 599, comprado = true),
                    ItemFeira(3, 1, "Sabão", "1 kg", Categoria.LIMPEZA, comprado = false),
                    ItemFeira(4, 1, "Arroz", "5 kg", Categoria.OUTROS, comprado = false),
                )
            ),
            onClick = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "ListaCard - Concluída")
@Composable
private fun ListaCardConcluidaPreview() {
    NossaFeiraTheme {
        ListaCard(
            listaComItens = ListaComItens(
                lista = ListaFeira(id = 2, nome = "Mercado mensal"),
                itens = listOf(
                    ItemFeira(1, 2, "Alface", "1 un", Categoria.HORTIFRUTI, comprado = true),
                    ItemFeira(2, 2, "Leite", "2 L", Categoria.LATICINIOS, comprado = true),
                )
            ),
            onClick = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "ListaCard - Vazia")
@Composable
private fun ListaCardVaziaPreview() {
    NossaFeiraTheme {
        ListaCard(
            listaComItens = ListaComItens(
                lista = ListaFeira(id = 3, nome = "Lista nova"),
                itens = emptyList()
            ),
            onClick = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
