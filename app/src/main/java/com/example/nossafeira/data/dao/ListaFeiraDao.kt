package com.example.nossafeira.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.nossafeira.data.model.ListaComItens
import com.example.nossafeira.data.model.ListaFeira
import kotlinx.coroutines.flow.Flow

@Dao
interface ListaFeiraDao {

    @Query("SELECT * FROM listas_feira ORDER BY criadaEm DESC")
    fun observarTodas(): Flow<List<ListaFeira>>

    @Transaction
    @Query("SELECT * FROM listas_feira ORDER BY criadaEm DESC")
    fun observarTodasComItens(): Flow<List<ListaComItens>>

    @Transaction
    @Query("SELECT * FROM listas_feira WHERE id = :id")
    fun observarPorId(id: Int): Flow<ListaComItens?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(lista: ListaFeira): Long

    @Update
    suspend fun atualizar(lista: ListaFeira)

    @Delete
    suspend fun deletar(lista: ListaFeira)

    @Query("DELETE FROM listas_feira WHERE id = :id")
    suspend fun deletarPorId(id: Int)
}
