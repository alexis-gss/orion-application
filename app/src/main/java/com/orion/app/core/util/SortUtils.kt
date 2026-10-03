package com.orion.app.core.util

/**
 * Sorts [items] by a nullable, comparable key (e.g. a date). Items with a non-null key are
 * ordered by that key, honoring [descending]. Items with no key ("unknown date") are always
 * pushed to the end of the list and, among themselves, sorted alphabetically by [titleOf] —
 * rather than being placed arbitrarily (or all at the top) as `nullsFirst`/`nullsLast` alone
 * would do, and rather than having their relative order flip when [descending] is toggled.
 */
fun <T, K : Comparable<K>> sortWithUnknownLast(
    items: List<T>,
    descending: Boolean,
    titleOf: (T) -> String,
    keyOf: (T) -> K?,
): List<T> {
    val (known, unknown) = items.partition { keyOf(it) != null }
    val sortedKnown = known.sortedBy { keyOf(it) }.let { if (descending) it.reversed() else it }
    val sortedUnknown = unknown.sortedBy { titleOf(it).lowercase() }
    return sortedKnown + sortedUnknown
}
