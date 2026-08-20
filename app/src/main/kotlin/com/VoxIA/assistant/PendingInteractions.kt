package com.voxia.assistant

import com.voxia.utils.ConfirmationParser
import com.voxia.utils.TextNormalizer

internal const val SENSITIVE_ACTION_TIMEOUT_MS = 20_000L
internal const val CONTACT_CHOICE_TIMEOUT_MS = 30_000L
internal const val CONTACT_NAME_TIMEOUT_MS = 30_000L

internal sealed interface ConfirmationResolution {
    data object None : ConfirmationResolution
    data class Confirmed(val token: Long) : ConfirmationResolution
    data class Cancelled(val token: Long) : ConfirmationResolution
    data class Expired(val token: Long) : ConfirmationResolution
    data class Ambiguous(val token: Long) : ConfirmationResolution
}

/**
 * Stores one sensitive action at a time. The monotonic token binds a spoken answer to the exact
 * action that produced the prompt; resolving or expiring the transaction consumes it permanently.
 */
internal class ConfirmationTransactionStore(
    private val timeoutMillis: Long = SENSITIVE_ACTION_TIMEOUT_MS,
    private val nowMillis: () -> Long
) {
    private data class Pending(
        val token: Long,
        val expiresAtMillis: Long,
        val action: () -> Unit
    )

    private var nextToken = 0L
    private var pending: Pending? = null

    fun begin(action: () -> Unit): Long {
        val token = ++nextToken
        pending = Pending(token, nowMillis() + timeoutMillis, action)
        return token
    }

    fun resolve(spokenAnswer: String): ConfirmationResolution {
        val transaction = pending ?: return ConfirmationResolution.None
        if (nowMillis() >= transaction.expiresAtMillis) {
            pending = null
            return ConfirmationResolution.Expired(transaction.token)
        }
        return when (ConfirmationParser.parse(spokenAnswer)) {
            true -> {
                pending = null
                transaction.action.invoke()
                ConfirmationResolution.Confirmed(transaction.token)
            }
            false -> {
                pending = null
                ConfirmationResolution.Cancelled(transaction.token)
            }
            null -> ConfirmationResolution.Ambiguous(transaction.token)
        }
    }

    fun expire(token: Long): Boolean {
        val transaction = pending ?: return false
        if (transaction.token != token || nowMillis() < transaction.expiresAtMillis) return false
        pending = null
        return true
    }

    fun clear() {
        pending = null
    }
}

internal data class ContactCandidate(
    val displayName: String,
    val number: String,
    val typeLabel: String
) {
    fun spokenDescriptionFr(): String = spokenDescription("finissant par")

    fun spokenDescriptionEn(): String = spokenDescription("ending in")

    private fun spokenDescription(suffixLabel: String): String {
        val digits = number.filter(Char::isDigit)
        val suffix = digits.takeLast(4).toCharArray().joinToString(" ")
        return buildString {
            append(displayName)
            if (typeLabel.isNotBlank()) append(", ").append(typeLabel)
            if (suffix.isNotBlank()) append(", ").append(suffixLabel).append(" ").append(suffix)
        }
    }
}

internal sealed interface ContactNameResolution {
    data object None : ContactNameResolution
    data class Provided(val token: Long, val name: String) : ContactNameResolution
    data class Cancelled(val token: Long) : ContactNameResolution
    data class Expired(val token: Long) : ContactNameResolution
    data class Invalid(val token: Long) : ContactNameResolution
}

internal class ContactNameRequestStore(
    private val timeoutMillis: Long = CONTACT_NAME_TIMEOUT_MS,
    private val nowMillis: () -> Long
) {
    private data class Pending(val token: Long, val expiresAtMillis: Long)

    private var nextToken = 0L
    private var pending: Pending? = null

    fun begin(): Long {
        val token = ++nextToken
        pending = Pending(token, nowMillis() + timeoutMillis)
        return token
    }

    fun resolve(spokenName: String): ContactNameResolution {
        val request = pending ?: return ContactNameResolution.None
        if (nowMillis() >= request.expiresAtMillis) {
            pending = null
            return ContactNameResolution.Expired(request.token)
        }
        if (ConfirmationParser.parse(spokenName) == false) {
            pending = null
            return ContactNameResolution.Cancelled(request.token)
        }
        val name = spokenName.trim()
        if (name.isBlank()) return ContactNameResolution.Invalid(request.token)
        pending = null
        return ContactNameResolution.Provided(request.token, name)
    }

    fun expire(token: Long): Boolean {
        val request = pending ?: return false
        if (request.token != token || nowMillis() < request.expiresAtMillis) return false
        pending = null
        return true
    }

    fun clear() {
        pending = null
    }
}

