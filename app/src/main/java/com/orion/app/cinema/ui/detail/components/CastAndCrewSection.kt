package com.orion.app.cinema.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.orion.app.cinema.data.CastMember
import com.orion.app.cinema.data.CrewMember
import com.orion.app.core.ui.components.SectionTitle
import androidx.compose.ui.res.stringResource
import com.orion.app.R

// Roles surfaced first within the crew (most relevant to a general audience).
private val PRIORITY_CREW_JOBS = listOf(
    "Director", "Writer", "Screenplay", "Creator", "Story",
    "Producer", "Executive Producer", "Director of Photography", "Original Music Composer"
)

@Composable
fun CastAndCrewSection(
    cast: List<CastMember>,
    crew: List<CrewMember> = emptyList()
) {
    val limitedCast = cast.take(15)
    val limitedCrew = remember(crew) {
        crew.distinctBy { it.id }
            .sortedBy { member -> PRIORITY_CREW_JOBS.indexOf(member.job).let { if (it == -1) Int.MAX_VALUE else it } }
            .take(15)
    }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        if (limitedCast.isNotEmpty()) {
            SectionTitle(
                title = stringResource(R.string.cast_title),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(limitedCast) { member ->
                    PersonCard(
                        name = member.name,
                        role = member.character,
                        profilePath = member.profilePath
                    )
                }
            }
        }

        if (limitedCrew.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(
                title = stringResource(R.string.crew_title),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(limitedCrew) { member ->
                    PersonCard(
                        name = member.name,
                        role = member.job,
                        profilePath = member.profilePath
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonCard(name: String, role: String?, profilePath: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(85.dp)
    ) {
        var loadFailed by remember(profilePath) { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (profilePath != null && !loadFailed) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185$profilePath",
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp),
                    onState = { state -> loadFailed = state is AsyncImagePainter.State.Error }
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        role?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}