package org.example

abstract class Match(
    val id: Int,
    val round: Int,
    val startTime: String,
    val team1: Team,
    val team2: Team,
    val status: MatchStatus,
    val maps: List<MapResult>
) : Describable {

    abstract val format: String
    abstract val mapsToWin: Int

    val title: String get() = "${team1.name} vs ${team2.name}"
    val winner: Team? get() = (status as? MatchStatus.Finished)?.winner

    fun involves(team: Team): Boolean = team == team1 || team == team2

    fun roundsFor(team: Team): Int = maps.sumOf { if (team == team1) it.score1 else it.score2 }
    fun roundsAgainst(team: Team): Int = maps.sumOf { if (team == team1) it.score2 else it.score1 }
    fun mapsWonBy(team: Team): Int = maps.count { (it.score1 > it.score2) == (team == team1) }

    override fun describe(): String {
        val info = when (status) {
            is MatchStatus.Upcoming -> "not started yet"
            is MatchStatus.Live -> "LIVE ${status.maps1}:${status.maps2} (first to $mapsToWin maps)"
            is MatchStatus.Finished -> "${status.winner.name} won ${status.winnerMaps}:${status.loserMaps}"
        }
        return "[#$id] $startTime $format $title -> $info"
    }
}

class BestOf1(
    id: Int, round: Int, startTime: String, team1: Team, team2: Team, status: MatchStatus, maps: List<MapResult>
) : Match(id, round, startTime, team1, team2, status, maps) {
    override val format = "BO1"
    override val mapsToWin = 1
}

class BestOf3(
    id: Int, round: Int, startTime: String, team1: Team, team2: Team, status: MatchStatus, maps: List<MapResult>
) : Match(id, round, startTime, team1, team2, status, maps) {
    override val format = "BO3"
    override val mapsToWin = 2
}

class BestOf5(
    id: Int, round: Int, startTime: String, team1: Team, team2: Team, status: MatchStatus, maps: List<MapResult>
) : Match(id, round, startTime, team1, team2, status, maps) {
    override val format = "BO5"
    override val mapsToWin = 3
}