internal sealed interface ContactMatch {
    data object NotFound : ContactMatch
    data class Unique(val candidate: ContactCandidate) : ContactMatch
    data class RequiresChoice(val candidates: List<ContactCandidate>) : ContactMatch
    data class TooMany(val count: Int) : ContactMatch
}

/** Never guesses between multiple phone rows. Exact display-name matches are preferred safely. */
internal fun selectContactMatch(
    query: String,
    candidates: List<ContactCandidate>,
    maximumChoices: Int = 5
): ContactMatch {
    val distinct = candidates.distinctBy {
        Triple(TextNormalizer.normalize(it.displayName), it.number.filter(Char::isDigit), it.typeLabel)
    }
    if (distinct.isEmpty()) return ContactMatch.NotFound

    val normalizedQuery = TextNormalizer.normalize(query)
    val exact = distinct.filter { TextNormalizer.normalize(it.displayName) == normalizedQuery }
    val relevant = exact.ifEmpty { distinct }
    return when {
        relevant.size == 1 -> ContactMatch.Unique(relevant.single())
        relevant.size > maximumChoices -> ContactMatch.TooMany(relevant.size)
        else -> ContactMatch.RequiresChoice(relevant.sortedWith(compareBy(ContactCandidate::displayName, ContactCandidate::typeLabel, ContactCandidate::number)))
    }
}

internal sealed interface ContactChoiceResolution {
    data object None : ContactChoiceResolution
    data class Selected(val token: Long, val candidate: ContactCandidate) : ContactChoiceResolution
    data class Cancelled(val token: Long) : ContactChoiceResolution
    data class Expired(val token: Long) : ContactChoiceResolution
    data class Ambiguous(val token: Long) : ContactChoiceResolution
    data class Invalid(val token: Long) : ContactChoiceResolution
}

internal class ContactChoiceStore(
    private val timeoutMillis: Long = CONTACT_CHOICE_TIMEOUT_MS,
    private val nowMillis: () -> Long
) {
    private data class Pending(
        val token: Long,
        val expiresAtMillis: Long,
        val candidates: List<ContactCandidate>
    )

    private var nextToken = 0L
    private var pending: Pending? = null

    fun begin(candidates: List<ContactCandidate>): Long {
        require(candidates.size >= 2) { "At least two contacts are required for disambiguation" }
        val token = ++nextToken
        pending = Pending(token, nowMillis() + timeoutMillis, candidates.toList())
        return token
    }

    fun resolve(spokenChoice: String): ContactChoiceResolution {
        val choice = pending ?: return ContactChoiceResolution.None
        if (nowMillis() >= choice.expiresAtMillis) {
            pending = null
            return ContactChoiceResolution.Expired(choice.token)
        }
        if (ConfirmationParser.parse(spokenChoice) == false) {
            pending = null
            return ContactChoiceResolution.Cancelled(choice.token)
        }

        val normalized = TextNormalizer.normalize(spokenChoice)
        val ordinal = parseOrdinal(normalized)
        if (ordinal != null) {
            val selected = choice.candidates.getOrNull(ordinal - 1)
                ?: return ContactChoiceResolution.Invalid(choice.token)
            pending = null
            return ContactChoiceResolution.Selected(choice.token, selected)
        }

        val nameMatches = choice.candidates.filter {
            TextNormalizer.normalize(it.displayName) == normalized ||
                TextNormalizer.normalize(it.spokenDescriptionFr()) == normalized ||
                TextNormalizer.normalize(it.spokenDescriptionEn()) == normalized
        }
        return when (nameMatches.size) {
            1 -> {
                pending = null
                ContactChoiceResolution.Selected(choice.token, nameMatches.single())
            }
            0 -> ContactChoiceResolution.Invalid(choice.token)
            else -> ContactChoiceResolution.Ambiguous(choice.token)
        }
    }

    fun expire(token: Long): Boolean {
        val choice = pending ?: return false
        if (choice.token != token || nowMillis() < choice.expiresAtMillis) return false
        pending = null
        return true
    }

    fun clear() {
        pending = null
    }

    private fun parseOrdinal(normalized: String): Int? {
        val value = normalized.removePrefix("le ").removePrefix("la ").removePrefix("numero ").trim()
        return when (value) {
            "1", "un", "une", "premier", "premiere", "first" -> 1
            "2", "deux", "deuxieme", "second", "seconde", "second one" -> 2
            "3", "trois", "troisieme", "third" -> 3
            "4", "quatre", "quatrieme", "fourth" -> 4
            "5", "cinq", "cinquieme", "fifth" -> 5
            else -> null
        }
    }
}
