package com.orion.app.games.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure logic test, no Robolectric needed: plain computed properties on a
 * @Serializable data class, no Android dependency involved.
 *
 * editionIds / hasRelatedContent back the reasoning used in
 * [GamesRepositorySearchTest] to filter DLC/expansions/editions out of search results
 * (parent_game / version_parent were the reliable IGDB signal, category was not — see
 * the "Phantom Liberty still shows up" fix).
 */
class IgdbGameTest {

    private fun game(
        parentGame: Int? = null,
        versionParent: Int? = null,
        expansions: List<Int> = emptyList(),
        dlcs: List<Int> = emptyList(),
        rating: Double? = null,
        aggregatedRating: Double? = null,
        totalRating: Double? = null,
    ) = IgdbGame(
        id = 1,
        name = "Cyberpunk 2077",
        parentGame = parentGame,
        versionParent = versionParent,
        expansions = expansions,
        dlcs = dlcs,
        rating = rating,
        aggregatedRating = aggregatedRating,
        totalRating = totalRating,
    )

    // ----- editionIds / hasRelatedContent -----

    @Test
    fun `un jeu de base sans parent n'a aucun editionId`() {
        assertTrue(game(parentGame = null, versionParent = null).editionIds.isEmpty())
    }

    @Test
    fun `un DLC avec parent_game a un editionId non vide`() {
        val dlc = game(parentGame = 1091, versionParent = null)
        assertEquals(listOf(1091), dlc.editionIds)
    }

    @Test
    fun `parentGame et versionParent identiques ne sont comptes qu'une fois`() {
        val game = game(parentGame = 42, versionParent = 42)
        assertEquals(listOf(42), game.editionIds)
    }

    @Test
    fun `hasRelatedContent est faux pour des dlcs seuls, meme sans parent_game`() {
        assertFalse(game(dlcs = listOf(101, 102)).hasRelatedContent)
    }

    @Test
    fun `hasRelatedContent est vrai des qu'il y a des expansions`() {
        assertTrue(game(expansions = listOf(55)).hasRelatedContent)
    }

    @Test
    fun `hasRelatedContent est faux pour un jeu de base sans aucun contenu lie`() {
        assertFalse(game().hasRelatedContent)
    }

    // ----- displayRating : priorite aux notes joueurs -----

    @Test
    fun `displayRating privilegie rating sur aggregatedRating et totalRating`() {
        val result = game(rating = 8.5, aggregatedRating = 9.0, totalRating = 7.0).displayRating
        assertEquals(8.5, result)
    }

    @Test
    fun `displayRating retombe sur aggregatedRating si rating est absent`() {
        val result = game(rating = null, aggregatedRating = 9.0, totalRating = 7.0).displayRating
        assertEquals(9.0, result)
    }

    @Test
    fun `displayRating retombe sur totalRating en dernier recours`() {
        val result = game(rating = null, aggregatedRating = null, totalRating = 7.0).displayRating
        assertEquals(7.0, result)
    }

    @Test
    fun `displayRating est nul si aucune note n'est disponible`() {
        assertEquals(null, game(rating = null, aggregatedRating = null, totalRating = null).displayRating)
    }
}