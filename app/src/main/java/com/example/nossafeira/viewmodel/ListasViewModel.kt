package com.example.nossafeira.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nossafeira.data.db.NossaFeiraDatabase
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.model.ListaFeira
import com.example.nossafeira.data.repository.NossaFeiraRepository
import com.example.nossafeira.data.repository.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

class ListasViewModel(
    application: Application,
    private val repository: NossaFeiraRepository
) : AndroidViewModel(application) {

    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca

    private val _todasListas: StateFlow<List<ListaComItens>> =
        repository.observarListasComItens()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val listas: StateFlow<List<ListaComItens>> =
        combine(_todasListas, _busca) { listas, busca ->
            if (busca.isBlank()) listas
            else listas.filter { it.lista.nome.contains(busca, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncEvento = MutableSharedFlow<SyncEvento>()
    val syncEvento: SharedFlow<SyncEvento> = _syncEvento.asSharedFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _sharingIds = MutableStateFlow<Set<Int>>(emptySet())
    val sharingIds: StateFlow<Set<Int>> = _sharingIds.asStateFlow()

    private val _deletingIds = MutableStateFlow<Set<Int>>(emptySet())
    val deletingIds: StateFlow<Set<Int>> = _deletingIds.asStateFlow()

    init {
        pullStartup()
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    fun criarLista(nome: String, valorEstimado: Int = 0) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            repository.criarLista(nome.trim(), valorEstimado)
        }
    }

    fun deletarLista(listaComItens: ListaComItens) {
        val id = listaComItens.lista.id
        if (_deletingIds.value.isNotEmpty()) return
        viewModelScope.launch {
            _deletingIds.value = setOf(id)
            if (listaComItens.lista.isShared) {
                runCatching { repository.deletarListaCompartilhada(listaComItens.lista) }
                    .onFailure { repository.deletarLista(listaComItens.lista.id) }
            } else {
                repository.deletarLista(listaComItens.lista.id)
            }
            _deletingIds.value = emptySet()
        }
    }

    fun compartilharLista(listaComItens: ListaComItens) {
        if (_sharingIds.value.isNotEmpty()) return
        val id = listaComItens.lista.id
        viewModelScope.launch {
            _sharingIds.value = setOf(id)
            runCatching { repository.compartilharLista(listaComItens) }
                .onSuccess { _syncEvento.emit(SyncEvento.Compartilhada) }
                .onFailure { _syncEvento.emit(SyncEvento.ErroRede) }
            _sharingIds.value = emptySet()
        }
    }

    fun sincronizarLista(listaComItens: ListaComItens) {
        viewModelScope.launch {
            runCatching { repository.sincronizarLista(listaComItens) }
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
        }
    }

    fun editarLista(lista: ListaFeira, novoNome: String, novoValor: Int) {
        if (novoNome.isBlank()) return
        viewModelScope.launch {
            repository.atualizarLista(lista.copy(nome = novoNome.trim(), valorEstimado = novoValor))
        }
    }

    fun atualizarBusca(query: String) {
        _busca.value = query
    }

    fun sincronizarTodas() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            runCatching { repository.pullStartup() }
                .onSuccess { _syncEvento.emit(SyncEvento.PullConcluido) }
                .onFailure { _syncEvento.emit(SyncEvento.ErroRede) }
            _isSyncing.value = false
        }
    }

    private fun pullStartup() {
        viewModelScope.launch {
            runCatching { repository.pullStartup() }
            // silencioso — erros de rede no startup não são exibidos ao usuário
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val db = NossaFeiraDatabase.getInstance(application)
                    val repo = NossaFeiraRepository(db.listaFeiraDao(), db.itemFeiraDao())
                    return ListasViewModel(application, repo) as T
                }
            }
    }
}
