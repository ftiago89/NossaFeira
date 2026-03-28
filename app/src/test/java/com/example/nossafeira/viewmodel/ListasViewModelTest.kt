package com.example.nossafeira.viewmodel

import android.app.Application
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.model.ListaFeira
import com.example.nossafeira.data.repository.NossaFeiraRepository
import com.example.nossafeira.data.repository.SyncResult
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListasViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: NossaFeiraRepository
    private lateinit var viewModel: ListasViewModel
    private lateinit var listasFlow: MutableStateFlow<List<ListaComItens>>

    private fun listaComItens(nome: String) = ListaComItens(
        lista = ListaFeira(id = 1, nome = nome),
        itens = emptyList()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        repository = mockk()
        listasFlow = MutableStateFlow(emptyList())

        // pullStartup() é chamado no init{} — precisa estar mockado antes de criar o ViewModel
        coJustRun { repository.pullStartup() }
        coEvery { repository.observarListasComItens() } returns listasFlow

        viewModel = ListasViewModel(mockk<Application>(), repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── atualizarBusca ────────────────────────────────────────────────────────

    @Test
    fun `busca inicial e string vazia`() = runTest(dispatcher) {
        assertEquals("", viewModel.busca.value)
    }

    @Test
    fun `atualizarBusca atualiza o estado`() = runTest(dispatcher) {
        viewModel.atualizarBusca("arroz")
        assertEquals("arroz", viewModel.busca.value)
    }

    // ── filtragem de listas ───────────────────────────────────────────────────

    @Test
    fun `busca vazia retorna todas as listas`() = runTest(dispatcher) {
        listasFlow.value = listOf(listaComItens("Feira"), listaComItens("Churrasco"))

        viewModel.atualizarBusca("")

        assertEquals(2, viewModel.listas.first().size)
    }

    @Test
    fun `busca filtra listas pelo nome`() = runTest(dispatcher) {
        listasFlow.value = listOf(listaComItens("Feira da semana"), listaComItens("Churrasco"))

        viewModel.atualizarBusca("feira")

        val resultado = viewModel.listas.first()
        assertEquals(1, resultado.size)
        assertEquals("Feira da semana", resultado[0].lista.nome)
    }

    @Test
    fun `busca e case insensitive`() = runTest(dispatcher) {
        listasFlow.value = listOf(listaComItens("Feira"))

        viewModel.atualizarBusca("FEIRA")

        assertEquals(1, viewModel.listas.first().size)
    }

    @Test
    fun `busca sem correspondencia retorna lista vazia`() = runTest(dispatcher) {
        listasFlow.value = listOf(listaComItens("Feira"))

        viewModel.atualizarBusca("xyz")

        assertEquals(0, viewModel.listas.first().size)
    }

    // ── criarLista ────────────────────────────────────────────────────────────

    @Test
    fun `criarLista com nome vazio nao chama repository`() = runTest(dispatcher) {
        viewModel.criarLista("")

        coVerify(exactly = 0) { repository.criarLista(any(), any()) }
    }

    @Test
    fun `criarLista com nome em branco nao chama repository`() = runTest(dispatcher) {
        viewModel.criarLista("   ")

        coVerify(exactly = 0) { repository.criarLista(any(), any()) }
    }

    @Test
    fun `criarLista com nome valido chama repository`() = runTest(dispatcher) {
        coEvery { repository.criarLista(any(), any()) } returns 1L

        viewModel.criarLista("Feira")

        coVerify { repository.criarLista("Feira", 0) }
    }

    @Test
    fun `criarLista aplica trim no nome`() = runTest(dispatcher) {
        coEvery { repository.criarLista(any(), any()) } returns 1L

        viewModel.criarLista("  Feira da Semana  ")

        coVerify { repository.criarLista("Feira da Semana", 0) }
    }

    @Test
    fun `criarLista repassa valorEstimado`() = runTest(dispatcher) {
        coEvery { repository.criarLista(any(), any()) } returns 1L

        viewModel.criarLista("Feira", valorEstimado = 5000)

        coVerify { repository.criarLista("Feira", 5000) }
    }

    // ── sincronizarTodas ──────────────────────────────────────────────────────

    @Test
    fun `sincronizarTodas emite PullConcluido em sucesso`() = runTest(dispatcher) {
        // async subscreve ANTES da chamada → garante que o subscriber está ativo
        // quando o evento for emitido (SharedFlow tem replay=0)
        val eventoDeferred = async { viewModel.syncEvento.first() }

        viewModel.sincronizarTodas()

        assertEquals(SyncEvento.PullConcluido, eventoDeferred.await())
    }

    @Test
    fun `sincronizarTodas emite ErroRede em falha`() = runTest(dispatcher) {
        coEvery { repository.pullStartup() } throws Exception("sem rede")

        val eventoDeferred = async { viewModel.syncEvento.first() }

        viewModel.sincronizarTodas()

        assertEquals(SyncEvento.ErroRede, eventoDeferred.await())
    }

    @Test
    fun `isSyncing e false apos sync concluir`() = runTest(dispatcher) {
        viewModel.sincronizarTodas()

        assertFalse(viewModel.isSyncing.value)
    }

    // ── sincronizarLista ──────────────────────────────────────────────────────

    @Test
    fun `sincronizarLista emite Sincronizada quando resultado e Sucesso`() = runTest(dispatcher) {
        val listaComItens = listaComItens("Feira")
        coEvery { repository.sincronizarLista(listaComItens) } returns SyncResult.Sucesso

        val eventoDeferred = async { viewModel.syncEvento.first() }

        viewModel.sincronizarLista(listaComItens)

        assertEquals(SyncEvento.Sincronizada, eventoDeferred.await())
    }

    @Test
    fun `sincronizarLista emite Mesclada quando resultado e Mesclada`() = runTest(dispatcher) {
        val listaComItens = listaComItens("Feira")
        coEvery { repository.sincronizarLista(listaComItens) } returns SyncResult.Mesclada

        val eventoDeferred = async { viewModel.syncEvento.first() }

        viewModel.sincronizarLista(listaComItens)

        assertEquals(SyncEvento.Mesclada, eventoDeferred.await())
    }
}
