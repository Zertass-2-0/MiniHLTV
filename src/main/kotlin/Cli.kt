package org.example

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


data class MenuItem(val title: String, val action: suspend () -> Unit)

class ConsoleApp {
    private val tournament = Tournament("ESL Pro League Season 24 - Swiss stage", location = "Katowice, Poland")
    private var commandsUsed: Int = 0
    private var cancelled: Boolean = false   // set when the user cancels a command with an empty Enter

    private var teams: List<Team> = emptyList()
    private var matches: List<Match> = emptyList()

    private val stats: TournamentStats get() = TournamentStats(teams, matches)

    private val menu: List<MenuItem> = listOf(
        MenuItem("Swiss standings") { showStandings() },
        MenuItem("Matches (all or one round)") { showMatches() },
        MenuItem("Match details (map scores)") { showMatchDetails() },
        MenuItem("Team profile (roster, record, matches)") { showTeamProfile() },
        MenuItem("All rosters") { showRosters() },
        MenuItem("Team ranking by a chosen metric") { showTeamRanking() },
        MenuItem("Map statistics") { showMapStats() },
        MenuItem("Countries of the players") { showCountries() },
        MenuItem("Search players, teams and matches") { search() },
        MenuItem("Watch the live matches (random results, coroutines)") { watchLive() },
        MenuItem("Simulate a match: pick two teams and BO1/BO3/BO5") { simulateMatch() },
        MenuItem("Reload data from the API (coroutines)") { loadData() }
    )

    suspend fun run() {
        println("=== ${tournament.name} (${tournament.where()}) ===")
        println("Data: Liquipedia, state of 5 October 2026")
        loadData()
        printMenu()

        while (true) {
            val input = prompt("\n> ", indent = "").lowercase()
            when {
                input == "q" || input == "quit" || input == "exit" -> break
                input.isEmpty() -> continue
                input == "m" || input == "menu" || input == "help" -> printMenu()
                else -> {
                    val item = input.toIntOrNull()?.let { menu.getOrNull(it - 1) }
                    if (item == null) {
                        println("Unknown command '$input'. Type 'm' to see the menu.")
                    } else {
                        println()
                        cancelled = false
                        item.action()
                        commandsUsed++
                        if (!cancelled) prompt("Press Enter to return to the menu...", indent = "\n")
                        printMenu()
                    }
                }
            }
        }
        println("Bye! Commands used: $commandsUsed")
    }

    private fun printMenu() {
        println("\nCommands:")
        menu.forEachIndexed { i, item -> println("  ${(i + 1).toString().padStart(2)}. ${item.title}") }
        println("   m. Show this menu")
        println("   q. Quit")
    }

    //Input helpers

    private fun prompt(question: String, indent: String = "  "): String {
        print(indent + question)
        System.out.flush()
        return readlnOrNull()?.trim() ?: "q" // end of input -> behave like "quit"
    }

    private fun askInt(question: String, default: Int, min: Int = 1): Int {
        while (true) {
            val text = prompt("$question [$default]: ")
            if (text.isEmpty() || text == "q") return default
            val n = text.toIntOrNull()
            if (n != null && n >= min) return n
            println("  Please enter a whole number, at least $min.")
        }
    }

    private fun printTeamList() {
        teams.mapIndexed { i, t -> "${(i + 1).toString().padStart(2)}. ${t.name.padEnd(14)}" }
            .chunked(4)
            .forEach { row -> println("  " + row.joinToString(" ")) }
    }

    private fun askTeam(question: String): Team? {
        while (true) {
            val text = prompt("$question (number or name, Enter to cancel): ")
            if (text.isEmpty() || text == "q") {
                cancelled = true
                return null
            }
            val found = text.toIntOrNull()?.let { teams.getOrNull(it - 1) }
                ?: teams.firstOrNull { it.name.equals(text, ignoreCase = true) }
                ?: teams.filter { it.name.contains(text, ignoreCase = true) }.singleOrNull()
            if (found != null) return found
            println("  No single team matches '$text'.")
        }
    }

    //Commands

