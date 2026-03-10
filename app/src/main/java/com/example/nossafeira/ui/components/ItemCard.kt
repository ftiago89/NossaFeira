package com.example.nossafeira.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.ui.theme.Border
import com.example.nossafeira.ui.theme.Green
import com.example.nossafeira.ui.theme.GreenDim
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.Orange
import com.example.nossafeira.ui.theme.Pink
import com.example.nossafeira.ui.theme.PinkDim
import com.example.nossafeira.ui.theme.Primary
import com.example.nossafeira.ui.theme.Yellow
import com.example.nossafeira.ui.theme.YellowDim
import com.example.nossafeira.ui.theme.Purple
import com.example.nossafeira.ui.theme.Surface
import com.example.nossafeira.ui.theme.Surface2
import com.example.nossafeira.ui.theme.TextPrimary
import com.example.nossafeira.ui.theme.TextSecondary
import com.example.nossafeira.ui.theme.TextTertiary

private fun Categoria.cor() = when (this) {
    Categoria.HORTIFRUTI -> Green
    Categoria.LATICINIOS -> Primary
    Categoria.LIMPEZA    -> Orange
    Categoria.OUTROS     -> Purple
    Categoria.PROTEINAS  -> Pink
    Categoria.PADARIA    -> Yellow
}

private fun Categoria.emoji() = when (this) {
    Categoria.HORTIFRUTI -> "🥬"
    Categoria.LATICINIOS -> "🥛"
    Categoria.LIMPEZA    -> "🧹"
    Categoria.OUTROS     -> "📦"
    Categoria.PROTEINAS  -> "🥩"
    Categoria.PADARIA    -> "🍞"
}

private fun Categoria.label() = when (this) {
    Categoria.HORTIFRUTI -> "Hortifruti"
    Categoria.LATICINIOS -> "Laticínios"
    Categoria.LIMPEZA    -> "Limpeza"
    Categoria.OUTROS     -> "Outros"
    Categoria.PROTEINAS  -> "Proteínas"
    Categoria.PADARIA    -> "Padaria"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemCard(
    item: ItemFeira,
    onToggleComprado: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PinkDim),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Deletar item",
                    tint = Pink,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .alpha(if (item.comprado) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra lateral colorida
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(72.dp)
                    .background(item.categoria.cor())
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Checkbox customizado
                val checkboxBg by animateColorAsState(
                    targetValue = if (item.comprado) Green else Color.Transparent,
                    label = "checkboxBg"
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(checkboxBg)
                        .border(2.dp, if (item.comprado) Green else Border, RoundedCornerShape(8.dp))
                        .clickable {
                            if (!item.comprado) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleComprado()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.comprado) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Emoji box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.categoria.emoji(), fontSize = 26.sp)
                }

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        textDecoration = if (item.comprado) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.categoria.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }

                // Badges de quantidade e preço
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface2)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.quantidade,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    if (item.preco > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenDim)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "R$ ${"%.2f".format(item.preco / 100.0).replace('.', ',')}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Green
                            )
                        }
                    }
                }

                // Botão de exclusão
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deletar item",
                        tint = Pink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun ItemCardHortifrutiPreview() {
    NossaFeiraTheme {
        ItemCard(
            item = ItemFeira(1, 1, "Alface crespa", "2 un", Categoria.HORTIFRUTI, preco = 350),
            onToggleComprado = {},
            onDelete = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "ItemCard - Comprado")
@Composable
private fun ItemCardCompradoPreview() {
    NossaFeiraTheme {
        ItemCard(
            item = ItemFeira(2, 1, "Leite integral", "2 L", Categoria.LATICINIOS, comprado = true),
            onToggleComprado = {},
            onDelete = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "ItemCard - Limpeza")
@Composable
private fun ItemCardLimpezaPreview() {
    NossaFeiraTheme {
        ItemCard(
            item = ItemFeira(3, 1, "Detergente líquido", "500 ml", Categoria.LIMPEZA),
            onToggleComprado = {},
            onDelete = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "ItemCard - Outros")
@Composable
private fun ItemCardOutrosPreview() {
    NossaFeiraTheme {
        ItemCard(
            item = ItemFeira(4, 1, "Arroz tipo 1", "5 kg", Categoria.OUTROS),
            onToggleComprado = {},
            onDelete = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
