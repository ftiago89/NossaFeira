package com.example.nossafeira.viewmodel

import android.app.Application
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.model.ListaFeira
import com.example.nossafeira.data.repository.NossaFeiraRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItensViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val listaId = 42
    private lateinit var repository: NossaFeiraRepository
    private lateinit var viewModel: ItensViewModel
    private lateinit var listaFlow: MutableStateFlow<ListaComItens?>

    private fun item(categoria: Categoria, nome: String = "Item") = ItemFeira(
        listaId = listaId,
        nome = nome,
        quantidade = "1",
        categoria = categoria
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        repository = mockk()
        listaFlow = MutableStateFlow(null)

        io.mockk.every { repository.observarListaPorId(listaId) } returns listaFlow

        viewModel = ItensViewModel(mockk<Application>(), listaId, repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── setFiltro ─────────────────────────────────────────────────────────────

    @Test
    fun `filtroCategoria inicial e null`() = runTest(dispatcher) {
        assertNull(viewModel.filtroCategoria.value)
    }

    @Test
    fun `setFiltro atualiza filtroCategoria`() = runTest(dispatcher) {
        viewModel.setFiltro(Categoria.HORTIFRUTI)
        assertEquals(Categoria.HORTIFRUTI, viewModel.filtroCategoria.value)
    }

    @Test
    fun `setFiltro null remove o filtro`() = runTest(dispatcher) {
        viewModel.setFiltro(Categoria.HORTIFRUTI)
        viewModel.setFiltro(null)
        assertNull(viewModel.filtroCategoria.value)
    }

    // ── itensFiltrados ────────────────────────────────────────────────────────

    @Test
    fun `sem filtro retorna todos os itens`() = runTest(dispatcher) {
        listaFlow.value = ListaComItens(
            lista = ListaFeira(id = listaId, nome = "Feira"),
            itens = listOf(item(Categoria.HORTIFRUTI), item(Categoria.LATICINIOS))
        )

        viewModel.setFiltro(null)

        assertEquals(2, viewModel.itensFiltrados.first().size)
    }

    @Test
    fun `com filtro retorna apenas itens da categoria selecionada`() = runTest(dispatcher) {
        listaFlow.value = ListaComItens(
            lista = ListaFeira(id = listaId, nome = "Feira"),
            itens = listOf(
                item(Categoria.HORTIFRUTI, "Alface"),
                item(Categoria.LATICINIOS, "Queijo"),
                item(Categoria.HORTIFRUTI, "Tomate")
            )
        )

        viewModel.setFiltro(Categoria.HORTIFRUTI)

        val filtrados = viewModel.itensFiltrados.first()
        assertEquals(2, filtrados.size)
        assert(filtrados.all { it.categoria == Categoria.HORTIFRUTI })
    }

    @Test
    fun `com filtro sem correspondencia retorna lista vazia`() = runTest(dispatcher) {
        listaFlow.value = ListaComItens(
            lista = ListaFeira(id = listaId, nome = "Feira"),
            itens = listOf(item(Categoria.LATICINIOS))
        )

        viewModel.setFiltro(Categoria.PADARIA)

        assertEquals(0, viewModel.itensFiltrados.first().size)
    }

    @Test
    fun `lista null retorna lista vazia`() = runTest(dispatcher) {
        listaFlow.value = null

        assertEquals(0, viewModel.itensFiltrados.first().size)
    }

    // ── adicionarItem ─────────────────────────────────────────────────────────

    @Test
    fun `adicionarItem com nome vazio nao chama repository`() = runTest(dispatcher) {
        viewModel.adicionarItem("", "1", Categoria.OUTROS)

        coVerify(exactly = 0) { repository.adicionarItem(any()) }
    }

    @Test
    fun `adicionarItem com nome em branco nao chama repository`() = runTest(dispatcher) {
        viewModel.adicionarItem("   ", "1", Categoria.OUTROS)

        coVerify(exactly = 0) { repository.adicionarItem(any()) }
    }

    @Test
    fun `adicionarItem com nome valido chama repository`() = runTest(dispatcher) {
        coJustRun { repository.adicionarItem(any()) }

        viewModel.adicionarItem("Arroz", "2 kg", Categoria.OUTROS)

        coVerify { repository.adicionarItem(any()) }
    }

    @Test
    fun `adicionarItem aplica trim no nome`() = runTest(dispatcher) {
        val itemCapturado = slot<ItemFeira>()
        coJustRun { repository.adicionarItem(capture(itemCapturado)) }

        viewModel.adicionarItem("  Arroz  ", "1", Categoria.OUTROS)

        assertEquals("Arroz", itemCapturado.captured.nome)
    }

    @Test
    fun `adicionarItem aplica trim na quantidade`() = runTest(dispatcher) {
        val itemCapturado = slot<ItemFeira>()
        coJustRun { repository.adicionarItem(capture(itemCapturado)) }

        viewModel.adicionarItem("Arroz", "  2 kg  ", Categoria.OUTROS)

        assertEquals("2 kg", itemCapturado.captured.quantidade)
    }

    @Test
    fun `adicionarItem repassa o listaId correto`() = runTest(dispatcher) {
        val itemCapturado = slot<ItemFeira>()
        coJustRun { repository.adicionarItem(capture(itemCapturado)) }

        viewModel.adicionarItem("Arroz", "1", Categoria.OUTROS)

        assertEquals(listaId, itemCapturado.captured.listaId)
    }

    @Test
    fun `adicionarItem repassa preco`() = runTest(dispatcher) {
        val itemCapturado = slot<ItemFeira>()
        coJustRun { repository.adicionarItem(capture(itemCapturado)) }

        viewModel.adicionarItem("Arroz", "1", Categoria.OUTROS, preco = 799)

        assertEquals(799, itemCapturado.captured.preco)
    }

    // ── editarItem ────────────────────────────────────────────────────────────

    @Test
    fun `editarItem com nome vazio nao chama repository`() = runTest(dispatcher) {
        val item = item(Categoria.OUTROS)
        viewModel.editarItem(item, "", "1", Categoria.OUTROS, 0)

        coVerify(exactly = 0) { repository.atualizarItem(any()) }
    }

    @Test
    fun `editarItem atualiza os campos corretamente`() = runTest(dispatcher) {
        val itemCapturado = slot<ItemFeira>()
        coJustRun { repository.atualizarItem(capture(itemCapturado)) }

        val original = item(Categoria.OUTROS, "Arroz")
        viewModel.editarItem(original, "Feijão", "500 g", Categoria.PROTEINAS, 350)

        assertEquals("Feijão", itemCapturado.captured.nome)
        assertEquals("500 g", itemCapturado.captured.quantidade)
        assertEquals(Categoria.PROTEINAS, itemCapturado.captured.categoria)
        assertEquals(350, itemCapturado.captured.preco)
    }

    // ── toggleComprado ────────────────────────────────────────────────────────

    @Test
    fun `toggleComprado chama repository`() = runTest(dispatcher) {
        coJustRun { repository.toggleComprado(any()) }
        val item = item(Categoria.OUTROS)

        viewModel.toggleComprado(item)

        coVerify { repository.toggleComprado(item) }
    }

    // ── deletarItem ───────────────────────────────────────────────────────────

    @Test
    fun `deletarItem chama repository`() = runTest(dispatcher) {
        coJustRun { repository.deletarItem(any()) }
        val item = item(Categoria.OUTROS)

        viewModel.deletarItem(item)

        coVerify { repository.deletarItem(item) }
    }
}
