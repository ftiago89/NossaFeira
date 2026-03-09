package com.example.nossafeira.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val preco: Double = 0.0,
    val comprado: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis()
)
