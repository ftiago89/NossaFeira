package com.example.nossafeira.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nossafeira.data.db.NossaFeiraDatabase
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.repository.NossaFeiraRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NossaFeiraRepository = run {
        val db = NossaFeiraDatabase.getInstance(application)
        NossaFeiraRepository(db.listaFeiraDao(), db.itemFeiraDao())
    }

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

    // ── Ações ─────────────────────────────────────────────────────────────────

    fun criarLista(nome: String, valorEstimado: Double = 0.0) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            repository.criarLista(nome.trim(), valorEstimado)
        }
    }

    fun deletarLista(id: Int) {
        viewModelScope.launch {
            repository.deletarLista(id)
        }
    }

    fun atualizarBusca(query: String) {
        _busca.value = query
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.AndroidViewModelFactory(application) {}
    }
}
