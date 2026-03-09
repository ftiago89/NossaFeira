package com.example.nossafeira.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.ui.theme.Border
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.Primary
import com.example.nossafeira.ui.theme.PrimaryContainer
import com.example.nossafeira.ui.theme.TextSecondary

private data class ChipOption(val label: String, val categoria: Categoria?)

private val chipOptions = listOf(
    ChipOption("Todos", null),
    ChipOption("🥬 Hortifruti", Categoria.HORTIFRUTI),
    ChipOption("🥛 Laticínios", Categoria.LATICINIOS),
    ChipOption("🧹 Limpeza", Categoria.LIMPEZA),
    ChipOption("📦 Outros", Categoria.OUTROS),
    ChipOption("🥩 Proteínas", Categoria.PROTEINAS),
    ChipOption("🍞 Padaria", Categoria.PADARIA),
)

@Composable
fun FilterChips(
    filtroAtivo: Categoria?,
    onFiltroChange: (Categoria?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(chipOptions) { option ->
            val ativo = filtroAtivo == option.categoria
            Surface(
                onClick = { onFiltroChange(option.categoria) },
                shape = RoundedCornerShape(20.dp),
                color = if (ativo) PrimaryContainer else Color.Transparent,
                border = BorderStroke(1.5.dp, if (ativo) Primary else Border)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ativo) Primary else TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun FilterChipsNenhumAtivoPreview() {
    NossaFeiraTheme {
        FilterChips(
            filtroAtivo = null,
            onFiltroChange = {},
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "FilterChips - Ativo")
@Composable
private fun FilterChipsAtivoPreview() {
    NossaFeiraTheme {
        FilterChips(
            filtroAtivo = Categoria.HORTIFRUTI,
            onFiltroChange = {},
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
