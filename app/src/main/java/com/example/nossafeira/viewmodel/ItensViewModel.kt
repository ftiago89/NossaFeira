package com.example.nossafeira.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nossafeira.data.db.NossaFeiraDatabase
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.repository.NossaFeiraRepository
import com.example.nossafeira.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItensViewModel(
    application: Application,
    val listaId: Int,
    private val repository: NossaFeiraRepository
) : AndroidViewModel(application) {

    val listaComItens: StateFlow<ListaComItens?> =
        repository.observarListaPorId(listaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _filtroCategoria = MutableStateFlow<Categoria?>(null)
    val filtroCategoria: StateFlow<Categoria?> = _filtroCategoria

    private val _syncEvento = MutableSharedFlow<SyncEvento>()
    val syncEvento: SharedFlow<SyncEvento> = _syncEvento.asSharedFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    val itensFiltrados: StateFlow<List<ItemFeira>> =
        combine(listaComItens, _filtroCategoria) { listaCom, filtro ->
            val itens = listaCom?.itens ?: emptyList()
            val filtrados = if (filtro == null) itens else itens.filter { it.categoria == filtro }
            filtrados.sortedBy { it.nome.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Ações ─────────────────────────────────────────────────────────────────

    fun adicionarItem(nome: String, quantidade: String, categoria: Categoria, preco: Int = 0) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            repository.adicionarItem(
                ItemFeira(
                    listaId = listaId,
                    nome = nome.trim(),
                    quantidade = quantidade.trim(),
                    categoria = categoria,
                    preco = preco
                )
            )
        }
    }

    fun editarItem(item: ItemFeira, nome: String, quantidade: String, categoria: Categoria, preco: Int) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            repository.atualizarItem(
                item.copy(nome = nome.trim(), quantidade = quantidade.trim(), categoria = categoria, preco = preco)
            )
        }
    }

    fun toggleComprado(item: ItemFeira) {
        viewModelScope.launch {
            repository.toggleComprado(item)
        }
    }

    fun deletarItem(item: ItemFeira) {
        viewModelScope.launch {
            repository.deletarItem(item)
        }
    }

    fun compartilharLista() {
        val listaCom = listaComItens.value ?: return
        if (_isSharing.value) return
        viewModelScope.launch {
            _isSharing.value = true
            runCatching { repository.compartilharLista(listaCom) }
                .onSuccess { _syncEvento.emit(SyncEvento.Compartilhada) }
                .onFailure { _syncEvento.emit(SyncEvento.ErroRede) }
            _isSharing.value = false
        }
    }

    fun sincronizarLista() {
        val listaCom = listaComItens.value ?: return
        if (listaCom.lista.remoteId == null) return
        viewModelScope.launch {
            _isSyncing.value = true
            runCatching { repository.sincronizarLista(listaCom) }
                .onSuccess { result ->
                    val evento = when (result) {
                        SyncResult.Sucesso       -> SyncEvento.Sincronizada
                        SyncResult.Mesclada      -> SyncEvento.Mesclada
                        SyncResult.Erro          -> SyncEvento.ErroRede
                        SyncResult.ListaDeletada -> SyncEvento.ListaDeletada
                    }
                    _syncEvento.emit(evento)
                }
                .onFailure { _syncEvento.emit(SyncEvento.ErroRede) }
            _isSyncing.value = false
        }
    }

    fun setFiltro(categoria: Categoria?) {
        _filtroCategoria.value = categoria
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(application: Application, listaId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val db = NossaFeiraDatabase.getInstance(application)
                    val repo = NossaFeiraRepository(db.listaFeiraDao(), db.itemFeiraDao())
                    return ItensViewModel(application, listaId, repo) as T
                }
            }
    }
}
