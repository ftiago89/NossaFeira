package com.example.nossafeira.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nossafeira.ui.theme.Green
import com.example.nossafeira.ui.theme.GreenDim
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.Primary
import com.example.nossafeira.ui.theme.PrimaryContainer
import com.example.nossafeira.ui.theme.PrimaryDim
import com.example.nossafeira.ui.theme.Surface3
import com.example.nossafeira.ui.theme.TextPrimary
import com.example.nossafeira.ui.theme.TextSecondary

@Composable
fun SummaryCard(
    totalItens: Int,
    itensComprados: Int,
    totalGasto: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val progresso = if (totalItens > 0) itensComprados.toFloat() / totalItens else 0f

    val gradienteFundo = Brush.linearGradient(
        colors = listOf(PrimaryContainer, Color(0xFF111827)),
        // 135° → de cima-direita para baixo-esquerda
        start = Offset(Float.POSITIVE_INFINITY, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    )
    val gradienteProgresso = Brush.horizontalGradient(listOf(Primary, Green))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradienteFundo)
            .border(1.dp, PrimaryDim, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Itens na lista",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenDim)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$itensComprados comprados",
                        style = MaterialTheme.typography.bodySmall,
                        color = Green
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$totalItens",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = TextPrimary
                )
                if (totalGasto > 0.0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GreenDim)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "R$ %.2f".format(totalGasto),
                            style = MaterialTheme.typography.bodySmall,
                            color = Green
                        )
                    }
                }
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
                        .background(gradienteProgresso)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117)
@Composable
private fun SummaryCardPreview() {
    NossaFeiraTheme {
        SummaryCard(
            totalItens = 12,
            itensComprados = 5,
            totalGasto = 47.90,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "SummaryCard - Completo")
@Composable
private fun SummaryCardCompletoPreview() {
    NossaFeiraTheme {
        SummaryCard(
            totalItens = 8,
            itensComprados = 8,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1117, name = "SummaryCard - Vazio")
@Composable
private fun SummaryCardVazioPreview() {
    NossaFeiraTheme {
        SummaryCard(
            totalItens = 0,
            itensComprados = 0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
