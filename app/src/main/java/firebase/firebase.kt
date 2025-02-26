package com.lavrik.koalajump.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HighScore(
    val playerName: String = "",
    val score: Int = 0,
    val characterId: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

class FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val highScoresCollection = db.collection("high_scores")

    // Save a new high score
    suspend fun saveHighScore(highScore: HighScore): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                highScoresCollection.add(highScore).await()
            }
            Log.d("Firebase", "High score saved successfully")
            true
        } catch (e: Exception) {
            Log.e("Firebase", "Error saving high score", e)
            false
        }
    }

    // Get top 10 high scores
    suspend fun getTopHighScores(limit: Int = 10): List<HighScore> {
        return try {
            withContext(Dispatchers.IO) {
                highScoresCollection
                    .orderBy("score", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                    .toObjects(HighScore::class.java)
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error getting high scores", e)
            emptyList()
        }
    }

    // Check if the score qualifies as a high score
    suspend fun isHighScore(score: Int): Boolean {
        return try {
            val lowestHighScore = withContext(Dispatchers.IO) {
                highScoresCollection
                    .orderBy("score", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.getLong("score")
                    ?.toInt() ?: 0
            }

            val totalScores = withContext(Dispatchers.IO) {
                highScoresCollection
                    .get()
                    .await()
                    .size()
            }

            score > lowestHighScore || totalScores < 10
        } catch (e: Exception) {
            Log.e("Firebase", "Error checking high score", e)
            true // If there's an error, we'll assume it's a high score to be safe
        }
    }
}