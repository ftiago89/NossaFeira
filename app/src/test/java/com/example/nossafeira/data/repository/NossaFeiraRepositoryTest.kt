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
import com.example.nossafeira.data.remote.dto.ListaPageDto
import com.example.nossafeira.data.remote.dto.PutListaRequest
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NossaFeiraRepositoryTest {

    private lateinit var listaDao: ListaFeiraDao
    private lateinit var itemDao: ItemFeiraDao
    private lateinit var remote: RemoteDataSource
    private lateinit var repository: NossaFeiraRepository

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Converte milissegundos para o formato ISO esperado pelo backend */
    private fun isoDate(ms: Long) = sdf.format(Date(ms))

    private fun listaDto(
        id: String = "remote-1",
        updatedAtMs: Long = 0L,
        itens: List<ItemDto> = emptyList()
    ) = ListaDto(
        id = id,
        nome = "Feira",
        valorEstimado = 0,
        valorCalculado = 0,
        criadaEm = 0L,
        updatedAt = isoDate(updatedAtMs),
        itens = itens
    )

    private fun lista(
        id: Int = 1,
        remoteId: String? = "remote-1",
        updatedAt: Long = 1000L,
        syncedAt: Long = 0L,
        isShared: Boolean = true
    ) = ListaFeira(
        id = id,
        nome = "Feira",
        remoteId = remoteId,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isShared = isShared
    )

    private fun listaComItens(lista: ListaFeira, itens: List<ItemFeira> = emptyList()) =
        ListaComItens(lista = lista, itens = itens)

    private fun item(
        id: Int = 10,
        listaId: Int = 1,
        comprado: Boolean = false
    ) = ItemFeira(
        id = id,
        listaId = listaId,
        nome = "Arroz",
        quantidade = "1",
        categoria = Categoria.OUTROS,
        comprado = comprado
    )

    private fun http404(): HttpException =
        HttpException(Response.error<Any>(404, okhttp3.ResponseBody.create(null, "")))

    @Before
    fun setup() {
        listaDao = mockk()
        itemDao = mockk()
        remote = mockk()
        repository = NossaFeiraRepository(listaDao, itemDao, remote)
    }

    // ── adicionarItem ─────────────────────────────────────────────────────────

    @Test
    fun `adicionarItem insere no dao e atualiza updatedAt`() = runTest {
        val item = item()
        coEvery { itemDao.inserir(item) } returns 1L
        coJustRun { listaDao.atualizarUpdatedAt(any(), any()) }

        repository.adicionarItem(item)

        coVerify { itemDao.inserir(item) }
        coVerify { listaDao.atualizarUpdatedAt(item.listaId, any()) }
    }

    // ── atualizarItem ─────────────────────────────────────────────────────────

    @Test
    fun `atualizarItem atualiza no dao e atualiza updatedAt`() = runTest {
        val item = item()
        coJustRun { itemDao.atualizar(item) }
        coJustRun { listaDao.atualizarUpdatedAt(any(), any()) }

        repository.atualizarItem(item)

        coVerify { itemDao.atualizar(item) }
        coVerify { listaDao.atualizarUpdatedAt(item.listaId, any()) }
    }

    // ── deletarItem ───────────────────────────────────────────────────────────

    @Test
    fun `deletarItem exclui do dao e atualiza updatedAt`() = runTest {
        val item = item()
        coJustRun { itemDao.deletarPorId(item.id) }
        coJustRun { listaDao.atualizarUpdatedAt(any(), any()) }

        repository.deletarItem(item)

        coVerify { itemDao.deletarPorId(item.id) }
        coVerify { listaDao.atualizarUpdatedAt(item.listaId, any()) }
    }

    // ── toggleComprado ────────────────────────────────────────────────────────

    @Test
    fun `toggleComprado inverte de false para true`() = runTest {
        val item = item(comprado = false)
        coJustRun { itemDao.atualizarComprado(any(), any()) }
        coJustRun { listaDao.atualizarUpdatedAt(any(), any()) }

        repository.toggleComprado(item)

        coVerify { itemDao.atualizarComprado(item.id, true) }
    }

    @Test
    fun `toggleComprado inverte de true para false`() = runTest {
        val item = item(comprado = true)
        coJustRun { itemDao.atualizarComprado(any(), any()) }
        coJustRun { listaDao.atualizarUpdatedAt(any(), any()) }

        repository.toggleComprado(item)

        coVerify { itemDao.atualizarComprado(item.id, false) }
    }

    @Test
    fun `toggleComprado atualiza updatedAt da lista`() = runTest {
        val item = item()
        coJustRun { itemDao.atualizarComprado(any(), any()) }
        coJustRun { listaDao.atualizarUpdatedAt(any(), any()) }

        repository.toggleComprado(item)

        coVerify { listaDao.atualizarUpdatedAt(item.listaId, any()) }
    }

    // ── sincronizarLista ──────────────────────────────────────────────────────

    @Test
    fun `sincronizarLista retorna Erro quando remoteId e null`() = runTest {
        val listaLocal = lista(remoteId = null)

        val result = repository.sincronizarLista(listaComItens(listaLocal))

        assertEquals(SyncResult.Erro, result)
        coVerify(exactly = 0) { remote.getLista(any()) }
    }

    @Test
    fun `sincronizarLista retorna ListaDeletada em HTTP 404`() = runTest {
        val listaLocal = lista()
        coEvery { remote.getLista("remote-1") } throws http404()
        coJustRun { listaDao.marcarComoLocal(listaLocal.id) }

        val result = repository.sincronizarLista(listaComItens(listaLocal))

        assertEquals(SyncResult.ListaDeletada, result)
        coVerify { listaDao.marcarComoLocal(listaLocal.id) }
    }

    @Test
    fun `sincronizarLista propaga excecao que nao seja 404`() = runTest {
        val listaLocal = lista()
        val erro500 = HttpException(Response.error<Any>(500, okhttp3.ResponseBody.create(null, "")))
        coEvery { remote.getLista(any()) } throws erro500

        runCatching { repository.sincronizarLista(listaComItens(listaLocal)) }
            .onSuccess { assert(false) { "Deveria ter lançado exceção" } }
            .onFailure { assertEquals(erro500, it) }
    }

    @Test
    fun `sincronizarLista retorna Sucesso quando nenhum lado mudou`() = runTest {
        // syncedAt=2000 → updatedAt=1000 (local não mudou) e remoteUpdatedAt=1000 (remote não mudou)
        val listaLocal = lista(updatedAt = 1000L, syncedAt = 2000L)
        val remoteDto = listaDto(updatedAtMs = 1000L)
        coEvery { remote.getLista("remote-1") } returns remoteDto

        val result = repository.sincronizarLista(listaComItens(listaLocal))

        assertEquals(SyncResult.Sucesso, result)
        coVerify(exactly = 0) { remote.atualizarLista(any(), any()) }
        coVerify(exactly = 0) { listaDao.atualizar(any()) }
    }

    @Test
    fun `sincronizarLista envia local ao backend quando so local mudou`() = runTest {
        // updatedAt=1000 > syncedAt=0 → local mudou
        // remoteUpdatedAt=0 (= syncedAt=0) → remote NÃO mudou
        val listaLocal = lista(updatedAt = 1000L, syncedAt = 0L)
        val remoteDto = listaDto(updatedAtMs = 0L)
        coEvery { remote.getLista("remote-1") } returns remoteDto
        coEvery { remote.atualizarLista(any(), any()) } returns remoteDto
        coJustRun { itemDao.atualizarSnapshot(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }

        val result = repository.sincronizarLista(listaComItens(listaLocal))

        assertEquals(SyncResult.Sucesso, result)
        coVerify { remote.atualizarLista(eq("remote-1"), any<PutListaRequest>()) }
        coVerify { itemDao.atualizarSnapshot(listaLocal.id) }
        coVerify { listaDao.atualizarSyncedAt(listaLocal.id, any()) }
    }

    @Test
    fun `sincronizarLista faz merge e push quando so remote mudou`() = runTest {
        val listaLocal = lista(updatedAt = 0L, syncedAt = 0L)
        val remoteDto = listaDto(updatedAtMs = 1000L)
        coEvery { remote.getLista("remote-1") } returns remoteDto
        coEvery { itemDao.buscarPorLista(listaLocal.id) } returns emptyList()
        coJustRun { listaDao.atualizar(any()) }
        coEvery { remote.atualizarLista(any(), any()) } returns remoteDto
        coJustRun { itemDao.atualizarSnapshot(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }

        val result = repository.sincronizarLista(listaComItens(listaLocal))

        assertEquals(SyncResult.Mesclada, result)
        coVerify { listaDao.atualizar(any()) }
        coVerify { remote.atualizarLista(eq("remote-1"), any<PutListaRequest>()) }
        coVerify { itemDao.atualizarSnapshot(listaLocal.id) }
        coVerify { listaDao.atualizarSyncedAt(listaLocal.id, any()) }
    }

    @Test
    fun `sincronizarLista faz merge e push quando ambos mudaram`() = runTest {
        val listaLocal = lista(updatedAt = 2000L, syncedAt = 0L)
        val remoteDto = listaDto(updatedAtMs = 1000L, itens = listOf(
            ItemDto("remote-item-1", "Arroz", "1kg", "OUTROS", 0, false, 0L)
        ))
        coEvery { remote.getLista("remote-1") } returns remoteDto
        coEvery { itemDao.buscarPorLista(listaLocal.id) } returns emptyList()
        coEvery { itemDao.inserir(any()) } returns 50L
        coJustRun { listaDao.atualizar(any()) }
        coEvery { remote.atualizarLista(any(), any()) } returns remoteDto
        coJustRun { itemDao.atualizarSnapshot(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }

        val result = repository.sincronizarLista(listaComItens(listaLocal))

        assertEquals(SyncResult.Mesclada, result)
        coVerify { remote.atualizarLista(eq("remote-1"), any<PutListaRequest>()) }
    }

    @Test
    fun `sincronizarLista merge preserva itens so locais`() = runTest {
        val itemLocal = item(id = 10, listaId = 1).copy(remoteItemId = "local-only-item")
        val listaLocal = lista(updatedAt = 0L, syncedAt = 0L)
        val remoteDto = listaDto(updatedAtMs = 1000L, itens = listOf(
            ItemDto("remote-item-1", "Arroz", "1kg", "OUTROS", 0, false, 0L)
        ))
        coEvery { remote.getLista("remote-1") } returns remoteDto
        coEvery { itemDao.buscarPorLista(listaLocal.id) } returns listOf(itemLocal)
        coEvery { itemDao.inserir(any()) } returns 50L
        coJustRun { listaDao.atualizar(any()) }
        coEvery { remote.atualizarLista(any(), any()) } returns remoteDto
        coJustRun { itemDao.atualizarSnapshot(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }

        repository.sincronizarLista(listaComItens(listaLocal, listOf(itemLocal)))

        // Item local preservado: não foi deletado
        coVerify(exactly = 0) { itemDao.deletarPorLista(any()) }
        coVerify(exactly = 0) { itemDao.deletarPorId(any()) }
        // Item novo do remote foi inserido
        coVerify { itemDao.inserir(any()) }
    }

    @Test
    fun `sincronizarLista merge aceita remote quando item local nao mudou`() = runTest {
        // Item local com snapshot = valores atuais (não foi editado)
        // Remote tem valores diferentes → remote mudou → aceitar remote
        val itemLocal = item(id = 10, listaId = 1, comprado = false)
            .copy(
                remoteItemId = "shared-item",
                syncNome = "Arroz", syncQuantidade = "1",
                syncPreco = 0, syncComprado = false, syncCategoria = "OUTROS"
            )
        val listaLocal = lista(updatedAt = 0L, syncedAt = 0L)
        val remoteDto = listaDto(updatedAtMs = 1000L, itens = listOf(
            ItemDto("shared-item", "Arroz", "2kg", "OUTROS", 999, true, 0L)
        ))
        coEvery { remote.getLista("remote-1") } returns remoteDto
        coEvery { itemDao.buscarPorLista(listaLocal.id) } returns listOf(itemLocal)
        coJustRun { itemDao.atualizar(any()) }
        coJustRun { listaDao.atualizar(any()) }
        coEvery { remote.atualizarLista(any(), any()) } returns remoteDto
        coJustRun { itemDao.atualizarSnapshot(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }

        repository.sincronizarLista(listaComItens(listaLocal, listOf(itemLocal)))

        // Remote mudou, local não → aceita remote
        coVerify { itemDao.atualizar(match { it.id == 10 && it.comprado && it.preco == 999 }) }
    }

    @Test
    fun `sincronizarLista merge mantem local quando so local mudou`() = runTest {
        // Item local foi editado (comprado=true, snapshot diz comprado=false)
        // Remote é igual ao snapshot → remote não mudou → manter local
        val itemLocal = item(id = 10, listaId = 1, comprado = true)
            .copy(
                remoteItemId = "shared-item",
                syncNome = "Arroz", syncQuantidade = "1",
                syncPreco = 0, syncComprado = false, syncCategoria = "OUTROS"
            )
        val listaLocal = lista(updatedAt = 2000L, syncedAt = 0L)
        val remoteDto = listaDto(updatedAtMs = 1000L, itens = listOf(
            // Remote idêntico ao snapshot: nome=Arroz, qtd=1, preco=0, comprado=false
            ItemDto("shared-item", "Arroz", "1", "OUTROS", 0, false, 0L)
        ))
        coEvery { remote.getLista("remote-1") } returns remoteDto
        coEvery { itemDao.buscarPorLista(listaLocal.id) } returns listOf(itemLocal)
        coJustRun { listaDao.atualizar(any()) }
        coEvery { remote.atualizarLista(any(), any()) } returns remoteDto
        coJustRun { itemDao.atualizarSnapshot(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }

        repository.sincronizarLista(listaComItens(listaLocal, listOf(itemLocal)))

        // Só local mudou → não chama itemDao.atualizar (mantém versão local)
        coVerify(exactly = 0) { itemDao.atualizar(any()) }
    }

    @Test
    fun `sincronizarLista merge aceita remote quando ambos mudaram`() = runTest {
        // Item local editado (comprado=true) E remote editado (preco=999)
        // Ambos diferem do snapshot → ambos mudaram → remote ganha
        val itemLocal = item(id = 10, listaId = 1, comprado = true)
            .copy(
                remoteItemId = "shared-item",
                syncNome = "Arroz", syncQuantidade = "1",
                syncPreco = 0, syncComprado = false, syncCategoria = "OUTROS"
            )
        val listaLocal = lista(updatedAt = 2000L, syncedAt = 0L)
        val remoteDto = listaDto(updatedAtMs = 1500L, itens = listOf(
            // Remote diferente do snapshot: preco mudou para 999
            ItemDto("shared-item", "Arroz", "1", "OUTROS", 999, false, 0L)
        ))
        coEvery { remote.getLista("remote-1") } returns remoteDto
        coEvery { itemDao.buscarPorLista(listaLocal.id) } returns listOf(itemLocal)
        coJustRun { itemDao.atualizar(any()) }
        coJustRun { listaDao.atualizar(any()) }
        coEvery { remote.atualizarLista(any(), any()) } returns remoteDto
        coJustRun { itemDao.atualizarSnapshot(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }

        repository.sincronizarLista(listaComItens(listaLocal, listOf(itemLocal)))

        // Ambos mudaram → remote ganha (preco=999, comprado=false)
        coVerify { itemDao.atualizar(match { it.id == 10 && !it.comprado && it.preco == 999 }) }
    }

    // ── pullStartup ───────────────────────────────────────────────────────────

    @Test
    fun `pullStartup insere nova lista que nao existe localmente`() = runTest {
        val remoteDto = listaDto(id = "nova-remote", updatedAtMs = 500L)
        coEvery { remote.getListas() } returns ListaPageDto(listOf(remoteDto), 0, 50, 1)
        coEvery { listaDao.buscarPorRemoteId("nova-remote") } returns null
        coEvery { listaDao.inserir(any()) } returns 99L
        coJustRun { itemDao.inserirTodos(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }
        coEvery { listaDao.buscarTodasCompartilhadas() } returns emptyList()

        repository.pullStartup()

        coVerify { listaDao.inserir(any()) }
        coVerify { itemDao.inserirTodos(any()) }
        coVerify { listaDao.atualizarSyncedAt(99, any()) }
    }

    @Test
    fun `pullStartup faz merge em lista existente quando remote e mais recente`() = runTest {
        // remoteUpdatedAt=1000 > local.syncedAt=0 → merge aditivo
        val remoteDto = listaDto(id = "remote-1", updatedAtMs = 1000L)
        val localExistente = listaComItens(lista(id = 5, syncedAt = 0L))
        coEvery { remote.getListas() } returns ListaPageDto(listOf(remoteDto), 0, 50, 1)
        coEvery { listaDao.buscarPorRemoteId("remote-1") } returns localExistente
        coEvery { itemDao.buscarPorLista(5) } returns emptyList()
        coJustRun { listaDao.atualizar(any()) }
        coJustRun { listaDao.atualizarSyncedAt(any(), any()) }
        coEvery { listaDao.buscarTodasCompartilhadas() } returns emptyList()

        repository.pullStartup()

        coVerify { listaDao.atualizar(any()) }
        // Merge aditivo: não deleta itens da lista
        coVerify(exactly = 0) { itemDao.deletarPorLista(any()) }
    }

    @Test
    fun `pullStartup nao toca lista existente quando remote nao e mais recente`() = runTest {
        // remoteUpdatedAt=500 <= local.syncedAt=1000 → sem mudanças externas
        val remoteDto = listaDto(id = "remote-1", updatedAtMs = 500L)
        val localExistente = listaComItens(lista(id = 5, syncedAt = 1000L))
        coEvery { remote.getListas() } returns ListaPageDto(listOf(remoteDto), 0, 50, 1)
        coEvery { listaDao.buscarPorRemoteId("remote-1") } returns localExistente
        coEvery { listaDao.buscarTodasCompartilhadas() } returns emptyList()

        repository.pullStartup()

        coVerify(exactly = 0) { listaDao.atualizar(any()) }
        coVerify(exactly = 0) { itemDao.deletarPorLista(any()) }
    }

    @Test
    fun `pullStartup marca como local lista compartilhada que sumiu do backend`() = runTest {
        // backend retorna lista vazia, mas localmente há uma lista compartilhada com remoteId="sumiu"
        coEvery { remote.getListas() } returns ListaPageDto(emptyList(), 0, 50, 0)
        val listaOrfã = listaComItens(lista(id = 7, remoteId = "sumiu"))
        coEvery { listaDao.buscarTodasCompartilhadas() } returns listOf(listaOrfã)
        coJustRun { listaDao.marcarComoLocal(any()) }

        repository.pullStartup()

        coVerify { listaDao.marcarComoLocal(7) }
    }

    @Test
    fun `pullStartup nao marca como local lista cujo remoteId ainda existe no backend`() = runTest {
        val remoteDto = listaDto(id = "remote-1", updatedAtMs = 0L)
        val localExistente = listaComItens(lista(id = 5, syncedAt = 1000L))
        val listaCompartilhada = listaComItens(lista(id = 5, remoteId = "remote-1"))
        coEvery { remote.getListas() } returns ListaPageDto(listOf(remoteDto), 0, 50, 1)
        coEvery { listaDao.buscarPorRemoteId("remote-1") } returns localExistente
        coEvery { listaDao.buscarTodasCompartilhadas() } returns listOf(listaCompartilhada)

        repository.pullStartup()

        coVerify(exactly = 0) { listaDao.marcarComoLocal(any()) }
    }
}
