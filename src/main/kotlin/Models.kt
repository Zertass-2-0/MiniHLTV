package org.example

import java.util.Locale

interface Describable {
    fun describe(): String
}

data class Player(
    val nickname: String,
    val country: String,
    val team: String
) : Describable {
    override fun describe(): String = "$nickname ($country), plays for $team"
}

data class Team(
    val name: String,
    val players: List<Player>,
    val coach: String
) : Describable {
    override fun describe(): String = "$name | coach $coach | players: ${players.joinToString { it.nickname }}"
}

data class Tournament(val name: String, val location: String?) {
    val isOnline: Boolean get() = location == null
    fun where(): String = location ?: "online"
}

data class MapResult(val map: String, val score1: Int, val score2: Int) {
    val rounds: Int get() = score1 + score2
    val isOvertime: Boolean get() = rounds > 24
    fun show(): String = "$map $score1-$score2"
}

data class TeamStats(
    val team: Team,
    val matchesWon: Int,
    val matchesLost: Int,
    val mapsWon: Int,
    val mapsLost: Int,
    val roundsWon: Int,
    val roundsLost: Int
) : Describable {
    val mapDiff: Int get() = mapsWon - mapsLost
    val roundDiff: Int get() = roundsWon - roundsLost
    val roundWinRate: Double
        get() = if (roundsWon + roundsLost == 0) 0.0 else roundsWon * 100.0 / (roundsWon + roundsLost)

    override fun describe(): String =
        "${team.name}: $matchesWon-$matchesLost, maps $mapsWon-$mapsLost, " +
            "rounds $roundsWon-$roundsLost (${roundWinRate.fmt(1)}% rounds won)"
}

sealed class MatchStatus {
    object Upcoming : MatchStatus()
    data class Live(val maps1: Int, val maps2: Int) : MatchStatus()
    data class Finished(val winner: Team, val winnerMaps: Int, val loserMaps: Int) : MatchStatus()
}

fun Double.fmt(digits: Int = 2): String = String.format(Locale.US, "%.${digits}f", this)