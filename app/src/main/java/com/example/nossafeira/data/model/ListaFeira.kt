package com.example.nossafeira.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listas_feira")
data class ListaFeira(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val valorEstimado: Int = 0,
    val criadaEm: Long = System.currentTimeMillis(),
    val remoteId: String? = null,
    val isShared: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long = 0L
)
