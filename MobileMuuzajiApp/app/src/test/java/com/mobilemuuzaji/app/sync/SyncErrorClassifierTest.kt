package com.mobilemuuzaji.app.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncErrorClassifierTest {

    @Test
    fun classifiesNotFoundAsPermanentFailure() {
        val decision = SyncErrorClassifier.classify(404, "{\"detail\":\"Item not found\"}")

        assertFalse(decision.shouldRetry)
        assertTrue(decision.shouldDrop)
    }

    @Test
    fun classifiesServerErrorAsTransientFailure() {
        val decision = SyncErrorClassifier.classify(503, "server error")

        assertTrue(decision.shouldRetry)
        assertFalse(decision.shouldDrop)
    }
}
