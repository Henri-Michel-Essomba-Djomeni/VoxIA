package com.voxia.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArithmeticEvaluatorTest {
    @Test
    fun evaluate_respectsOperatorPrecedence() {
        assertEquals(14.0, ArithmeticEvaluator.evaluate("2 + 3 * 4")!!, 0.0001)
    }

    @Test
    fun evaluate_supportsParenthesesAndFrenchWords() {
        assertEquals(20.0, ArithmeticEvaluator.evaluate("(2 plus 3) fois 4")!!, 0.0001)
    }

    @Test
    fun evaluate_supportsUnaryMinus() {
        assertEquals(-6.0, ArithmeticEvaluator.evaluate("-2 * 3")!!, 0.0001)
    }

    @Test
    fun evaluate_rejectsInvalidOrUnsafeInput() {
        assertNull(ArithmeticEvaluator.evaluate("ouvre le fichier secret"))
        assertNull(ArithmeticEvaluator.evaluate("1 / 0"))
        assertNull(ArithmeticEvaluator.evaluate(null))
    }
}
