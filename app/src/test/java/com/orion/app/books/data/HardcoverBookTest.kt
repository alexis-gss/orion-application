package com.orion.app.books.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure logic test, no Robolectric needed: these are plain computed properties on
 * [HardcoverBookRaw] / [HardcoverBook], no Android dependency involved.
 *
 * Replaces the old GoogleVolumeTest (Google Books-specific: ISBN presence, cover
 * requirement, webReaderLink/viewability) which no longer applies since the switch to
 * Hardcover — isDisplayable only requires a non-blank title (see BooksRepository, which
 * is responsible for the ghost-entry filtering that used to live on the model itself),
 * and previewUrl now points to the public Hardcover book page built from its slug.
 */
class HardcoverBookTest {

    private fun rawBook(
        id: Int = 1,
        title: String = "Berserk Tome 36",
        slug: String? = "berserk-tome-36",
        rating: Double? = 4.5,
    ) = HardcoverBookRaw(id = id, title = title, slug = slug, rating = rating)

    // ----- isDisplayable -----

    @Test
    fun `isDisplayable est vrai pour un livre avec un titre`() {
        assertTrue(rawBook().toHardcoverBook().isDisplayable)
    }

    @Test
    fun `isDisplayable est faux si le titre est vide`() {
        assertFalse(rawBook(title = "").toHardcoverBook().isDisplayable)
    }

    @Test
    fun `isDisplayable est faux si le titre est uniquement des espaces`() {
        assertFalse(rawBook(title = "   ").toHardcoverBook().isDisplayable)
    }

    // ----- previewUrl : lien public Hardcover construit depuis le slug -----

    @Test
    fun `previewUrl est nul si Hardcover ne renvoie aucun slug`() {
        assertNull(rawBook(slug = null).toHardcoverBook().previewUrl)
    }

    @Test
    fun `previewUrl pointe vers la fiche publique Hardcover a partir du slug`() {
        val result = rawBook(slug = "berserk-tome-36").toHardcoverBook().previewUrl
        assertEquals("https://hardcover.app/books/berserk-tome-36", result)
    }

    // ----- displayRating : conversion /5 vers /10 -----

    @Test
    fun `displayRating convertit la note sur 5 en note sur 10`() {
        assertEquals(9.0, rawBook(rating = 4.5).toHardcoverBook().displayRating!!, 0.0001)
    }

    @Test
    fun `displayRating est nul si Hardcover ne renvoie aucune note`() {
        assertNull(rawBook(rating = null).toHardcoverBook().displayRating)
    }
}
