package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTest {

    @Test
    public void testCalculator() {
        Calculator calculator = new Calculator();
        assertEquals(9, calculator.add(5,4));
        assertEquals(1, calculator.subtract(5,4));
        assertEquals(9, calculator.multiply(3,3));
        assertEquals(9, calculator.divide(36,4));
        assertThrows(ArithmeticException.class, () -> {calculator.divide(1,0);});
        assertTrue(calculator.add(1,0) > 0);
    }

}
