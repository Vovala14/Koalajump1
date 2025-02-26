package com.lavrik.koalajump.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal data class CharacterData(
    val id: Int,
    val name: String
)

@Composable
fun SelectCharacterScreen(
    selectedCharacter: Int?,
    onCharacterSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Your Koala",
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            val characters = listOf(
                CharacterData(1, "Sport Koala"),
                CharacterData(2, "Fitness Koala"),
                CharacterData(3, "Ninja Koala")
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(characters) { character ->
                    CharacterCard(
                        character = character,
                        isSelected = selectedCharacter == character.id,
                        onClick = { onCharacterSelected(character.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(
    character: CharacterData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(120.dp)
            .height(160.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val resourceName = when(character.id) {
                1 -> "koala_sport"
                2 -> "koala_fitness"
                else -> "koala_ninja"
            }

            val resourceId = remember(resourceName) {
                context.resources.getIdentifier(resourceName, "drawable", context.packageName)
            }

            if (resourceId != 0) {
                Image(
                    painter = painterResource(id = resourceId),
                    contentDescription = character.name,
                    modifier = Modifier.size(80.dp)
                )
            } else {
                Text(text = "Image not found")
            }

            Text(
                text = character.name,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}