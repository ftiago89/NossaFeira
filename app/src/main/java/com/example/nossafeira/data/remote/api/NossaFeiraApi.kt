package com.example.nossafeira.data.remote.api

import com.example.nossafeira.data.remote.dto.ListaDto
import com.example.nossafeira.data.remote.dto.ListaPageDto
import com.example.nossafeira.data.remote.dto.PostListaRequest
import com.example.nossafeira.data.remote.dto.PutListaRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NossaFeiraApi {

    @POST("listas")
    suspend fun compartilharLista(@Body body: PostListaRequest): ListaDto

    @GET("listas")
    suspend fun getListas(
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 50
    ): ListaPageDto

    @GET("listas/{id}")
    suspend fun getLista(@Path("id") id: String): ListaDto

    @PUT("listas/{id}")
    suspend fun atualizarLista(
        @Path("id") id: String,
        @Body body: PutListaRequest
    ): ListaDto

    @DELETE("listas/{id}")
    suspend fun deletarLista(@Path("id") id: String)
}
