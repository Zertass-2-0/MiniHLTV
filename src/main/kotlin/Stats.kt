package org.example

data class MapSummary(val map: String, val timesPlayed: Int, val avgRounds: Double, val overtimes: Int)

class TournamentStats(val teams: List<Team>, val matches: List<Match>) {

    val players: List<Player> = teams.flatMap { it.players }

    fun teamStats(): List<TeamStats> {
        val finished = matches.filter { it.status is MatchStatus.Finished }
        return teams.map { team ->
            val own = finished.filter(involving(team))
            val won = own.count { it.winner == team }
            TeamStats(
                team = team,
                matchesWon = won,
                matchesLost = own.size - won,
                mapsWon = own.sumOf { it.mapsWonBy(team) },
                mapsLost = own.sumOf { it.maps.size - it.mapsWonBy(team) },
                roundsWon = own.sumOf { it.roundsFor(team) },
                roundsLost = own.sumOf { it.roundsAgainst(team) }
            )
        }
    }

    fun standings(): List<TeamStats> = teamStats().sortedWith(
        compareByDescending<TeamStats> { it.matchesWon }
            .thenByDescending { it.mapDiff }
            .thenByDescending { it.roundDiff }
    )

    fun rankTeams(n: Int, selector: (TeamStats) -> Double): List<TeamStats> =
        teamStats().sortedByDescending(selector).take(n)

    fun filterMatches(predicate: (Match) -> Boolean): List<Match> = matches.filter(predicate)

    fun countries(): Set<String> = players.map { it.country }.toSet()

    fun playersByCountry(): Map<String, List<Player>> = players.groupBy { it.country }

    fun mapSummaries(): List<MapSummary> =
        matches.flatMap { it.maps }
            .groupBy { it.map }
            .map { (name, list) ->
                MapSummary(name, list.size, list.map { it.rounds }.average(), list.count { it.isOvertime })
            }
            .sortedByDescending { it.timesPlayed }
}

fun involving(team: Team): (Match) -> Boolean = { it.involves(team) }

fun inRound(round: Int): (Match) -> Boolean = { it.round == round }