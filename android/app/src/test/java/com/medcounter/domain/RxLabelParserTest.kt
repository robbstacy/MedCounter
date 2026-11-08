package com.medcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// All label text below is invented demo data.
class RxLabelParserTest {
    @Test
    fun parsesTypicalChainLabel() {
        val text = """
            CVS pharmacy
            123 MAIN ST ANYTOWN (555) 010-0000
            RX# 6123456-07311
            JANE DOE
            TAKE 1 TABLET BY MOUTH DAILY
            LISINOPRIL 10 MG TAB
            QTY: 30   REFILLS: 3
            DR. A. SAMPLE
        """.trimIndent()
        val info = RxLabelParser.parse(text)
        assertEquals("6123456-07311", info.rxNumber)
        assertEquals(3, info.refillsRemaining)
        assertEquals(30.0, info.quantity!!, 0.0)
        assertEquals("CVS", info.pharmacy)
        assertEquals("Lisinopril", info.name)
        assertEquals("10", info.strength)
        assertEquals("mg", info.strengthUnit)
    }

    @Test
    fun parsesRefillsLeftPhrasingAndRxNo() {
        val text = """
            Walgreens
            Rx No: 0712345
            Metformin HCl 500mg
            Qty 60 TAB
            2 Refills left before 03/01/27
        """.trimIndent()
        val info = RxLabelParser.parse(text)
        assertEquals("0712345", info.rxNumber)
        assertEquals(2, info.refillsRemaining)
        assertEquals(60.0, info.quantity!!, 0.0)
        assertEquals("Walgreens", info.pharmacy)
        assertEquals("Metformin HCL", info.name)
        assertEquals("500", info.strength)
    }

    @Test
    fun noRefillsMeansZero() {
        val info = RxLabelParser.parse("RX 5550001\nNO REFILLS - DR AUTH REQUIRED\nQTY 90")
        assertEquals(0, info.refillsRemaining)
        assertEquals("5550001", info.rxNumber)
        assertEquals(90.0, info.quantity!!, 0.0)
    }

    @Test
    fun instructionLineIsNotMistakenForDrugName() {
        val info = RxLabelParser.parse("TAKE 2 TABLETS DAILY\nATORVASTATIN 20 MG")
        assertEquals("Atorvastatin", info.name)
        assertEquals("20", info.strength)
    }

    @Test
    fun garbageYieldsEmpty() {
        val info = RxLabelParser.parse("~~ ## .. ")
        assertTrue(info.isEmpty)
        assertNull(info.rxNumber)
    }

    @Test
    fun shortNumbersAreNotRxNumbers() {
        assertNull(RxLabelParser.parse("RX 12").rxNumber)
    }
}
