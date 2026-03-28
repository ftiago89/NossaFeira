package com.example.nossafeira.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import com.example.nossafeira.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import kotlin.math.roundToInt
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.ui.theme.Border
import com.example.nossafeira.ui.theme.Green
import com.example.nossafeira.ui.theme.GreenDim
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.OnPrimary
import com.example.nossafeira.ui.theme.Orange
import com.example.nossafeira.ui.theme.OrangeDim
import com.example.nossafeira.ui.theme.Primary
import com.example.nossafeira.ui.theme.PrimaryContainer
import com.example.nossafeira.ui.theme.PrimaryDim
import com.example.nossafeira.ui.theme.Pink
import com.example.nossafeira.ui.theme.PinkDim
import com.example.nossafeira.ui.theme.Purple
import com.example.nossafeira.ui.theme.PurpleDim
import com.example.nossafeira.ui.theme.Yellow
import com.example.nossafeira.ui.theme.YellowDim
import com.example.nossafeira.ui.theme.Surface
import com.example.nossafeira.ui.theme.Surface2
import com.example.nossafeira.ui.theme.Surface3
import com.example.nossafeira.ui.theme.TextPrimary
import com.example.nossafeira.ui.theme.TextSecondary
import com.example.nossafeira.ui.theme.TextTertiary

private data class CategoriaOption(
    val categoria: Categoria,
    val emoji: String,
    val label: String,
    val cor: Color,
    val corDim: Color
)

private val categoriaOptions = listOf(
    CategoriaOption(Categoria.HORTIFRUTI, "🥬", "Hortifruti", Green,   GreenDim),
    CategoriaOption(Categoria.LATICINIOS, "🥛", "Laticínios", Primary, PrimaryContainer),
    CategoriaOption(Categoria.LIMPEZA,    "🧹", "Limpeza",    Orange,  OrangeDim),
    CategoriaOption(Categoria.OUTROS,     "📦", "Outros",     Purple,  PurpleDim),
    CategoriaOption(Categoria.PROTEINAS,  "🥩", "Proteínas",  Pink,    PinkDim),
    CategoriaOption(Categoria.PADARIA,    "🍞", "Padaria",    Yellow,  YellowDim),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onConfirm: (nome: String, quantidade: String, categoria: Categoria, preco: Int) -> Unit,
    itemParaEditar: ItemFeira? = null,
    onCameraRequest: () -> Unit = {},
    precoSugeridos: List<Int> = emptyList(),
    isProcessandoOcr: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetHandle() }
    ) {
        AddItemSheetContent(
            onConfirm = onConfirm,
            nomeInicial = itemParaEditar?.nome ?: "",
            quantidadeInicial = itemParaEditar?.quantidade ?: "1",
            precoInicial = if ((itemParaEditar?.preco ?: 0) > 0)
                "%.2f".format(itemParaEditar!!.preco / 100.0).replace('.', ',') else "",
            categoriaInicial = itemParaEditar?.categoria,
            modoEdicao = itemParaEditar != null,
            onCameraRequest = onCameraRequest,
            precoSugeridos = precoSugeridos,
            isProcessandoOcr = isProcessandoOcr
        )
    }
}

