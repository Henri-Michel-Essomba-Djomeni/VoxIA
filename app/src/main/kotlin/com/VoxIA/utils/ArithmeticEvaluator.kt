package com.voxia.utils

import java.util.ArrayDeque

object ArithmeticEvaluator {
    fun evaluate(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val expression = TextNormalizer.normalize(raw)
            .replace("multiplie par", "*").replace("fois", "*")
            .replace("divise par", "/").replace("sur", "/")
            .replace("plus", "+").replace("moins", "-")
            .replace(',', '.')
        if (!expression.matches(Regex("[0-9+*/().\\-\\s]+"))) return null
        return runCatching { evaluateTokens(tokenize(expression)) }
            .getOrNull()?.takeIf { it.isFinite() }
    }

    private fun tokenize(value: String): List<String> = Regex("\\d+(?:\\.\\d+)?|[()+*/-]")
        .findAll(value).map { it.value }.toList()

    private fun evaluateTokens(tokens: List<String>): Double {
        val values = ArrayDeque<Double>()
        val operators = ArrayDeque<Char>()
        var expectUnary = true
        tokens.forEach { token ->
            token.toDoubleOrNull()?.let {
                values.addLast(it)
                expectUnary = false
                return@forEach
            }
            val op = token.single()
            if (op == '(') {
                operators.addLast(op)
                expectUnary = true
            } else if (op == ')') {
                while (operators.isNotEmpty() && operators.last() != '(') apply(values, operators.removeLast())
                require(operators.isNotEmpty())
                operators.removeLast()
                expectUnary = false
            } else {
                if (op == '-' && expectUnary) values.addLast(0.0)
                while (operators.isNotEmpty() && operators.last() != '(' && precedence(operators.last()) >= precedence(op)) {
                    apply(values, operators.removeLast())
                }
                operators.addLast(op)
                expectUnary = true
            }
        }
        while (operators.isNotEmpty()) apply(values, operators.removeLast())
        require(values.size == 1)
        return values.last()
    }

    private fun precedence(op: Char) = if (op == '*' || op == '/') 2 else 1

    private fun apply(values: ArrayDeque<Double>, op: Char) {
        require(values.size >= 2)
        val right = values.removeLast()
        val left = values.removeLast()
        values.addLast(when (op) {
            '+' -> left + right
            '-' -> left - right
            '*' -> left * right
            '/' -> left / right
            else -> error("Operateur invalide")
        })
    }
}
