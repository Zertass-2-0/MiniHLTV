package org.example

import kotlinx.coroutines.delay

object EslApi {
    const val TEAM_DELAY_MS = 700L
    private const val MATCHES_DELAY_MS = 500L

    private fun team(name: String, coach: String, vararg players: Pair<String, String>) =
        Team(name, players.map { (nick, country) -> Player(nick, country, name) }, coach)

    private val rosters: List<Team> = listOf(
        team("Team Spirit", "hally",
            "magixx" to "Russia", "zont1x" to "Ukraine", "donk" to "Russia", "sh1ro" to "Russia", "tN1R" to "Belarus"),
        team("Team Falcons", "zonic",
            "NiKo" to "Bosnia and Herzegovina", "TeSeS" to "Denmark", "m0NESY" to "Russia", "kyousuke" to "Russia", "karrigan" to "Denmark"),
        team("MOUZ", "sycrone",
            "torzsi" to "Hungary", "xertioN" to "Israel", "Spinx" to "Israel", "xelex" to "Hungary", "PR" to "Czech Republic"),
        team("9z Team", "BIT",
            "max" to "Uruguay", "HUASOPEEK" to "Chile", "luchov" to "Argentina", "meyern" to "Argentina", "dgt" to "Uruguay"),
        team("Team Vitality", "XTQZZZ",
            "apEX" to "France", "ZywOo" to "France", "flameZ" to "Israel", "mezii" to "United Kingdom", "ropz" to "Estonia"),
        team("Natus Vincere", "B1ad3",
            "b1t" to "Ukraine", "Aleksib" to "Finland", "iM" to "Romania", "w0nderful" to "Ukraine", "makazze" to "Kosovo"),
        team("Legacy", "adr",
            "latto" to "Brazil", "dumau" to "Brazil", "n1ssim" to "Brazil", "try" to "Argentina", "bobz" to "Brazil"),
        team("FURIA", "sidde",
            "yuurih" to "Brazil", "KSCERATO" to "Brazil", "FalleN" to "Brazil", "molodoy" to "Kazakhstan", "YEKINDAR" to "Latvia"),
        team("BetBoom Team", "Fierce",
            "s1ren" to "Russia", "zorte" to "Russia", "Magnojez" to "Russia", "Boombl4" to "Russia", "d1Ledez" to "Russia"),
        team("Aurora Gaming", "ashhh",
            "XANTARES" to "Turkey", "Wicadia" to "Turkey", "woxic" to "Turkey", "Jimpphat" to "Finland", "kyxsan" to "North Macedonia"),
        team("G2 Esports", "sAw",
            "huNter-" to "Bosnia and Herzegovina", "HeavyGod" to "Israel", "matys" to "Slovakia", "NertZ" to "Israel", "r1nkle" to "Ukraine"),
        team("PARIVISION", "dastan",
            "Jame" to "Russia", "xiELO" to "Russia", "zweih" to "Russia", "FL1T" to "Russia", "HObbit" to "Kazakhstan"),
        team("1w Team", "groove",
            "nafany" to "Russia", "Ax1Le" to "Russia", "ArtFr0st" to "Russia", "fame" to "Russia", "BELCHONOKK" to "Russia"),
        team("M80", "dephh",
            "Swisher" to "United States", "slaxz-" to "Germany", "s1n" to "Germany", "Lake" to "United States", "JBa" to "United States"),
        team("ShindeN", "ninro",
            "abizz" to "Argentina", "ivz" to "Argentina", "naz" to "Argentina", "tom1jed" to "Argentina", "guty" to "Argentina"),
        team("TYLOO", "zhokiNg",
            "JamYoung" to "China", "Moseyuh" to "China", "Mercury" to "China", "Jee" to "China", "Zero" to "China")
    )

    fun teamNames(): List<String> = rosters.map { it.name }

    suspend fun fetchTeam(name: String): Team {
        delay(TEAM_DELAY_MS)
        return rosters.first { it.name == name }
    }

