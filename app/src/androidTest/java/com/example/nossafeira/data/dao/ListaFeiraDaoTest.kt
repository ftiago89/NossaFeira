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
class ListaFeiraDaoTest {

    private lateinit var db: NossaFeiraDatabase
    private lateinit var listaDao: ListaFeiraDao
    private lateinit var itemDao: ItemFeiraDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NossaFeiraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        listaDao = db.listaFeiraDao()
        itemDao = db.itemFeiraDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun inserir_retorna_id_valido() = runBlocking {
        val id = listaDao.inserir(ListaFeira(nome = "Feira da semana"))
        assertTrue(id > 0)
    }

    @Test
    fun observarTodas_retorna_listas_ordenadas_por_criadaEm_desc() = runBlocking {
        listaDao.inserir(ListaFeira(nome = "Lista A", criadaEm = 1000))
        listaDao.inserir(ListaFeira(nome = "Lista B", criadaEm = 2000))

        val listas = listaDao.observarTodas().first()

        assertEquals(2, listas.size)
        assertEquals("Lista B", listas[0].nome) // criadaEm 2000 vem primeiro
        assertEquals("Lista A", listas[1].nome)
    }

    @Test
    fun observarTodasComItens_retorna_relacao_correta() = runBlocking {
        val listaId = listaDao.inserir(ListaFeira(nome = "Lista com itens")).toInt()
        itemDao.inserir(ItemFeira(listaId = listaId, nome = "Arroz", quantidade = "1kg", categoria = Categoria.OUTROS))
        itemDao.inserir(ItemFeira(listaId = listaId, nome = "Leite", quantidade = "1L", categoria = Categoria.LATICINIOS))

        val resultado = listaDao.observarTodasComItens().first()

        assertEquals(1, resultado.size)
        assertEquals(2, resultado[0].itens.size)
    }

    @Test
    fun deletar_remove_itens_em_cascata() = runBlocking {
        val listaId = listaDao.inserir(ListaFeira(nome = "Lista")).toInt()
        itemDao.inserir(ItemFeira(listaId = listaId, nome = "Arroz", quantidade = "1kg", categoria = Categoria.OUTROS))

        val lista = listaDao.observarTodas().first().first()
        listaDao.deletar(lista)

        assertTrue(listaDao.observarTodas().first().isEmpty())
        assertTrue(itemDao.observarPorLista(listaId).first().isEmpty())
    }

    @Test
    fun atualizarUpdatedAt_atualiza_somente_o_timestamp() = runBlocking {
        val id = listaDao.inserir(ListaFeira(nome = "Lista", updatedAt = 0L)).toInt()

        listaDao.atualizarUpdatedAt(id, 99999L)

        val lista = listaDao.observarTodas().first().first()
        assertEquals(99999L, lista.updatedAt)
        assertEquals("Lista", lista.nome) // demais campos inalterados
    }

    @Test
    fun atualizarCompartilhamento_define_remoteId_isShared_e_syncedAt() = runBlocking {
        val id = listaDao.inserir(ListaFeira(nome = "Lista")).toInt()

        listaDao.atualizarCompartilhamento(id, "uuid-remoto-123", 12345L)

        val lista = listaDao.observarTodas().first().first()
        assertEquals("uuid-remoto-123", lista.remoteId)
        assertTrue(lista.isShared)
        assertEquals(12345L, lista.syncedAt)
    }

    @Test
    fun atualizarSyncedAt_atualiza_somente_syncedAt() = runBlocking {
        val id = listaDao.inserir(ListaFeira(nome = "Lista", syncedAt = 0L)).toInt()

        listaDao.atualizarSyncedAt(id, 77777L)

        val lista = listaDao.observarTodas().first().first()
        assertEquals(77777L, lista.syncedAt)
        assertFalse(lista.isShared) // isShared não foi tocado
    }

    @Test
    fun marcarComoLocal_limpa_remoteId_e_isShared() = runBlocking {
        val id = listaDao.inserir(ListaFeira(nome = "Lista")).toInt()
        listaDao.atualizarCompartilhamento(id, "uuid-remoto", 1000L)

        listaDao.marcarComoLocal(id)

        val lista = listaDao.observarTodas().first().first()
        assertNull(lista.remoteId)
        assertFalse(lista.isShared)
    }

    @Test
    fun buscarPorRemoteId_retorna_lista_correta() = runBlocking {
        val id = listaDao.inserir(ListaFeira(nome = "Compartilhada")).toInt()
        listaDao.inserir(ListaFeira(nome = "Local"))
        listaDao.atualizarCompartilhamento(id, "meu-remote-uuid", 1000L)

        val resultado = listaDao.buscarPorRemoteId("meu-remote-uuid")

        assertNotNull(resultado)
        assertEquals("Compartilhada", resultado!!.lista.nome)
    }

    @Test
    fun buscarTodasCompartilhadas_filtra_somente_isShared_true() = runBlocking {
        val idA = listaDao.inserir(ListaFeira(nome = "Compartilhada")).toInt()
        listaDao.inserir(ListaFeira(nome = "Local"))
        listaDao.atualizarCompartilhamento(idA, "uuid-a", 1000L)

        val compartilhadas = listaDao.buscarTodasCompartilhadas()

        assertEquals(1, compartilhadas.size)
        assertEquals("Compartilhada", compartilhadas[0].lista.nome)
    }
}
