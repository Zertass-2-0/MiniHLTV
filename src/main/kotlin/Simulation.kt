package org.example

import kotlinx.coroutines.delay
import kotlin.random.Random


object MatchSimulator {
    private val mapPool = listOf("Ancient", "Anubis", "Cache", "Dust II", "Inferno", "Mirage", "Nuke")

    suspend fun play(
        match: Match,
        onMap: (result: MapResult, maps1: Int, maps2: Int) -> Unit
    ): List<MapResult> {
        val played = match.maps.toMutableList()
        val usedMaps = played.map { it.map }.toMutableSet()
        var maps1 = played.count { it.score1 > it.score2 }
        var maps2 = played.size - maps1

        while (maps1 < match.mapsToWin && maps2 < match.mapsToWin) {
            delay(Random.nextLong(800, 1600))
            val mapName = mapPool.filter { it !in usedMaps }.random()
            usedMaps += mapName
            val result = randomMap(mapName)
            played += result
            if (result.score1 > result.score2) maps1++ else maps2++
            onMap(result, maps1, maps2)
        }
        return played
    }

    private fun randomMap(mapName: String): MapResult {
        val team1Wins = Random.nextBoolean()
        val overtime = Random.nextInt(100) < 15
        val winnerScore = if (overtime) 16 else 13
        val loserScore = if (overtime) Random.nextInt(12, 15) else Random.nextInt(1, 12)
        return if (team1Wins) MapResult(mapName, winnerScore, loserScore)
        else MapResult(mapName, loserScore, winnerScore)
    }
}