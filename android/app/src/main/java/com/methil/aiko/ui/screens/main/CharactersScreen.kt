package com.methil.aiko.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import com.methil.aiko.bridge.AikoConfig
import com.methil.aiko.service.AuthService
import com.methil.aiko.domain.Character
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable

@Composable
fun CharactersScreen(
    sessionToken: String,
    onCharacterSelect: (Character) -> Unit = {}
) {
    var characters by remember { mutableStateOf<List<Character>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val authService = remember { AuthService(AikoConfig.BASE_URL) }

    LaunchedEffect(sessionToken) {
        authService.getCharacters(sessionToken).onSuccess {
            characters = it
            isLoading = false
        }.onFailure {
            Log.e("CharactersScreen", "Failed to fetch characters", it)
            errorMessage = it.message
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
        } else if (characters.isEmpty()) {
            Text(text = "No characters found.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(characters) { character ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCharacterSelect(character) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (character.image_url != null) {
                                AsyncImage(
                                    model = "${AikoConfig.BASE_URL}${character.image_url}",
                                    contentDescription = character.name,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .padding(end = 16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = character.name, style = MaterialTheme.typography.titleLarge)
                                    if (character.is_public) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "PUBLIC",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Text(text = character.model_name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
