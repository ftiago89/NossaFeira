package com.example.nossafeira.ui.screens.itens

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nossafeira.R
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
import com.example.nossafeira.ui.utils.extrairPrecosDaEtiqueta
import com.example.nossafeira.ui.utils.extrairQuantidadeNumerica
import com.example.nossafeira.viewmodel.ItensViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ItensScreen(
    listaId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: ItensViewModel = viewModel(
        key = "itens_$listaId",
        factory = ItensViewModel.factory(application, listaId)
    )
    val scope = rememberCoroutineScope()

    val listaComItens by viewModel.listaComItens.collectAsStateWithLifecycle()
    val itensFiltrados by viewModel.itensFiltrados.collectAsStateWithLifecycle()
    val filtroCategoria by viewModel.filtroCategoria.collectAsStateWithLifecycle()
    var mostrarAddSheet by remember { mutableStateOf(false) }
    var itemParaEditar by remember { mutableStateOf<ItemFeira?>(null) }

    // Estado da câmera/OCR — rememberSaveable para sobreviver a process death
    var precoSugeridos by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var isProcessandoOcr by rememberSaveable { mutableStateOf(false) }
    var fotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // Estado da entrada por voz
    var nomeReconhecido by rememberSaveable { mutableStateOf<String?>(null) }

    val totalItens = listaComItens?.itens?.size ?: 0
    val itensComprados = listaComItens?.itens?.count { it.comprado } ?: 0
    val totalGasto = calcularTotalGasto(listaComItens?.itens ?: emptyList())

    // Launcher de câmera
    val cameraLauncher = rememberLauncherForActivityResult(TakePicture()) { sucesso ->
        val uri = fotoUri
        android.util.Log.d("PrecoOCR", "Camera resultado: sucesso=$sucesso uri=$uri")
        if (sucesso && uri != null) {
            isProcessandoOcr = true
            scope.launch {
                val texto = withContext(Dispatchers.IO) {
                    val bitmap = carregarBitmapRedimensionado(context, uri)
                    android.util.Log.d("PrecoOCR", "Bitmap carregado: ${bitmap?.width}x${bitmap?.height}")
                    if (bitmap != null) {
                        executarOcr(bitmap)
                    } else null
                }
                // Deleta o arquivo temporário
                fotoUri?.path?.let { File(it).delete() }
                fotoUri = null

                android.util.Log.d("PrecoOCR", "Texto OCR: $texto")
                if (texto != null) {
                    val candidatos = extrairPrecosDaEtiqueta(texto)
                    android.util.Log.d("PrecoOCR", "Candidatos: $candidatos")
                    precoSugeridos = candidatos
                    if (candidatos.isEmpty()) {
                        Toast.makeText(context, "Nenhum preço encontrado na imagem", Toast.LENGTH_SHORT).show()
                    }
                }
                isProcessandoOcr = false
            }
        } else {
            fotoUri?.path?.let { File(it).delete() }
            fotoUri = null
        }
    }

    // Launcher de permissão de câmera
    val permissaoLauncher = rememberLauncherForActivityResult(RequestPermission()) { concedida ->
        if (concedida) {
            fotoUri = criarArquivoFoto(context)
            fotoUri?.let { cameraLauncher.launch(it) }
        } else {
            Toast.makeText(context, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
        }
    }

    fun solicitarCameraOuAbrir() {
        precoSugeridos = emptyList()
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                fotoUri = criarArquivoFoto(context)
                fotoUri?.let { cameraLauncher.launch(it) }
            }
            else -> permissaoLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Launcher de reconhecimento de voz
    val speechLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                nomeReconhecido = spoken
            }
        }
    }

    // Launcher de permissão de microfone
    val micPermissaoLauncher = rememberLauncherForActivityResult(RequestPermission()) { concedida ->
        if (concedida) {
            launchSpeechRecognizer(context, speechLauncher)
        } else {
            Toast.makeText(context, context.getString(R.string.voice_permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    fun solicitarVozOuAbrir() {
        nomeReconhecido = null
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                launchSpeechRecognizer(context, speechLauncher)
            }
            else -> micPermissaoLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            ItensTopBar(
                titulo = listaComItens?.lista?.nome ?: "",
                onBack = onBack
            )
        },
        floatingActionButton = {
            NossaFeiraFab(onClick = {
                precoSugeridos = emptyList()
                nomeReconhecido = null
                mostrarAddSheet = true
            })
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
                        onLongClick = {
                            precoSugeridos = emptyList()
                            nomeReconhecido = null
                            itemParaEditar = item
                        },
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
            onDismiss = {
                mostrarAddSheet = false
                precoSugeridos = emptyList()
                nomeReconhecido = null
            },
            onConfirm = { nome, quantidade, categoria, preco ->
                viewModel.adicionarItem(nome, quantidade, categoria, preco)
                mostrarAddSheet = false
                precoSugeridos = emptyList()
                nomeReconhecido = null
            },
            onCameraRequest = { solicitarCameraOuAbrir() },
            precoSugeridos = precoSugeridos,
            isProcessandoOcr = isProcessandoOcr,
            onVoiceRequest = { solicitarVozOuAbrir() },
            nomeReconhecido = nomeReconhecido
        )
    }

    itemParaEditar?.let { item ->
        AddItemSheet(
            onDismiss = {
                itemParaEditar = null
                precoSugeridos = emptyList()
                nomeReconhecido = null
            },
            onConfirm = { nome, quantidade, categoria, preco ->
                viewModel.editarItem(item, nome, quantidade, categoria, preco)
                itemParaEditar = null
                precoSugeridos = emptyList()
                nomeReconhecido = null
            },
            itemParaEditar = item,
            onCameraRequest = { solicitarCameraOuAbrir() },
            precoSugeridos = precoSugeridos,
            isProcessandoOcr = isProcessandoOcr,
            onVoiceRequest = { solicitarVozOuAbrir() },
            nomeReconhecido = nomeReconhecido
        )
    }
}

// ── Helper de voz ────────────────────────────────────────────────────────────

private fun launchSpeechRecognizer(context: Context, launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt))
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        launcher.launch(intent)
    } else {
        Toast.makeText(context, context.getString(R.string.voice_not_available), Toast.LENGTH_SHORT).show()
    }
}

// ── Helpers de câmera/OCR ─────────────────────────────────────────────────────

private fun criarArquivoFoto(context: Context): Uri? {
    return try {
        val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
        val arquivo = File.createTempFile("foto_etiqueta_", ".jpg", dir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
    } catch (e: Exception) {
        null
    }
}

private fun carregarBitmapRedimensionado(context: Context, uri: Uri, maxPx: Int = 1200): Bitmap? {
    return try {
        // Primeira passagem: descobre as dimensões sem alocar pixels
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val escala = maxOf(bounds.outWidth, bounds.outHeight) / maxPx
        val opcoes = BitmapFactory.Options().apply {
            inSampleSize = if (escala > 1) escala else 1
        }

        // Segunda passagem: decodifica com subamostragem
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
    } catch (e: Exception) {
        null
    }
}

private suspend fun executarOcr(bitmap: Bitmap): String? {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val imagem = InputImage.fromBitmap(bitmap, 0)
    return try {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            recognizer.process(imagem)
                .addOnSuccessListener { resultado ->
                    android.util.Log.d("PrecoOCR", "ML Kit sucesso, texto=${resultado.text}")
                    cont.resume(resultado.text) {}
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrecoOCR", "ML Kit falhou", e)
                    cont.resume(null) {}
                }
        }
    } catch (e: Exception) {
        android.util.Log.e("PrecoOCR", "executarOcr exception", e)
        null
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
