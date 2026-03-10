package com.example.nossafeira.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class Categoria { HORTIFRUTI, LATICINIOS, LIMPEZA, OUTROS, PROTEINAS, PADARIA }

@Entity(
    tableName = "itens_feira",
    foreignKeys = [ForeignKey(
        entity = ListaFeira::class,
        parentColumns = ["id"],
        childColumns = ["listaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("listaId")]
)
data class ItemFeira(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listaId: Int,
    val nome: String,
    val quantidade: String,
    val categoria: Categoria,
    val preco: Int = 0,
    val comprado: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis(),
    val remoteItemId: String = UUID.randomUUID().toString()
)
