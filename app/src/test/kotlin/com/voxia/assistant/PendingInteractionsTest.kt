package com.voxia.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingInteractionsTest {
    private var now = 1_000L

    @Test
    fun `confirmation executes the bound action once`() {
        var executions = 0
        val store = ConfirmationTransactionStore(timeoutMillis = 100, nowMillis = { now })
        val token = store.begin { executions++ }

        val resolution = store.resolve("oui") as ConfirmationResolution.Confirmed

        assertEquals(token, resolution.token)
        assertEquals(1, executions)
        assertEquals(ConfirmationResolution.None, store.resolve("oui"))
    }

    @Test
    fun `new confirmation invalidates the previous token and action`() {
        var firstExecutions = 0
        var secondExecutions = 0
        val store = ConfirmationTransactionStore(timeoutMillis = 100, nowMillis = { now })
        val oldToken = store.begin { firstExecutions++ }
        val currentToken = store.begin { secondExecutions++ }

        now += 100
        assertFalse(store.expire(oldToken))
        now -= 100
        val resolution = store.resolve("yes") as ConfirmationResolution.Confirmed

        assertEquals(currentToken, resolution.token)
        assertEquals(0, firstExecutions)
        assertEquals(1, secondExecutions)
    }

    @Test
    fun `ambiguous confirmation keeps transaction pending`() {
        val store = ConfirmationTransactionStore(timeoutMillis = 100, nowMillis = { now })
        val token = store.begin {}

        assertEquals(ConfirmationResolution.Ambiguous(token), store.resolve("peut etre"))
        assertTrue(store.resolve("non") is ConfirmationResolution.Cancelled)
        assertEquals(ConfirmationResolution.None, store.resolve("oui"))
    }

    @Test
    fun `confirmation expires at deadline and cannot be replayed`() {
        var executions = 0
        val store = ConfirmationTransactionStore(timeoutMillis = 100, nowMillis = { now })
        val token = store.begin { executions++ }

        now += 100
        assertEquals(ConfirmationResolution.Expired(token), store.resolve("oui"))
        assertEquals(ConfirmationResolution.None, store.resolve("oui"))
        assertEquals(0, executions)
    }

    @Test
    fun `expiration callback consumes only matching current token`() {
        val store = ConfirmationTransactionStore(timeoutMillis = 100, nowMillis = { now })
        val obsoleteToken = store.begin {}
        val currentToken = store.begin {}

        now += 100
        assertFalse(store.expire(obsoleteToken))
        assertTrue(store.expire(currentToken))
        assertFalse(store.expire(currentToken))
    }

    @Test
    fun `contact matching returns unique exact candidate over partial matches`() {
        val exact = contact("Jean", "0101")
        val partial = contact("Jean Pierre", "0202")

        assertEquals(ContactMatch.Unique(exact), selectContactMatch("Jean", listOf(partial, exact)))
    }

    @Test
    fun `contact matching never guesses between duplicate phone rows`() {
        val mobile = contact("Jean", "0101", "Mobile")
        val work = contact("Jean", "0202", "Travail")

        val result = selectContactMatch("Jean", listOf(work, mobile)) as ContactMatch.RequiresChoice

        assertEquals(listOf(mobile, work), result.candidates)
    }

    @Test
    fun `contact matching collapses identical rows and reports large ambiguity`() {
        val duplicate = contact("Jean", "0101")
        assertEquals(ContactMatch.Unique(duplicate), selectContactMatch("Jean", listOf(duplicate, duplicate)))

        val many = (1..6).map { contact("Jean $it", "000$it") }
        assertEquals(ContactMatch.TooMany(6), selectContactMatch("Jean", many))
    }

    @Test
    fun `contact choice accepts explicit ordinal and consumes session`() {
        val first = contact("Jean", "0101")
        val second = contact("Jeanne", "0202")
        val store = ContactChoiceStore(timeoutMillis = 100, nowMillis = { now })
        val token = store.begin(listOf(first, second))

        assertEquals(ContactChoiceResolution.Selected(token, second), store.resolve("le deuxième"))
        assertEquals(ContactChoiceResolution.None, store.resolve("1"))
    }

    @Test
    fun `contact choice requires an ordinal when names remain identical`() {
        val store = ContactChoiceStore(timeoutMillis = 100, nowMillis = { now })
        val token = store.begin(listOf(contact("Jean", "0101"), contact("Jean", "0202")))

        assertEquals(ContactChoiceResolution.Ambiguous(token), store.resolve("Jean"))
        assertEquals(ContactChoiceResolution.Invalid(token), store.resolve("numéro 8"))
        assertTrue(store.resolve("premier") is ContactChoiceResolution.Selected)
    }

    @Test
    fun `contact choice cancellation and expiration are terminal`() {
        val candidates = listOf(contact("Jean", "0101"), contact("Jeanne", "0202"))
        val cancelledStore = ContactChoiceStore(timeoutMillis = 100, nowMillis = { now })
        val cancelledToken = cancelledStore.begin(candidates)
        assertEquals(ContactChoiceResolution.Cancelled(cancelledToken), cancelledStore.resolve("annule"))
        assertEquals(ContactChoiceResolution.None, cancelledStore.resolve("1"))

        val expiredStore = ContactChoiceStore(timeoutMillis = 100, nowMillis = { now })
        val expiredToken = expiredStore.begin(candidates)
        now += 100
        assertEquals(ContactChoiceResolution.Expired(expiredToken), expiredStore.resolve("1"))
    }

    @Test
    fun `obsolete contact choice expiration callback cannot cancel replacement`() {
        val candidates = listOf(contact("Jean", "0101"), contact("Jeanne", "0202"))
        val store = ContactChoiceStore(timeoutMillis = 100, nowMillis = { now })
        val oldToken = store.begin(candidates)
        val currentToken = store.begin(candidates.reversed())

        now += 100
        assertFalse(store.expire(oldToken))
        assertTrue(store.expire(currentToken))
    }

    @Test
    fun `contact name request is tokenized expiring and cancellable`() {
        val store = ContactNameRequestStore(timeoutMillis = 100, nowMillis = { now })
        val oldToken = store.begin()
        val currentToken = store.begin()

        now += 100
        assertFalse(store.expire(oldToken))
        assertTrue(store.expire(currentToken))

        val cancelToken = store.begin()
        assertEquals(ContactNameResolution.Cancelled(cancelToken), store.resolve("non"))
        assertEquals(ContactNameResolution.None, store.resolve("Jean"))
    }

    @Test
    fun `contact name request returns provided name once before expiry`() {
        val store = ContactNameRequestStore(timeoutMillis = 100, nowMillis = { now })
        val token = store.begin()

        assertEquals(ContactNameResolution.Provided(token, "Jean Dupont"), store.resolve(" Jean Dupont "))
        assertEquals(ContactNameResolution.None, store.resolve("Paul"))
    }

    private fun contact(name: String, number: String, type: String = "Mobile") =
        ContactCandidate(name, number, type)
}