    suspend fun fetchMatches(teams: List<Team>): List<Match> {
        delay(MATCHES_DELAY_MS)
        val byName = teams.associateBy { it.name }

        // Local helper functions that build matches (all Swiss stage matches are best of 3)
        fun finished(id: Int, round: Int, time: String, a: String, b: String, vararg maps: MapResult): Match {
            val team1 = byName.getValue(a)
            val team2 = byName.getValue(b)
            val wins1 = maps.count { it.score1 > it.score2 }
            val wins2 = maps.size - wins1
            val status = if (wins1 > wins2) MatchStatus.Finished(team1, wins1, wins2)
            else MatchStatus.Finished(team2, wins2, wins1)
            return BestOf3(id, round, time, team1, team2, status, maps.toList())
        }

        fun live(id: Int, round: Int, time: String, a: String, b: String, vararg maps: MapResult): Match {
            val wins1 = maps.count { it.score1 > it.score2 }
            return BestOf3(id, round, time, byName.getValue(a), byName.getValue(b),
                MatchStatus.Live(wins1, maps.size - wins1), maps.toList())
        }

        fun upcoming(id: Int, round: Int, time: String, a: String, b: String): Match =
            BestOf3(id, round, time, byName.getValue(a), byName.getValue(b), MatchStatus.Upcoming, emptyList())

        return listOf(
            finished(1, 1, "Oct 3, 11:00 CEST", "Team Falcons", "TYLOO", MapResult("Mirage", 13, 11), MapResult("Anubis", 13, 7)),
            finished(2, 1, "Oct 3, 11:00 CEST", "Legacy", "PARIVISION", MapResult("Dust II", 13, 5), MapResult("Inferno", 2, 13), MapResult("Ancient", 12, 16)),
            finished(3, 1, "Oct 3, 14:05 CEST", "Team Spirit", "ShindeN", MapResult("Dust II", 11, 13), MapResult("Nuke", 13, 11), MapResult("Mirage", 13, 4)),
            finished(4, 1, "Oct 3, 13:30 CEST", "Team Vitality", "1w Team", MapResult("Dust II", 13, 1), MapResult("Cache", 13, 5)),
            finished(5, 1, "Oct 3, 17:25 CEST", "MOUZ", "M80", MapResult("Cache", 13, 9), MapResult("Mirage", 13, 6)),
            finished(6, 1, "Oct 3, 16:00 CEST", "Aurora Gaming", "9z Team", MapResult("Dust II", 13, 3), MapResult("Mirage", 13, 11)),
            finished(7, 1, "Oct 3, 19:25 CEST", "FURIA", "BetBoom Team", MapResult("Mirage", 13, 5), MapResult("Cache", 16, 14)),
            finished(8, 1, "Oct 3, 18:30 CEST", "G2 Esports", "Natus Vincere", MapResult("Cache", 7, 13), MapResult("Inferno", 4, 13)),
            finished(9, 2, "Oct 4, 11:00 CEST", "Team Spirit", "PARIVISION", MapResult("Ancient", 11, 13), MapResult("Dust II", 13, 6), MapResult("Anubis", 13, 5)),
            finished(10, 2, "Oct 4, 14:20 CEST", "Team Falcons", "Aurora Gaming", MapResult("Mirage", 13, 4), MapResult("Dust II", 13, 10)),
            finished(11, 2, "Oct 4, 16:25 CEST", "MOUZ", "FURIA", MapResult("Cache", 13, 4), MapResult("Mirage", 13, 7)),
            finished(12, 2, "Oct 4, 18:30 CEST", "Team Vitality", "Natus Vincere", MapResult("Anubis", 13, 5), MapResult("Nuke", 5, 13), MapResult("Mirage", 13, 4)),
            finished(13, 2, "Oct 4, 11:00 CEST", "Legacy", "ShindeN", MapResult("Nuke", 13, 8), MapResult("Mirage", 13, 11)),
            finished(14, 2, "Oct 4, 13:30 CEST", "9z Team", "TYLOO", MapResult("Nuke", 13, 10), MapResult("Mirage", 13, 2)),
            finished(15, 2, "Oct 4, 16:00 CEST", "G2 Esports", "1w Team", MapResult("Dust II", 11, 13), MapResult("Ancient", 13, 1), MapResult("Mirage", 8, 13)),
            finished(16, 2, "Oct 4, 19:10 CEST", "BetBoom Team", "M80", MapResult("Cache", 16, 14), MapResult("Anubis", 13, 11)),
            upcoming(17, 3, "Oct 5, 16:00 CEST", "Team Spirit", "MOUZ"),
            upcoming(18, 3, "Oct 5, 18:30 CEST", "Team Vitality", "Team Falcons"),
            live(19, 3, "Oct 5, 11:00 CEST", "PARIVISION", "FURIA", MapResult("Ancient", 13, 7), MapResult("Mirage", 5, 13)),
            upcoming(20, 3, "Oct 5, 13:30 CEST", "9z Team", "Natus Vincere"),
            live(21, 3, "Oct 5, 13:30 CEST", "Legacy", "1w Team"),
            upcoming(22, 3, "Oct 5, 18:30 CEST", "Aurora Gaming", "BetBoom Team"),
            finished(23, 3, "Oct 5, 11:00 CEST", "ShindeN", "G2 Esports", MapResult("Dust II", 6, 13), MapResult("Ancient", 9, 13)),
            upcoming(24, 3, "Oct 5, 16:00 CEST", "M80", "TYLOO")
        )
    }
}
