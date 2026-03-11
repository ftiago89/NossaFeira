package com.example.nossafeira.ui.utils

import com.example.nossafeira.data.model.Categoria
import com.example.nossafeira.data.model.ItemFeira
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtrairQuantidadeNumericaTest {

    @Test
    fun `numero inteiro simples`() {
        assertEquals(3.0, extrairQuantidadeNumerica("3"), 0.001)
    }

    @Test
    fun `numero com unidade separada por espaco`() {
        assertEquals(2.0, extrairQuantidadeNumerica("2 un"), 0.001)
    }

    @Test
    fun `numero decimal com virgula`() {
        assertEquals(1.5, extrairQuantidadeNumerica("1,5 kg"), 0.001)
    }

    @Test
    fun `numero decimal com ponto`() {
        assertEquals(0.5, extrairQuantidadeNumerica("0.5 l"), 0.001)
    }

    @Test
    fun `texto sem numero retorna 1`() {
        assertEquals(1.0, extrairQuantidadeNumerica("texto"), 0.001)
    }

    @Test
    fun `string vazia retorna 1`() {
        assertEquals(1.0, extrairQuantidadeNumerica(""), 0.001)
    }

    @Test
    fun `zero retorna 1 pois nao e maior que zero`() {
        assertEquals(1.0, extrairQuantidadeNumerica("0"), 0.001)
    }

    @Test
    fun `string com espacos ao redor`() {
        assertEquals(4.0, extrairQuantidadeNumerica("  4  "), 0.001)
    }

    @Test
    fun `numero grande`() {
        assertEquals(100.0, extrairQuantidadeNumerica("100 g"), 0.001)
    }
}

class CalcularTotalGastoTest {

    private fun item(
        preco: Int,
        comprado: Boolean,
        quantidade: String = "1"
    ) = ItemFeira(
        listaId = 1,
        nome = "Item",
        quantidade = quantidade,
        categoria = Categoria.OUTROS,
        preco = preco,
        comprado = comprado
    )

    @Test
    fun `lista vazia retorna zero`() {
        assertEquals(0, calcularTotalGasto(emptyList()))
    }

    @Test
    fun `item nao comprado nao e somado`() {
        val itens = listOf(item(preco = 500, comprado = false))
        assertEquals(0, calcularTotalGasto(itens))
    }

    @Test
    fun `item com preco zero nao e somado mesmo se comprado`() {
        val itens = listOf(item(preco = 0, comprado = true))
        assertEquals(0, calcularTotalGasto(itens))
    }

    @Test
    fun `item comprado com preco e somado`() {
        val itens = listOf(item(preco = 999, comprado = true))
        assertEquals(999, calcularTotalGasto(itens))
    }

    @Test
    fun `dois itens comprados sao somados`() {
        val itens = listOf(
            item(preco = 500, comprado = true),
            item(preco = 850, comprado = true)
        )
        assertEquals(1350, calcularTotalGasto(itens))
    }

    @Test
    fun `mistura de comprados e nao comprados soma apenas comprados`() {
        val itens = listOf(
            item(preco = 500, comprado = true),
            item(preco = 300, comprado = false),
            item(preco = 200, comprado = true)
        )
        assertEquals(700, calcularTotalGasto(itens))
    }

    @Test
    fun `quantidade multiplica o preco`() {
        // R$ 5,00 × 3 unidades = R$ 15,00 = 1500 centavos
        val itens = listOf(item(preco = 500, comprado = true, quantidade = "3"))
        assertEquals(1500, calcularTotalGasto(itens))
    }

    @Test
    fun `quantidade decimal multiplica o preco`() {
        // R$ 10,00 × 1.5 kg = R$ 15,00 = 1500 centavos
        val itens = listOf(item(preco = 1000, comprado = true, quantidade = "1,5 kg"))
        assertEquals(1500, calcularTotalGasto(itens))
    }

    @Test
    fun `quantidade textual usa fator 1`() {
        val itens = listOf(item(preco = 799, comprado = true, quantidade = "a gosto"))
        assertEquals(799, calcularTotalGasto(itens))
    }
}
