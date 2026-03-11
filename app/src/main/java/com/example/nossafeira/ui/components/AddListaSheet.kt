package com.example.nossafeira.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import kotlin.math.roundToInt
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nossafeira.ui.theme.Border
import com.example.nossafeira.ui.theme.NossaFeiraTheme
import com.example.nossafeira.ui.theme.OnPrimary
import com.example.nossafeira.ui.theme.Primary
import com.example.nossafeira.ui.theme.PrimaryDim
import com.example.nossafeira.ui.theme.Surface
import com.example.nossafeira.ui.theme.Surface2
import com.example.nossafeira.ui.theme.Surface3
import com.example.nossafeira.ui.theme.TextPrimary
import com.example.nossafeira.ui.theme.TextSecondary
import com.example.nossafeira.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListaSheet(
    onDismiss: () -> Unit,
    onConfirm: (nome: String, valorEstimado: Int) -> Unit,
    nomeInicial: String = "",
    valorInicial: String = "",
    modoEdicao: Boolean = false
) {
    var nome by remember { mutableStateOf(nomeInicial) }
    var valorTexto by remember { mutableStateOf(valorInicial) }
    var nomeFocado by remember { mutableStateOf(false) }
    var valorFocado by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetHandle() }
    ) {
        AddListaSheetContent(
            nome = nome,
            valorTexto = valorTexto,
            nomeFocado = nomeFocado,
            valorFocado = valorFocado,
            modoEdicao = modoEdicao,
            onNomeChange = { nome = it },
            onValorChange = { valorTexto = it },
            onNomeFocusChange = { nomeFocado = it },
            onValorFocusChange = { valorFocado = it },
            onConfirm = {
                val valor = ((valorTexto.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).roundToInt()
                onConfirm(nome.trim(), valor)
            }
        )
    }
}

@Composable
private fun AddListaSheetContent(
    nome: String,
    valorTexto: String,
    nomeFocado: Boolean,
    valorFocado: Boolean,
    modoEdicao: Boolean = false,
    onNomeChange: (String) -> Unit,
    onValorChange: (String) -> Unit,
    onNomeFocusChange: (Boolean) -> Unit,
    onValorFocusChange: (Boolean) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (modoEdicao) "Editar lista" else "Nova lista",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CampoLabel("NOME DA LISTA")
            CampoTexto(
                value = nome,
                onValueChange = onNomeChange,
                placeholder = "Ex: Feira da semana",
                focado = nomeFocado,
                onFocusChange = onNomeFocusChange,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CampoLabel("VALOR ESTIMADO R$ (OPCIONAL)")
            CampoTexto(
                value = valorTexto,
                onValueChange = onValorChange,
                placeholder = "0,00",
                focado = valorFocado,
                onFocusChange = onValorFocusChange,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            )
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onConfirm,
            enabled = nome.isNotBlank(),
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
                text = if (modoEdicao) "Salvar alterações" else "Criar lista",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Shared helpers (internal → reutilizados em AddItemSheet) ──────────────────

@Composable
internal fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Surface3)
    )
}

@Composable
internal fun CampoLabel(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary
    )
}

@Composable
internal fun CampoTexto(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focado: Boolean,
    onFocusChange: (Boolean) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(
                width = 1.5.dp,
                color = if (focado) Primary else Border,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { onFocusChange(it.isFocused) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
        singleLine = true,
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextTertiary
                )
            }
            innerTextField()
        }
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A1D27)
@Composable
private fun AddListaSheetPreview() {
    NossaFeiraTheme {
        Surface(color = Surface) {
            AddListaSheetContent(
                nome = "",
                valorTexto = "",
                nomeFocado = false,
                valorFocado = false,
                onNomeChange = {},
                onValorChange = {},
                onNomeFocusChange = {},
                onValorFocusChange = {},
                onConfirm = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1D27, name = "AddListaSheet - Preenchido")
@Composable
private fun AddListaSheetPreenchidoPreview() {
    NossaFeiraTheme {
        Surface(color = Surface) {
            AddListaSheetContent(
                nome = "Feira da semana",
                valorTexto = "150,00",
                nomeFocado = true,
                valorFocado = false,
                onNomeChange = {},
                onValorChange = {},
                onNomeFocusChange = {},
                onValorFocusChange = {},
                onConfirm = {}
            )
        }
    }
}
