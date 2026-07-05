package com.sibi.aem.one.core.learnings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class CalculatorTest {

    @Test
    public void testCalculator() {
        Calculator calculator = new Calculator();
        assertEquals(9, calculator.add(5,4));
        assertEquals(1, calculator.subtract(5,4));
        assertEquals(9, calculator.multiply(3,3));
        assertEquals(9, calculator.divide(36,4));
        assertEquals(9, calculator.square(3));
        assertThrows(ArithmeticException.class, () -> {calculator.divide(1,0);});
        assertTrue(calculator.add(1,0) > 0);
    }

    @Test
    void testCalculatorSpy() {
        Calculator calculatorSpy = spy(Calculator.class);
        doReturn(100).when(calculatorSpy).add(anyInt(), anyInt());
        assertEquals(25, calculatorSpy.square(5));
        assertEquals(100, calculatorSpy.add(2,5));
        assertEquals(100, calculatorSpy.add(10,12));
    }

}
