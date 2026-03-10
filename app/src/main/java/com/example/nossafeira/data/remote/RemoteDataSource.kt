package com.example.nossafeira.data.remote

import com.example.nossafeira.BuildConfig
import com.example.nossafeira.data.remote.api.NossaFeiraApi
import com.example.nossafeira.data.remote.dto.ListaDto
import com.example.nossafeira.data.remote.dto.ListaPageDto
import com.example.nossafeira.data.remote.dto.PostListaRequest
import com.example.nossafeira.data.remote.dto.PutListaRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("x-api-key", BuildConfig.API_KEY)
            .addHeader("x-family-id", BuildConfig.FAMILY_ID)
            .build()
        return chain.proceed(request)
    }
}

class RemoteDataSource {

    private val api: NossaFeiraApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor())
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NossaFeiraApi::class.java)

    suspend fun compartilharLista(body: PostListaRequest): ListaDto =
        api.compartilharLista(body)

    suspend fun getListas(page: Int = 0, pageSize: Int = 50): ListaPageDto =
        api.getListas(page, pageSize)

    suspend fun getLista(id: String): ListaDto =
        api.getLista(id)

    suspend fun atualizarLista(id: String, body: PutListaRequest): ListaDto =
        api.atualizarLista(id, body)

    suspend fun deletarLista(id: String) =
        api.deletarLista(id)
}
