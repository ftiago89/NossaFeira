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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItensViewModel(
    application: Application,
    val listaId: Int
) : AndroidViewModel(application) {

    private val repository: NossaFeiraRepository = run {
        val db = NossaFeiraDatabase.getInstance(application)
        NossaFeiraRepository(db.listaFeiraDao(), db.itemFeiraDao())
    }

    val listaComItens: StateFlow<ListaComItens?> =
        repository.observarListaPorId(listaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _filtroCategoria = MutableStateFlow<Categoria?>(null)
    val filtroCategoria: StateFlow<Categoria?> = _filtroCategoria

    val itensFiltrados: StateFlow<List<ItemFeira>> =
        combine(listaComItens, _filtroCategoria) { listaCom, filtro ->
            val itens = listaCom?.itens ?: emptyList()
            if (filtro == null) itens else itens.filter { it.categoria == filtro }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Ações ─────────────────────────────────────────────────────────────────

    fun adicionarItem(nome: String, quantidade: String, categoria: Categoria, preco: Double = 0.0) {
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

    fun editarItem(item: ItemFeira, nome: String, quantidade: String, categoria: Categoria, preco: Double) {
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

    fun deletarItem(id: Int) {
        viewModelScope.launch {
            repository.deletarItem(id)
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
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    ItensViewModel(application, listaId) as T
            }
    }
}
