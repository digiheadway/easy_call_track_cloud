package com.example.deviceadmin

import org.junit.Test
import org.junit.Assert.*

class PinTest {
    private val MASTER_PIN = "1133"

    @Test
    fun testMasterPin() {
        assertEquals("1133", MASTER_PIN)
    }

    @Test
    fun testPinLogic() {
        val entry = "1133"
        assertTrue(entry == MASTER_PIN)
        
        val wrongEntry = "1234"
        assertFalse(wrongEntry == MASTER_PIN)
    }
}
