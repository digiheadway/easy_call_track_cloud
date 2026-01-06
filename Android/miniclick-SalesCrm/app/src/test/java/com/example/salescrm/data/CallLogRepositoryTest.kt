package com.example.salescrm.data

import org.junit.Test
import org.junit.Assert.*

class CallLogRepositoryTest {

    @Test
    fun testNormalizePhoneNumber() {
        // Indian Mobile
        assertEquals("9068062563", CallLogRepository.normalizePhoneNumber("+91 90680-62563"))
        assertEquals("9068062563", CallLogRepository.normalizePhoneNumber("09068062563"))
        assertEquals("9068062563", CallLogRepository.normalizePhoneNumber("919068062563"))
        
        // Indian Short Codes
        assertEquals("198", CallLogRepository.normalizePhoneNumber("198"))
        assertEquals("198", CallLogRepository.normalizePhoneNumber("+91198"))
        assertEquals("198", CallLogRepository.normalizePhoneNumber("+91 198"))
        assertEquals("121", CallLogRepository.normalizePhoneNumber("+91 121"))
        
        // US Numbers
        assertEquals("2025550123", CallLogRepository.normalizePhoneNumber("+1 202-555-0123"))
        assertEquals("2025550123", CallLogRepository.normalizePhoneNumber("12025550123"))
        
        // US Short Codes
        assertEquals("911", CallLogRepository.normalizePhoneNumber("+1 911"))
        assertEquals("911", CallLogRepository.normalizePhoneNumber("1911"))
        assertEquals("911", CallLogRepository.normalizePhoneNumber("911"))
        
        // UK Numbers
        assertEquals("7911123456", CallLogRepository.normalizePhoneNumber("+44 7911 123456"))
        
        // UK Short Code
        assertEquals("123", CallLogRepository.normalizePhoneNumber("+44 123"))
        
        // Garbage
        assertEquals("", CallLogRepository.normalizePhoneNumber(""))
        assertEquals("", CallLogRepository.normalizePhoneNumber("   "))
        assertEquals("123", CallLogRepository.normalizePhoneNumber("abc 1-2-3"))
    }
}
