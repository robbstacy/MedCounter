package com.medcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrugNameMatcherTest {
    @Test
    fun toleratesTyposAndOcrMisreads() {
        assertTrue(DrugNameMatcher.matches("Mematine", "Memantine")) // missing letter
        assertTrue(DrugNameMatcher.matches("Metfromin", "Metformin")) // swapped letters
        assertTrue(DrugNameMatcher.matches("Amlod1pine", "Amlodipine")) // OCR 1 for i
        assertTrue(DrugNameMatcher.matches("Metforrnin", "Metformin")) // OCR rn for m
        assertTrue(DrugNameMatcher.matches("Lemborexent", "Lemborexant"))
        assertTrue(DrugNameMatcher.matches("ATORVASTATIN", "atorvastatin"))
        assertTrue(DrugNameMatcher.matches("Hydrochlorothaizide", "Hydrochlorothiazide")) // long name, 2 allowed
    }

    @Test
    fun keepsLookAlikeDrugsApart() {
        assertFalse(DrugNameMatcher.matches("Prednisone", "Prednisolone"))
        assertFalse(DrugNameMatcher.matches("Hydroxyzine", "Hydralazine"))
        assertFalse(DrugNameMatcher.matches("Losartan", "Valsartan"))
        assertFalse(DrugNameMatcher.matches("Tramadol", "Trazodone"))
        assertFalse(DrugNameMatcher.matches("Celexa", "Celebrex"))
        assertFalse(DrugNameMatcher.matches("Memantine", "Lemborexant"))
        assertFalse("short names need an exact match", DrugNameMatcher.matches("Losec", "Lasix"))
    }

    @Test
    fun bestReturnsNullOnTie() {
        val names = listOf("Bolamine", "Belamine")
        assertNull(DrugNameMatcher.best("Balamine", names) { listOf(it) })
        assertEquals("Bolamine", DrugNameMatcher.best("Bolamune", names) { listOf(it) })
    }

    @Test
    fun slipMatchForUsesFuzzyFallback() {
        val scan = SlipScan(
            items = listOf(
                SlipItem(name = "Memantine", brand = "Ebixa"),
                SlipItem(name = "Lemborexant", brand = "Dayvigo"),
                SlipItem(name = "Metformin Hcl"),
            ),
        )
        assertEquals("Memantine", scan.matchFor("Mematine")?.name)
        assertEquals("Lemborexant", scan.matchFor("Dayvigo")?.name)
        assertEquals("Lemborexant", scan.matchFor("Dayvigio")?.name)
        assertEquals("Metformin Hcl", scan.matchFor("Metfromin 500")?.name)
        assertNull(scan.matchFor("Donepezil"))
    }
}
