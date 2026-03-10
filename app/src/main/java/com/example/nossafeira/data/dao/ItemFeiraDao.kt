package com.example.nossafeira.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemFeiraDao {

    @Query("SELECT * FROM itens_feira WHERE listaId = :listaId ORDER BY criadoEm ASC")
    fun observarPorLista(listaId: Int): Flow<List<ItemFeira>>

    @Query(
        "SELECT * FROM itens_feira WHERE listaId = :listaId AND categoria = :categoria ORDER BY criadoEm ASC"
    )
    fun observarPorListaECategoria(listaId: Int, categoria: Categoria): Flow<List<ItemFeira>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(item: ItemFeira): Long

    @Update
    suspend fun atualizar(item: ItemFeira)

    @Delete
    suspend fun deletar(item: ItemFeira)

    @Query("DELETE FROM itens_feira WHERE id = :id")
    suspend fun deletarPorId(id: Int)

    @Query("UPDATE itens_feira SET comprado = :comprado WHERE id = :id")
    suspend fun atualizarComprado(id: Int, comprado: Boolean)

    @Query("DELETE FROM itens_feira WHERE listaId = :listaId")
    suspend fun deletarPorLista(listaId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(itens: List<ItemFeira>)
}
