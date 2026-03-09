package com.example.nossafeira.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ListaComItens(
    @Embedded val lista: ListaFeira,
    @Relation(
        parentColumn = "id",
        entityColumn = "listaId"
    )
    val itens: List<ItemFeira>
)