    private suspend fun loadData() = coroutineScope {
        println("Loading data from the API...")
        val ticker = launch {
            while (true) {
                delay(250)
                println("  ... still loading")
            }
        }
        val start = System.currentTimeMillis()
        val loadedTeams = EslApi.teamNames()
            .map { teamName -> async { EslApi.fetchTeam(teamName) } }
            .awaitAll()
        val loadedMatches = EslApi.fetchMatches(loadedTeams)
        ticker.cancelAndJoin()

        teams = loadedTeams
        matches = loadedMatches
        val time = System.currentTimeMillis() - start
        println("Loaded ${teams.size} teams and ${matches.size} matches in $time ms ")
    }

    private fun showStandings() {
        println("Swiss standings (finished matches only; tiebreak: map difference, then round difference)")
        println("   #  ${"Team".padEnd(14)} ${"W-L".padEnd(5)} ${"Maps".padEnd(6)} ${"Rounds".padEnd(8)} Rounds won")
        stats.standings().forEachIndexed { i, s ->
            val record = "${s.matchesWon}-${s.matchesLost}"
            val maps = "${s.mapsWon}-${s.mapsLost}"
            val rounds = "${s.roundsWon}-${s.roundsLost}"
            println("  ${(i + 1).toString().padStart(2)}. ${s.team.name.padEnd(14)} ${record.padEnd(5)} " +
                "${maps.padEnd(6)} ${rounds.padEnd(8)} ${s.roundWinRate.fmt(1)}%")
        }
    }

    private fun showMatches() {
        val round = askInt("Which round (1-3)? 0 = all", 0, min = 0)
        val list = if (round == 0) matches else stats.filterMatches(inRound(round))
        if (list.isEmpty()) {
            println("No matches in round $round.")
            return
        }
        for ((r, group) in list.groupBy { it.round }) {
            println("Round $r")
            group.forEach { println("  " + it.describe()) }
        }
        val finished = list.count { it.status is MatchStatus.Finished }
        val live = list.count { it.status is MatchStatus.Live }
        println("\n${list.size} matches: $finished finished, $live live, ${list.size - finished - live} upcoming")
    }

    private fun showMatchDetails() {
        val id = askInt("Match number (1-${matches.size})?", 1)
        val match = matches.find { it.id == id } ?: run {
            println("No match #$id.")
            return
        }
        println(match.describe())
        if (match.maps.isEmpty()) println("  No maps played yet.")
        match.maps.forEachIndexed { i, m ->
            val winner = if (m.score1 > m.score2) match.team1.name else match.team2.name
            println("  Map ${i + 1}: ${m.show()} -> $winner${if (m.isOvertime) " (overtime)" else ""}")
        }
    }

    private fun showTeamProfile() {
        printTeamList()
        val team = askTeam("Team") ?: return
        val teamStats = stats.teamStats().first { it.team == team }
        println("\n${team.name} (coach: ${team.coach})")
        println("  Roster: " + team.players.joinToString { "${it.nickname} (${it.country})" })
        println("  Record: ${teamStats.describe()}")
        println("  Matches:")
        stats.filterMatches(involving(team)).forEach { println("    " + it.describe()) }
    }

    private fun showRosters() {
        for (team in teams) {
            println("${team.name} (coach: ${team.coach})")
            println("    " + team.players.joinToString { "${it.nickname} (${it.country})" })
        }
    }

    private fun showTeamRanking() {
        // A (label, lambda) pair: the metric is chosen by the user and passed on as a lambda
        val (label, metric) = when (askInt("Metric: 1 = round win %, 2 = map difference, 3 = round difference", 1)) {
            2 -> "map difference" to { s: TeamStats -> s.mapDiff.toDouble() }
            3 -> "round difference" to { s: TeamStats -> s.roundDiff.toDouble() }
            else -> "round win %" to { s: TeamStats -> s.roundWinRate }
        }
        val n = askInt("How many teams?", 5)
        println("Top $n teams by $label:")
        stats.rankTeams(n, metric).forEachIndexed { i, s ->
            println("  ${i + 1}. ${s.team.name.padEnd(14)} ${metric(s).fmt(1).padStart(6)}   (${s.describe()})")
        }
    }

