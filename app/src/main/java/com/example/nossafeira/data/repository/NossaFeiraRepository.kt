package com.example.nossafeira.data.repository

import com.example.nossafeira.data.dao.ItemFeiraDao
import com.example.nossafeira.data.dao.ListaFeiraDao
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.model.ListaFeira
import com.example.nossafeira.data.remote.RemoteDataSource
import com.example.nossafeira.data.remote.dto.ItemDto
import com.example.nossafeira.data.remote.dto.ListaDto
import com.example.nossafeira.data.remote.dto.PostListaRequest
import com.example.nossafeira.data.remote.dto.PutListaRequest
import com.example.nossafeira.ui.utils.calcularTotalGasto
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException

class NossaFeiraRepository(
    private val listaDao: ListaFeiraDao,
    private val itemDao: ItemFeiraDao,
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) {

    // ── Listas ────────────────────────────────────────────────────────────────

    fun observarListasComItens(): Flow<List<ListaComItens>> =
        listaDao.observarTodasComItens()

    fun observarListaPorId(id: Int): Flow<ListaComItens?> =
        listaDao.observarPorId(id)

    suspend fun criarLista(nome: String, valorEstimado: Int = 0): Long =
        listaDao.inserir(ListaFeira(nome = nome, valorEstimado = valorEstimado))

    suspend fun atualizarLista(lista: ListaFeira) =
        listaDao.atualizar(lista.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deletarLista(id: Int) =
        listaDao.deletarPorId(id)

    // ── Itens ─────────────────────────────────────────────────────────────────

    fun observarItensDaLista(listaId: Int): Flow<List<ItemFeira>> =
        itemDao.observarPorLista(listaId)

    fun observarItensPorCategoria(listaId: Int, categoria: Categoria): Flow<List<ItemFeira>> =
        itemDao.observarPorListaECategoria(listaId, categoria)

    suspend fun adicionarItem(item: ItemFeira): Long {
        val result = itemDao.inserir(item)
        listaDao.atualizarUpdatedAt(item.listaId, System.currentTimeMillis())
        return result
    }

    suspend fun atualizarItem(item: ItemFeira) {
        itemDao.atualizar(item)
        listaDao.atualizarUpdatedAt(item.listaId, System.currentTimeMillis())
    }

    suspend fun deletarItem(item: ItemFeira) {
        itemDao.deletarPorId(item.id)
        listaDao.atualizarUpdatedAt(item.listaId, System.currentTimeMillis())
    }

    suspend fun toggleComprado(item: ItemFeira) {
        itemDao.atualizarComprado(item.id, !item.comprado)
        listaDao.atualizarUpdatedAt(item.listaId, System.currentTimeMillis())
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    suspend fun compartilharLista(listaComItens: ListaComItens) {
        val lista = listaComItens.lista
        val now = System.currentTimeMillis()
        val body = PostListaRequest(
            id = lista.remoteId ?: java.util.UUID.randomUUID().toString(),
            nome = lista.nome,
            valorEstimado = lista.valorEstimado,
            valorCalculado = calcularTotalGasto(listaComItens.itens),
            criadaEm = lista.criadaEm,
            itens = listaComItens.itens.map { it.toDto() }
        )
        val response = remoteDataSource.compartilharLista(body)
        listaDao.atualizarCompartilhamento(lista.id, response.id, now)
        itemDao.atualizarSnapshot(lista.id)
    }

    suspend fun sincronizarLista(listaComItens: ListaComItens): SyncResult {
        val lista = listaComItens.lista
        val remoteId = lista.remoteId ?: return SyncResult.Erro
        val now = System.currentTimeMillis()

        val remote = try {
            remoteDataSource.getLista(remoteId)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                listaDao.marcarComoLocal(lista.id)
                return SyncResult.ListaDeletada
            }
            throw e
        }

        val remoteUpdatedAt = remote.updatedAt.toTimestampMs()
        val localTemMudancas = lista.updatedAt > lista.syncedAt
        val remoteTemMudancas = remoteUpdatedAt > lista.syncedAt

        return when {
            localTemMudancas && !remoteTemMudancas -> {
                // Apenas local mudou → envia para o backend
                val body = PutListaRequest(
                    nome = lista.nome,
                    valorEstimado = lista.valorEstimado,
                    valorCalculado = calcularTotalGasto(listaComItens.itens),
                    itens = listaComItens.itens.map { it.toDto() }
                )
                remoteDataSource.atualizarLista(remoteId, body)
                itemDao.atualizarSnapshot(lista.id)
                listaDao.atualizarSyncedAt(lista.id, now)
                SyncResult.Sucesso
            }
            remoteTemMudancas -> {
                // Remote mudou (com ou sem mudanças locais) → merge three-way + push
                val itensMerged = mergeItens(lista.id, remote)
                listaDao.atualizar(
                    remote.toLista().copy(id = lista.id, updatedAt = now)
                )
                val body = PutListaRequest(
                    nome = remote.nome,
                    valorEstimado = remote.valorEstimado,
                    valorCalculado = calcularTotalGasto(itensMerged),
                    itens = itensMerged.map { it.toDto() }
                )
                remoteDataSource.atualizarLista(remoteId, body)
                itemDao.atualizarSnapshot(lista.id)
                listaDao.atualizarSyncedAt(lista.id, now)
                SyncResult.Mesclada
            }
            else -> SyncResult.Sucesso // Nada mudou
        }
    }

    suspend fun pullStartup() {
        val response = remoteDataSource.getListas()

        val remoteListas = response.content
        val remoteIds = remoteListas.map { it.id }.toSet()

        remoteListas.forEach { remote ->
            val local = listaDao.buscarPorRemoteId(remote.id)
            if (local == null) {
                // Nova lista compartilhada por outro membro → insere no Room
                val novaLista = remote.toLista()
                val novoId = listaDao.inserir(novaLista).toInt()
                itemDao.inserirTodos(remote.itens.map { it.toItem(novoId) })
                listaDao.atualizarSyncedAt(novoId, remote.updatedAt.toTimestampMs())
            } else if (remote.updatedAt.toTimestampMs() > local.lista.syncedAt) {
                // Outro membro atualizou → merge aditivo (preserva itens locais)
                mergeItens(local.lista.id, remote)
                listaDao.atualizar(remote.toLista().copy(id = local.lista.id))
                listaDao.atualizarSyncedAt(local.lista.id, remote.updatedAt.toTimestampMs())
            }
            // Se updatedAt <= syncedAt: sem mudanças externas, não faz nada
        }

        // Listas compartilhadas localmente que sumiram do backend → viram locais
        listaDao.buscarTodasCompartilhadas().forEach { local ->
            if (local.lista.remoteId !in remoteIds) {
                listaDao.marcarComoLocal(local.lista.id)
            }
        }
    }

    suspend fun deletarListaCompartilhada(lista: ListaFeira) {
        lista.remoteId?.let { remoteDataSource.deletarLista(it) }
        listaDao.deletarPorId(lista.id)
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Merge three-way: compara local vs snapshot vs remote para cada item.
     * - Só local mudou → manter local
     * - Só remote mudou (ou ambos) → aceitar remote
     * - Nenhum mudou → manter local (são iguais)
     * - Item só no remote → inserir
     * - Item só no local → preservar
     * Retorna a lista completa de itens após o merge.
     */
    private suspend fun mergeItens(localId: Int, remote: ListaDto): List<ItemFeira> {
        val itensLocais = itemDao.buscarPorLista(localId)
        val mapaLocal = itensLocais.associateBy { it.remoteItemId }
        val remoteItemIds = remote.itens.map { it.id }.toSet()

        val itensProcessados = remote.itens.map { remoteItem ->
            val local = mapaLocal[remoteItem.id]
            if (local != null) {
                val localMudou = local.nome != local.syncNome ||
                    local.quantidade != local.syncQuantidade ||
                    local.preco != local.syncPreco ||
                    local.comprado != local.syncComprado ||
                    local.categoria.name != local.syncCategoria

                val remoteMudou = remoteItem.nome != local.syncNome ||
                    remoteItem.quantidade != local.syncQuantidade ||
                    remoteItem.preco != local.syncPreco ||
                    remoteItem.comprado != local.syncComprado ||
                    remoteItem.categoria != local.syncCategoria

                if (localMudou && !remoteMudou) {
                    // Só local mudou → manter versão local
                    local
                } else if (remoteMudou) {
                    // Remote mudou (com ou sem local) → aceitar remote
                    val atualizado = remoteItem.toItem(localId).copy(id = local.id)
                    itemDao.atualizar(atualizado)
                    atualizado
                } else {
                    // Nenhum mudou → manter local (são iguais)
                    local
                }
            } else {
                // Novo no remote → insere
                val novoId = itemDao.inserir(remoteItem.toItem(localId)).toInt()
                remoteItem.toItem(localId).copy(id = novoId)
            }
        }

        // Itens só locais (não existem no remote) → preservados, sem alteração
        val itensSoLocais = itensLocais.filter { it.remoteItemId !in remoteItemIds }

        return itensProcessados + itensSoLocais
    }

    private fun ItemFeira.toDto() = ItemDto(
        id = remoteItemId,
        nome = nome,
        quantidade = quantidade,
        categoria = categoria.name,
        preco = preco,
        comprado = comprado,
        criadoEm = criadoEm
    )

    private fun ListaDto.toLista() = ListaFeira(
        nome = nome,
        valorEstimado = valorEstimado,
        criadaEm = criadaEm,
        remoteId = id,
        isShared = true,
        updatedAt = updatedAt.toTimestampMs(),
        syncedAt = 0L
    )

    private fun String.toTimestampMs(): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(this)?.time ?: 0L
    } catch (_: Exception) { 0L }

    private fun ItemDto.toItem(listaId: Int) = ItemFeira(
        listaId = listaId,
        nome = nome,
        quantidade = quantidade,
        categoria = Categoria.valueOf(categoria),
        preco = preco,
        comprado = comprado,
        criadoEm = criadoEm,
        remoteItemId = id,
        syncNome = nome,
        syncQuantidade = quantidade,
        syncPreco = preco,
        syncComprado = comprado,
        syncCategoria = categoria
    )
}
