package com.example.nossafeira.ui.utils

/**
 * Extrai preços em reais de um texto obtido via OCR.
 *
 * Padrões reconhecidos:
 *  - R$ 12,99  /  R$12,99           (vírgula decimal com símbolo)
 *  - R$ 1.299,00                    (ponto milhar + vírgula decimal)
 *  - 12,99                          (sem símbolo, vírgula decimal)
 *  - 99 , 99                        (espaços ao redor da vírgula)
 *  - 99.99                          (ponto decimal — OCR às vezes usa padrão americano)
 *
 * Ignorados: parcelas (3x R$ 4,33), moedas estrangeiras ($12.99, EUR 5,99).
 *
 * @return Lista de valores em centavos, na ordem de aparição, sem duplicatas.
 */
fun extrairPrecosDaEtiqueta(texto: String): List<Int> {
    if (texto.isBlank()) return emptyList()

    val vistos = mutableSetOf<Int>()
    val resultado = mutableListOf<Int>()
    val rangesJaCapturados = mutableListOf<IntRange>()

    fun registrar(inteiro: String, decimal: String, range: IntRange) {
        val inteiroSemPontos = inteiro.replace(".", "")
        val centavos = inteiroSemPontos.toLongOrNull()?.times(100)?.plus(decimal.toInt())
        if (centavos != null && centavos > 0 && centavos <= Int.MAX_VALUE) {
            val valor = centavos.toInt()
            if (vistos.add(valor)) resultado.add(valor)
            rangesJaCapturados.add(range)
        }
    }

    fun jaCoberto(range: IntRange) = rangesJaCapturados.any { r ->
        range.first >= r.first && range.last <= r.last
    }

    // 1. R$ com vírgula decimal (e opcional separador de milhar com ponto)
    val reaisVirgula = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})*)\s*,\s*(\d{2})""")
    for (m in reaisVirgula.findAll(texto)) {
        registrar(m.groupValues[1], m.groupValues[2], m.range)
    }

    // 2. Sem símbolo, vírgula decimal — ex: "12,99" ou "99 , 99"
    // Lookbehind exclui dígitos e letras; lookahead exclui terceiro dígito após vírgula
    val semSimboloVirgula = Regex("""(?<![A-Za-z\d])(\d{1,3}(?:\.\d{3})*)\s*,\s*(\d{2})(?!\d)""")
    for (m in semSimboloVirgula.findAll(texto)) {
        if (!jaCoberto(m.range)) registrar(m.groupValues[1], m.groupValues[2], m.range)
    }

    // 3. Ponto como decimal — OCR lê "99.99" em vez de "99,99"
    // Lookbehind também exclui ponto para não capturar grupos internos de milhar (ex: "1.234.56")
    val pontoDecimal = Regex("""(?<![A-Za-z\d.])(\d{1,4})\.(\d{2})(?!\d)""")
    for (m in pontoDecimal.findAll(texto)) {
        if (!jaCoberto(m.range)) registrar(m.groupValues[1], m.groupValues[2], m.range)
    }

    return resultado
}
