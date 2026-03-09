package com.example.nossafeira.data.repository

import com.example.nossafeira.data.dao.ItemFeiraDao
import com.example.nossafeira.data.dao.ListaFeiraDao
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.model.ListaFeira
import kotlinx.coroutines.flow.Flow

class NossaFeiraRepository(
    private val listaDao: ListaFeiraDao,
    private val itemDao: ItemFeiraDao
) {

    // ── Listas ────────────────────────────────────────────────────────────────

    fun observarListasComItens(): Flow<List<ListaComItens>> =
        listaDao.observarTodasComItens()

    fun observarListaPorId(id: Int): Flow<ListaComItens?> =
        listaDao.observarPorId(id)

    suspend fun criarLista(nome: String, valorEstimado: Double = 0.0): Long =
        listaDao.inserir(ListaFeira(nome = nome, valorEstimado = valorEstimado))

    suspend fun atualizarLista(lista: ListaFeira) =
        listaDao.atualizar(lista)

    suspend fun deletarLista(id: Int) =
        listaDao.deletarPorId(id)

    // ── Itens ─────────────────────────────────────────────────────────────────

    fun observarItensDaLista(listaId: Int): Flow<List<ItemFeira>> =
        itemDao.observarPorLista(listaId)

    fun observarItensPorCategoria(listaId: Int, categoria: Categoria): Flow<List<ItemFeira>> =
        itemDao.observarPorListaECategoria(listaId, categoria)

    suspend fun adicionarItem(item: ItemFeira): Long =
        itemDao.inserir(item)

    suspend fun atualizarItem(item: ItemFeira) =
        itemDao.atualizar(item)

    suspend fun deletarItem(id: Int) =
        itemDao.deletarPorId(id)

    suspend fun toggleComprado(item: ItemFeira) =
        itemDao.atualizarComprado(item.id, !item.comprado)
}
