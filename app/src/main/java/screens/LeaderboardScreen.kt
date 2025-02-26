package com.lavrik.koalajump.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@Composable
fun LeaderboardScreen(
    onClose: () -> Unit
) {
    var scores by remember { mutableStateOf<List<Score>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val scoresList = db.collection("scores")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    Score(
                        userName = doc.getString("userName") ?: "Anonymous",
                        score = doc.getLong("score")?.toInt() ?: 0,
                        userPhoto = doc.getString("userPhoto")
                    )
                }
            scores = scoresList
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Leaderboard",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Text("Loading scores...")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(scores) { index, score ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}. ${score.userName}")
                            Text("${score.score}")
                        }
                    }
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
        ) {
            Text("Close")
        }
    }
}

data class Score(
    val userName: String,
    val score: Int,
    val userPhoto: String?
)