@Composable
private fun AddItemSheetContent(
    onConfirm: (nome: String, quantidade: String, categoria: Categoria, preco: Int) -> Unit,
    nomeInicial: String = "",
    quantidadeInicial: String = "",
    precoInicial: String = "",
    categoriaInicial: Categoria? = null,
    modoEdicao: Boolean = false,
    onCameraRequest: () -> Unit = {},
    precoSugeridos: List<Int> = emptyList(),
    isProcessandoOcr: Boolean = false
) {
    var nome by remember { mutableStateOf(nomeInicial) }
    var quantidade by remember { mutableStateOf(quantidadeInicial) }
    var precoTexto by remember { mutableStateOf(precoInicial) }
    var categoriaEscolhida by remember { mutableStateOf(categoriaInicial) }
    var nomeFocado by remember { mutableStateOf(false) }
    var quantidadeFocada by remember { mutableStateOf(false) }
    var precoFocado by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.ime)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (modoEdicao) "Editar item" else "Adicionar item",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        // Campo nome
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CampoLabel("NOME DO ITEM")
            CampoTexto(
                value = nome,
                onValueChange = { nome = it },
                placeholder = "Ex: Alface crespa",
                focado = nomeFocado,
                onFocusChange = { nomeFocado = it },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )
        }

        // Campo quantidade
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CampoLabel("QUANTIDADE")
            CampoTexto(
                value = quantidade,
                onValueChange = { quantidade = it },
                placeholder = "Ex: 1, 2, 0,5",
                focado = quantidadeFocada,
                onFocusChange = { quantidadeFocada = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
        }

        // Campo preço (opcional)
        // Quando há 1 candidato vindo do OCR, preenche o campo automaticamente
        LaunchedEffect(precoSugeridos) {
            if (precoSugeridos.size == 1) {
                precoTexto = "%.2f".format(precoSugeridos[0] / 100.0).replace('.', ',')
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CampoLabel("PREÇO R$ (OPCIONAL)")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CampoTexto(
                    value = precoTexto,
                    onValueChange = { precoTexto = it },
                    placeholder = "0,00",
                    focado = precoFocado,
                    onFocusChange = { precoFocado = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface2)
                        .border(1.5.dp, Border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessandoOcr) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onCameraRequest) {
                            Icon(
                                painter = painterResource(R.drawable.ic_camera_alt),
                                contentDescription = "Fotografar etiqueta de preço",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Chips de candidatos (exibidos quando OCR retorna 2+ preços)
            if (precoSugeridos.size >= 2) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(precoSugeridos) { centavos ->
                        val label = "R$ ${"%.2f".format(centavos / 100.0).replace('.', ',')}"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Surface3)
                                .border(1.5.dp, Border, RoundedCornerShape(20.dp))
                                .clickable {
                                    precoTexto = "%.2f".format(centavos / 100.0).replace('.', ',')
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Grade de categorias 2×2
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CampoLabel("CATEGORIA")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categoriaOptions.chunked(2).forEach { linha ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        linha.forEach { option ->
                            val selecionada = categoriaEscolhida == option.categoria
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selecionada) option.corDim else Surface2)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (selecionada) option.cor else Border,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { categoriaEscolhida = option.categoria }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = option.emoji, fontSize = 18.sp)
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selecionada) option.cor else TextSecondary
                                    )
                                }
                            }
                        }
                        // Completa a última linha se houver número ímpar de opções
                        repeat(2 - linha.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val podeSalvar = nome.isNotBlank() && categoriaEscolhida != null
        Button(
            onClick = {
                val preco = ((precoTexto.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).roundToInt()
                onConfirm(nome.trim(), quantidade.trim(), categoriaEscolhida!!, preco)
            },
            enabled = podeSalvar,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = OnPrimary,
                disabledContainerColor = PrimaryDim,
                disabledContentColor = TextTertiary
            )
        ) {
            Text(
                text = if (modoEdicao) "Salvar alterações" else "Adicionar item",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A1D27)
@Composable
private fun AddItemSheetPreview() {
    NossaFeiraTheme {
        Surface(color = Surface) {
            AddItemSheetContent(onConfirm = { _, _, _, _ -> })
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1D27, name = "AddItemSheet - Categoria selecionada")
@Composable
private fun AddItemSheetComSelecaoPreview() {
    NossaFeiraTheme {
        Surface(color = Surface) {
            AddItemSheetContent(
                nomeInicial = "Alface crespa",
                quantidadeInicial = "2 un",
                categoriaInicial = Categoria.HORTIFRUTI,
                onConfirm = { _, _, _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1D27, name = "AddItemSheet - Outros")
@Composable
private fun AddItemSheetOutrosPreview() {
    NossaFeiraTheme {
        Surface(color = Surface) {
            AddItemSheetContent(
                nomeInicial = "Arroz",
                quantidadeInicial = "5 kg",
                categoriaInicial = Categoria.OUTROS,
                onConfirm = { _, _, _, _ -> }
            )
        }
    }
}
