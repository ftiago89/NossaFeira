package com.example.nossafeira.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ItemDto(
    @SerializedName("id")       val id: String,
    @SerializedName("nome")     val nome: String,
    @SerializedName("quantidade") val quantidade: String,
    @SerializedName("categoria")  val categoria: String,
    @SerializedName("preco")    val preco: Int,
    @SerializedName("comprado") val comprado: Boolean,
    @SerializedName("criadoEm") val criadoEm: Long
)
