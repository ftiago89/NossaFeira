package com.example.nossafeira.ui.utils

import com.example.nossafeira.data.model.ItemFeira
import kotlin.math.roundToInt

/**
 * Extrai o número inicial de uma string de quantidade livre.
 * "2 un" → 2.0 | "1,5 kg" → 1.5 | "3" → 3.0 | "texto" → 1.0
 */
fun extrairQuantidadeNumerica(quantidade: String): Double =
    quantidade.trim()
        .takeWhile { it.isDigit() || it == ',' || it == '.' }
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 } ?: 1.0

/**
 * Soma o preço (em centavos) dos itens comprados, multiplicado pela quantidade.
 */
fun calcularTotalGasto(itens: List<ItemFeira>): Int =
    itens
        .filter { it.comprado && it.preco > 0 }
        .sumOf { it.preco * extrairQuantidadeNumerica(it.quantidade) }
        .roundToInt()
