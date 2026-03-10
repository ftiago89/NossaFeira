package com.example.nossafeira.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ListaDto(
    @SerializedName("_id")            val id: String,
    @SerializedName("nome")           val nome: String,
    @SerializedName("valorEstimado")  val valorEstimado: Int,
    @SerializedName("valorCalculado") val valorCalculado: Int,
    @SerializedName("criadaEm")       val criadaEm: Long,
    @SerializedName("updatedAt")      val updatedAt: String,
    @SerializedName("itens")          val itens: List<ItemDto>
)

data class ListaPageDto(
    @SerializedName("content")       val content: List<ListaDto>,
    @SerializedName("page")          val page: Int,
    @SerializedName("pageSize")      val pageSize: Int,
    @SerializedName("totalElements") val totalElements: Int
)

data class PostListaRequest(
    @SerializedName("_id")            val id: String,
    @SerializedName("nome")           val nome: String,
    @SerializedName("valorEstimado")  val valorEstimado: Int,
    @SerializedName("valorCalculado") val valorCalculado: Int,
    @SerializedName("criadaEm")       val criadaEm: Long,
    @SerializedName("itens")          val itens: List<ItemDto>
)

data class PutListaRequest(
    @SerializedName("nome")           val nome: String,
    @SerializedName("valorEstimado")  val valorEstimado: Int,
    @SerializedName("valorCalculado") val valorCalculado: Int,
    @SerializedName("itens")          val itens: List<ItemDto>
)