    private fun showMapStats() {
        val allMaps = matches.flatMap { it.maps }
        if (allMaps.isEmpty()) {
            println("No maps played yet.")
            return
        }
        val totalRounds = allMaps.map { it.rounds }.reduce { acc, r -> acc + r }   // reduce: sum of rounds
        println("${allMaps.size} maps played, $totalRounds rounds in total")
        println("  ${"Map".padEnd(8)} ${"Played".padEnd(7)} ${"Avg rounds".padEnd(11)} Overtimes")
        for (s in stats.mapSummaries()) {
            println("  ${s.map.padEnd(8)} ${s.timesPlayed.toString().padEnd(7)} ${s.avgRounds.fmt(1).padEnd(11)} ${s.overtimes}")
        }
    }

    private fun showCountries() {
        val countries = stats.countries()                      // Set: unique values
        println("${stats.players.size} players from ${countries.size} countries:")
        for ((country, list) in stats.playersByCountry().entries.sortedByDescending { it.value.size }) {
            println("  ${country.padEnd(22)} ${list.size.toString().padStart(2)}  ${list.joinToString { it.nickname }}")
        }
    }

    /** Polymorphism: players, teams and matches are all Describable, so one list and one loop handle them. */
    private fun search() {
        val query = prompt("Search for: ")
        if (query.isEmpty()) {
            cancelled = true
            return
        }
        val everything: List<Describable> = buildList {
            addAll(teams)
            addAll(stats.players)
            addAll(matches)
        }
        val found = everything.filter { it.describe().contains(query, ignoreCase = true) }
        println("${found.size} result(s) for '$query'" + if (found.size > 15) " (showing 15):" else ":")
        found.take(15).forEach { item ->
            val kind = when (item) {
                is Team -> "TEAM  "
                is Player -> "PLAYER"
                is Match -> "MATCH "
                else -> "OTHER "
            }
            println("  $kind ${item.describe()}")
        }
    }

    /** Two live matches are played at the same time: one coroutine (async) per match. */
    private suspend fun watchLive() = coroutineScope {
        val live = matches.filter { it.status is MatchStatus.Live }
        if (live.isEmpty()) {
            println("No live matches right now.")
            return@coroutineScope
        }
        println("Watching ${live.size} live match(es). The rest of each match is played with random results.")
        for (match in live) {
            val status = match.status as MatchStatus.Live
            println("  ${match.title}: ${status.maps1}:${status.maps2} after ${match.maps.size} map(s)")
        }
        println()

        val finishedMaps = live.map { match ->
            async {
                MatchSimulator.play(match) { result, maps1, maps2 ->
                    println("  [${match.team1.name} vs ${match.team2.name}] ${result.show()} -> maps $maps1:$maps2")
                }
            }
        }.awaitAll()

        println()
        live.zip(finishedMaps).forEach { (match, maps) ->
            val wins1 = maps.count { it.score1 > it.score2 }
            val winner = if (wins1 >= match.mapsToWin) match.team1 else match.team2
            println("Final: ${winner.name} wins ${match.title} ${maxOf(wins1, maps.size - wins1)}:${minOf(wins1, maps.size - wins1)}")
        }
        println("(simulation only, the stored results and standings are unchanged)")
    }

    private suspend fun simulateMatch() {
        printTeamList()
        val a = askTeam("First team") ?: return
        val b = askTeam("Second team") ?: return
        if (a == b) {
            println("Pick two different teams.")
            return
        }
        // Polymorphism: BestOf1/3/5 are used through the same Match type
        val match: Match = when (askInt("Format: 1, 3 or 5 maps (anything else = 3)", 3)) {
            1 -> BestOf1(0, 0, "now", a, b, MatchStatus.Live(0, 0), emptyList())
            5 -> BestOf5(0, 0, "now", a, b, MatchStatus.Live(0, 0), emptyList())
            else -> BestOf3(0, 0, "now", a, b, MatchStatus.Live(0, 0), emptyList())
        }
        println("\nSimulating ${match.format} ${match.title}...")
        val maps = MatchSimulator.play(match) { result, maps1, maps2 ->
            println("  ${result.show()} -> maps $maps1:$maps2")
        }
        val wins1 = maps.count { it.score1 > it.score2 }
        val winner = if (wins1 >= match.mapsToWin) a else b
        println("Winner: ${winner.name} (${maxOf(wins1, maps.size - wins1)}:${minOf(wins1, maps.size - wins1)})")
    }
}
