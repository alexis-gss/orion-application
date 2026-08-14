package com.orion.app.core.util

import com.orion.app.core.util.StringUtils.stripHtmlTags
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure logic test, no Robolectric needed: stripHtmlTags has no Android dependency.
 * Covers the HTML shapes Google Books actually sends in `volumeInfo.description`
 * (see books/ui search discussion: descriptions arrive as raw HTML with <p>, <br>,
 * basic inline tags, and a handful of named entities).
 */
class StringUtilsTest {

    @Test
    fun `stripHtmlTags renvoie une chaine vide inchangee`() {
        assertEquals("", "".stripHtmlTags())
    }

    @Test
    fun `stripHtmlTags retire les balises br en les remplacant par un saut de ligne`() {
        assertEquals("Ligne 1\nLigne 2", "Ligne 1<br>Ligne 2".stripHtmlTags())
        assertEquals("Ligne 1\nLigne 2", "Ligne 1<br/>Ligne 2".stripHtmlTags())
        assertEquals("Ligne 1\nLigne 2", "Ligne 1<br />Ligne 2".stripHtmlTags())
    }

    @Test
    fun `stripHtmlTags separe les paragraphes par une ligne vide`() {
        assertEquals(
            "Premier paragraphe\n\nSecond paragraphe",
            "<p>Premier paragraphe</p><p>Second paragraphe</p>".stripHtmlTags()
        )
    }

    @Test
    fun `stripHtmlTags retire les balises inline sans laisser de trace`() {
        assertEquals(
            "Du texte en gras et en italique",
            "Du texte en <b>gras</b> et en <i>italique</i>".stripHtmlTags()
        )
    }

    @Test
    fun `stripHtmlTags decode les entites HTML courantes`() {
        assertEquals(
            "Tom & Jerry <3 \"guillemets\" c'est bon",
            "Tom &amp; Jerry &lt;3 &quot;guillemets&quot; c&#39;est bon".stripHtmlTags()
        )
        assertEquals("Un espace insecable", "Un espace insecable".stripHtmlTags())
        assertEquals("A B", "A&nbsp;B".stripHtmlTags())
    }

    @Test
    fun `stripHtmlTags reduit les sauts de ligne multiples a deux au maximum`() {
        val input = "A<br><br><br><br>B"
        assertEquals("A\n\nB", input.stripHtmlTags())
    }

    @Test
    fun `stripHtmlTags retire les espaces de debut et fin`() {
        assertEquals("Texte propre", "  <p>Texte propre</p>  ".stripHtmlTags())
    }

    @Test
    fun `stripHtmlTags laisse le texte brut intact quand il n'y a aucune balise`() {
        assertEquals("Un texte tout a fait normal.", "Un texte tout a fait normal.".stripHtmlTags())
    }
}
