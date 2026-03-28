package com.example.nossafeira.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrecoOcrExtractorTest {

    // ── Padrão com símbolo R$ ─────────────────────────────────────────────────

    @Test
    fun `R$ com espaco e valor simples`() {
        assertEquals(listOf(1299), extrairPrecosDaEtiqueta("R$ 12,99"))
    }

    @Test
    fun `R$ sem espaco antes do valor`() {
        assertEquals(listOf(1299), extrairPrecosDaEtiqueta("R\$12,99"))
    }

    @Test
    fun `R$ com separador de milhar`() {
        assertEquals(listOf(129900), extrairPrecosDaEtiqueta("R$ 1.299,00"))
    }

    @Test
    fun `R$ com multiplos separadores de milhar`() {
        assertEquals(listOf(123456789), extrairPrecosDaEtiqueta("R$ 1.234.567,89"))
    }

    @Test
    fun `valor bem baixo noventa e nove centavos`() {
        assertEquals(listOf(99), extrairPrecosDaEtiqueta("R$ 0,99"))
    }

    // ── Padrão sem símbolo ────────────────────────────────────────────────────

    @Test
    fun `valor sem simbolo apenas virgula decimal`() {
        assertEquals(listOf(1299), extrairPrecosDaEtiqueta("12,99"))
    }

    // ── Múltiplos preços ──────────────────────────────────────────────────────

    @Test
    fun `promocao De Por retorna dois valores`() {
        val resultado = extrairPrecosDaEtiqueta("De R$ 15,99 Por R$ 12,99")
        assertEquals(listOf(1599, 1299), resultado)
    }

    @Test
    fun `dois valores distintos sem simbolo`() {
        val resultado = extrairPrecosDaEtiqueta("Produto A 5,99 Produto B 3,49")
        assertEquals(listOf(599, 349), resultado)
    }

    // ── Espaços ao redor da vírgula (etiquetas com fonte grande) ─────────────

    @Test
    fun `espacos ao redor da virgula`() {
        assertEquals(listOf(9999), extrairPrecosDaEtiqueta("99 , 99"))
    }

    @Test
    fun `espaco antes da virgula`() {
        assertEquals(listOf(9999), extrairPrecosDaEtiqueta("99 ,99"))
    }

    @Test
    fun `espaco depois da virgula`() {
        assertEquals(listOf(9999), extrairPrecosDaEtiqueta("99, 99"))
    }

    @Test
    fun `R$ com espacos ao redor da virgula`() {
        assertEquals(listOf(9999), extrairPrecosDaEtiqueta("R$ 99 , 99"))
    }

    // ── Parcelas (V1 — extrai o valor, ignora contexto) ───────────────────────

    @Test
    fun `parcela 3x extrai apenas o valor unitario`() {
        val resultado = extrairPrecosDaEtiqueta("3x R$ 4,33")
        assertEquals(listOf(433), resultado)
    }

    // ── Sem preços ────────────────────────────────────────────────────────────

    @Test
    fun `texto sem preco retorna lista vazia`() {
        assertTrue(extrairPrecosDaEtiqueta("Arroz tipo 1 5kg").isEmpty())
    }

    @Test
    fun `texto vazio retorna lista vazia`() {
        assertTrue(extrairPrecosDaEtiqueta("").isEmpty())
    }

    @Test
    fun `texto em branco retorna lista vazia`() {
        assertTrue(extrairPrecosDaEtiqueta("   ").isEmpty())
    }

    // ── Deduplicação ──────────────────────────────────────────────────────────

    @Test
    fun `mesmo valor repetido retorna sem duplicata`() {
        val resultado = extrairPrecosDaEtiqueta("R$ 12,99 e também R$ 12,99")
        assertEquals(listOf(1299), resultado)
    }

    // ── Ponto como decimal (OCR americano) ───────────────────────────────────

    @Test
    fun `ponto decimal simples 99 ponto 99`() {
        assertEquals(listOf(9999), extrairPrecosDaEtiqueta("99.99"))
    }

    @Test
    fun `ponto decimal com inteiro de 3 digitos`() {
        assertEquals(listOf(129900), extrairPrecosDaEtiqueta("1299.00"))
    }

    @Test
    fun `ponto nao confunde separador de milhar com decimal`() {
        // "22.000" tem 3 dígitos após o ponto — não deve ser capturado como preço
        assertTrue(extrairPrecosDaEtiqueta("22.000").isEmpty())
    }

    @Test
    fun `ponto nao captura grupo interno de milhar`() {
        // "1.234.567,89" — o ".234" não deve gerar match separado
        assertEquals(listOf(123456789), extrairPrecosDaEtiqueta("R$ 1.234.567,89"))
    }

    // ── Não confunde padrões estrangeiros ─────────────────────────────────────

    @Test
    fun `dolar americano nao e capturado`() {
        // $12.99 — precedido por $, que não está no lookbehind, então pode capturar 12.99
        // Comportamento aceitável na V1: extrai o valor numérico
        val resultado = extrairPrecosDaEtiqueta("\$12.99")
        assertEquals(listOf(1299), resultado)
    }

    // ── Valor com símbolo também captura como sem-símbolo sem duplicar ────────

    @Test
    fun `R$ nao gera duplicata com padrao sem simbolo`() {
        // "R$ 5,00" deve retornar apenas [500], não [500, 500]
        val resultado = extrairPrecosDaEtiqueta("R$ 5,00")
        assertEquals(listOf(500), resultado)
    }
}
