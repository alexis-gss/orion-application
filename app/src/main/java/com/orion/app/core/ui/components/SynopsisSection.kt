package com.orion.app.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.orion.app.R

/**
 * SHARED COMPONENT — used by cinema, games and books detail screens (moved here from
 * cinema/ui/detail/components, its original, domain-specific location).
 *
 * Renders nothing (returns early) if both [tagline] and [overview] are blank/null, so
 * callers can pass through raw, possibly-empty API fields without an extra guard at the
 * call site.
 */
@Composable
fun SynopsisSection(
    tagline: String?,
    overview: String?,
) {
    val cleanTagline = tagline?.takeIf { it.isNotBlank() }
    val cleanOverview = overview?.takeIf { it.isNotBlank() }

    if (cleanTagline == null && cleanOverview == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionTitle(title = stringResource(R.string.synopsis_title))

        cleanTagline?.let {
            Text(
                text = "\"$it\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }

        cleanOverview?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (cleanTagline != null) 8.dp else 0.dp)
            )
        }
    }
}
