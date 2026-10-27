package com.capsula.app.domain

import com.capsula.app.data.model.Mood
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CapsuleValidatorTest {

    private val now = 1_000_000L

    @Test
    fun `valid capsule passes`() {
        val result = CapsuleValidator.validate("Title", "A long enough letter", Mood.GOOD, now + 1, now)
        assertTrue(result.isValid)
    }

    @Test
    fun `blank title, short message, missing mood and past date are all reported`() {
        val result = CapsuleValidator.validate("  ", "short", null, now - 1, now)
        assertFalse(result.isValid)
        assertNotNull(result.titleError)
        assertNotNull(result.messageError)
        assertNotNull(result.moodError)
        assertNotNull(result.unlockError)
    }
}
