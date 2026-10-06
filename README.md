# MiniHLTV

A small interactive Kotlin console application that imitates an esports statistics site (like HLTV).
It uses **real data**: the 16 teams (rosters, coaches, player countries) and the Swiss stage results of
**ESL Pro League Season 24** (October 2026), as of 5 October 2026, copied from
[Liquipedia](https://liquipedia.net/counterstrike/ESL/Pro_League/Season_24).
Map scores include overtime rounds. Per-player statistics are not included (no free source).

The matches PARIVISION vs FURIA and Legacy vs 1w Team are treated as live: the app plays them to the end with
**random results** and delays. You can also simulate your own match between any two teams (BO1/BO3/BO5).
Simulated results are never stored, so the standings always show only the real results.

After start the program "loads" the data from a fake API (with delays) and shows a menu. Type a command number
and press Enter. After every command press Enter to return to the menu. `m` shows the menu, `q` quits.

## How to run

Requirements: JDK 17+ (the project uses a Java 21 toolchain). Gradle is downloaded automatically by the wrapper.

```
./gradlew run          # Linux / macOS / Git Bash
gradlew.bat run        # Windows
```

Or open the project in IntelliJ IDEA and press the green run button next to `main()` in `Main.kt`.
The Kotlin Coroutines dependency (`kotlinx-coroutines-core`) is declared in `build.gradle.kts`.

## Where the requirements are demonstrated

| Requirement | Where |
|---|---|
| Variables, data types, conditions, loops | `Cli.kt` (`val`/`var`, `String`/`Boolean`/`Int`, `if`, `when`, `for`, `while` in the menu loop and input helpers) |
| List, Set, Map | `Stats.kt`: `TournamentStats.countries()` is a Set, `playersByCountry()` is a Map; `Cli.kt`: the menu is a `List<MenuItem>`, `groupBy` by round |
| map, filter, reduce | `Stats.kt` (`map`, `filter`, `groupBy`, `sumOf`, `sortedWith`), `Cli.kt` (`reduce` for total rounds, `filter` in search) |
| Functions, higher-order functions, lambdas | `Stats.kt`: `rankTeams(n) { ... }` and `filterMatches { ... }` take lambdas, `involving(team)` and `inRound(n)` return functions; `Cli.kt`: every menu action is a lambda, the ranking metric is chosen by the user and passed as a lambda |
| Classes and objects | `ConsoleApp`, `TournamentStats`, `Player`, `Team`, `Match`; singletons `object EslApi` and `object MatchSimulator` |
| Inheritance | `Match.kt`: abstract `Match` and subclasses `BestOf1`, `BestOf3`, `BestOf5` (each overrides `format` and `mapsToWin`) |
| Interfaces and polymorphism | `Models.kt`: `Describable`, implemented by `Player`, `Team`, `TeamStats` and `Match`; the "Search" command puts all of them into one `List<Describable>`; `Match` subclasses are used through the `Match` type in the simulator |
| Data class | `Models.kt`: `Player`, `Team`, `MapResult`, `TeamStats`; `MenuItem` in `Cli.kt`, `MapSummary` in `Stats.kt` |
| Sealed class | `Models.kt`: `MatchStatus` (`Upcoming`, `Live`, `Finished`), handled with an exhaustive `when` in `Match.describe()` |
| Suspend function and coroutine | `EslApi.kt`: `suspend fun fetchTeam()` / `fetchMatches()` (use `delay`); `Simulation.kt`: `suspend fun play()`; `Cli.kt`: `loadData()` uses `async` + `awaitAll` (teams load in parallel) and `launch` (background "loading" ticker, stopped with `cancelAndJoin()`), `watchLive()` plays two matches at the same time with `async`; `main()` uses `runBlocking` |

## Project structure

```
src/main/kotlin/
  Main.kt        entry point: main() starts the console app
  Cli.kt         interactive menu and all commands
  Models.kt      data classes, sealed class, interface
  Match.kt       abstract Match + BestOf1/3/5
  Stats.kt       TournamentStats, higher-order functions
  Simulation.kt  MatchSimulator (suspend function, random results)
  EslApi.kt      fake API with the real tournament data (suspend functions)
```
