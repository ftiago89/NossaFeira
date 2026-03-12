package com.example.nossafeira.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nossafeira.data.db.NossaFeiraDatabase
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import com.example.nossafeira.data.model.ListaFeira
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemFeiraDaoTest {

    private lateinit var db: NossaFeiraDatabase
    private lateinit var listaDao: ListaFeiraDao
    private lateinit var itemDao: ItemFeiraDao
    private var listaId: Int = 0

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NossaFeiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        listaDao = db.listaFeiraDao()
        itemDao = db.itemFeiraDao()
        listaId = listaDao.inserir(ListaFeira(nome = "Lista base")).toInt()
    }

    @After
    fun teardown() { db.close() }

    private fun item(
        nome: String,
        categoria: Categoria = Categoria.OUTROS,
        comprado: Boolean = false
    ) = ItemFeira(listaId = listaId, nome = nome, quantidade = "1un", categoria = categoria, comprado = comprado)

    @Test
    fun inserir_e_observar_por_lista() = runBlocking {
        itemDao.inserir(item("Arroz"))
        itemDao.inserir(item("Leite"))

        val itens = itemDao.observarPorLista(listaId).first()

        assertEquals(2, itens.size)
    }

    @Test
    fun observarPorListaECategoria_filtra_corretamente() = runBlocking {
        itemDao.inserir(item("Alface", Categoria.HORTIFRUTI))
        itemDao.inserir(item("Leite", Categoria.LATICINIOS))
        itemDao.inserir(item("Arroz", Categoria.OUTROS))

        val hortifruti = itemDao.observarPorListaECategoria(listaId, Categoria.HORTIFRUTI).first()

        assertEquals(1, hortifruti.size)
        assertEquals("Alface", hortifruti[0].nome)
    }

    @Test
    fun atualizarComprado_alterna_estado() = runBlocking {
        val id = itemDao.inserir(item("Arroz", comprado = false)).toInt()

        itemDao.atualizarComprado(id, true)
        assertTrue(itemDao.observarPorLista(listaId).first().first { it.id == id }.comprado)

        itemDao.atualizarComprado(id, false)
        assertFalse(itemDao.observarPorLista(listaId).first().first { it.id == id }.comprado)
    }

    @Test
    fun deletarPorLista_remove_todos_itens_da_lista() = runBlocking {
        itemDao.inserir(item("Arroz"))
        itemDao.inserir(item("Leite"))

        itemDao.deletarPorLista(listaId)

        assertTrue(itemDao.observarPorLista(listaId).first().isEmpty())
    }

    @Test
    fun inserirTodos_insere_multiplos_itens_de_uma_vez() = runBlocking {
        val itens = listOf(
            item("Arroz"),
            item("Leite", Categoria.LATICINIOS),
            item("Sabão", Categoria.LIMPEZA)
        )

        itemDao.inserirTodos(itens)

        assertEquals(3, itemDao.observarPorLista(listaId).first().size)
    }
}
