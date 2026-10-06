# World Cup Scoreboard

A small in-memory Java library that tracks live football World Cup matches and lists them in a scoreboard summary.

## Requirements

- JDK 21
- Maven 3.9+

```bash
mvn test       # run the test suite
mvn package    # build the jar
```

The library has no runtime dependencies. JUnit 5 and AssertJ are used for tests only.

## Usage

```java
Scoreboard scoreboard = new Scoreboard();

MatchId match = scoreboard.startMatch("Mexico", "Canada");             // live, 0-0
scoreboard.updateScore(match, Side.AWAY, ScoreChange.GOAL);           // Mexico 0 - Canada 1
scoreboard.updateScore(match, Side.AWAY, ScoreChange.GOAL_CANCELLED); // VAR: back to 0-0

List<MatchSnapshot> summary = scoreboard.getSummary();                // ordered, read-only

scoreboard.finishMatch(match);                                        // removed from the board
```

## API

| Operation | Behaviour |
|---|---|
| `MatchId startMatch(String homeTeam, String awayTeam)` | Starts a match at 0-0 and returns its id. The match is live immediately. |
| `void updateScore(MatchId id, Side side, ScoreChange change)` | Applies one score event to the `HOME` or `AWAY` side: `GOAL` adds one, `GOAL_CANCELLED` removes one. |
| `void finishMatch(MatchId id)` | Removes the match from the board and frees both teams. |
| `List<MatchSnapshot> getSummary()` | Returns the live matches ordered by total score (descending); ties go to the most recently started match. |

Packages, one per responsibility, under `io.github.maksimlk.worldcup.scoreboard`:

| Package | Contents | Role |
|---|---|---|
| `service` | `Scoreboard` | Entry point and business rules |
| `domain` | `Match`, `TeamName` | The logic behind the contract: one match's mutable state, score rules and summary order; validated team names. Not returned by `Scoreboard`'s operations, but visible to anyone implementing a custom `MatchRepository` |
| `repository` | `MatchRepository`, `InMemoryMatchRepository` | Storage abstraction and its in-memory implementation |
| `api` | `MatchId`, `MatchSnapshot`, `Side`, `ScoreChange` | The contract: immutable values callers pass in and get back |
| `exception` | `ScoreboardException` and its subclasses | Domain errors |

### Errors

| Situation | Exception |
|---|---|
| A `null` argument | `NullPointerException` |
| A blank team name, or the same team on both sides | `IllegalArgumentException` |
| A team is already playing in a live match | `TeamAlreadyPlayingException` |
| An unknown or already finished match id | `MatchNotFoundException` |
| Cancelling a goal when that side's score is 0 | `NoGoalToCancelException` |

The three domain exceptions extend `ScoreboardException`, so callers can catch every rule violation in one place.
Messages name the team or match involved. A rejected call never changes the board.

## Assumptions

1. **A match is live as soon as it starts.** There is no separate "scheduled" state, and the summary shows every started match that has not finished.
2. **The same two teams can play more than once**, for example in the group stage and again in a knockout round. A match is therefore identified by a `MatchId`, not by its team pair.
3. **A team plays at most one live match at a time**, on either side. A team cannot play itself.
4. **Team names are case-insensitive and trimmed.** `" Brazil "`, `"brazil"` and `"BRAZIL"` are the same team. The name is stored trimmed, with the caller's capitalisation.
5. **Scores change one goal at a time.** Goals can be cancelled (for example by VAR), but a score can never go below zero.
6. **A finished match leaves the board.** The task only asks for matches in progress, so finished matches are not kept.
7. **"Multiple simultaneous matches" means many matches live at the same time, not many threads.** See [Trade-offs](#trade-offs).

## Design and reasoning

- **SOLID, with one job per class and one package per responsibility.**
  - `Scoreboard` (`service`) orchestrates the four operations and enforces the one-live-match-per-team rule. Each
    public method is a few lines that delegate the rest.
  - `MatchRepository` (`repository`) is the storage abstraction: it saves, finds and removes matches and issues ids,
    like a database sequence. `Scoreboard` depends on this interface, not on a concrete map (dependency inversion),
    and receives it through its constructor. `new Scoreboard()` uses `InMemoryMatchRepository`.
  - `Match` (`domain`) holds one match's score, enforces "never below zero" and defines the summary order.
  - `TeamName` (`domain`) is a value object: it validates and trims a name once, when it is created, and its
    `equals`/`hashCode` ignore case, so there is one notion of "same team" everywhere, including in collections.
- **Event-based score updates.** The library is meant for real-time monitoring, where each goal is pushed as it happens. `updateScore(id, side, GOAL | GOAL_CANCELLED)` matches that flow. An enum, rather than a signed number, makes meaningless inputs like `+7` or `0` impossible. The
  task does not require absolute scores: the example's final scores (such as Mexico 0 - Canada 5) are reached by
  sending one event per goal.
- **A `MatchId` handle.** Every operation after the start uses the id, so a rematch between the same teams can never be confused with an earlier match.
- **One counter for the id and the start order.** Ids come from an increasing counter, and the summary breaks ties by that same counter, newest first. A clock could give two matches the same millisecond; a counter never ties, and tests need no fake clock.
- **Snapshots in the summary.** `getSummary()` returns a new unmodifiable list of immutable `MatchSnapshot` records. Callers cannot change the board through it, and later changes do not affect a list already returned.
- **Fail fast.** Invalid calls throw instead of being silently ignored, so feed errors surface early. Standard Java
  exceptions cover argument errors; custom exceptions cover domain rules.
- **Test-driven.** The behaviour is specified by tests written before the implementation, one test class per operation.

## Trade-offs

- **Not thread-safe.** The task asks for a *simple* library and nothing in it requires concurrent access, so `Scoreboard` has no synchronisation. A thread-safe version built on lock-free maps was considered and dropped. Starting a match has to reserve two teams atomically, which needs reserve-then-rollback logic, and race tests are non-deterministic. If concurrent access is needed, the simplest correct option is a single lock: make the four public methods `synchronized`, or wrap the scoreboard in a synchronized decorator. One lock keeps the two-team check atomic without any rollback code.
- **No absolute score updates.** A feed that sends full scores (for example a correction straight to 3-1) has to send the matching sequence of goal events. This keeps the API small and every change explicit.
- **Team availability is checked by scanning.** `startMatch` asks the repository for a live match involving each
  team, and the in-memory repository scans the live matches. That is fine for a few dozen live matches and needs no
  second structure to keep in sync; a repository with a team index would make it constant-time without any change
  to `Scoreboard`.
- **Summary sorting on every read.** `getSummary()` sorts the live matches each time it is called. With a few dozen live matches at most, this is cheaper and simpler than keeping a sorted structure up to date on every goal.
- **In-memory only.** There is no persistence and no history of finished matches.
- **Internals are public.** With one package per responsibility, `Match` and the repository types must be public to
  be used across packages, so they are visible to callers. Keeping them package-private in one package would hide
  them, but would mix roles in one folder. The benefit is that callers can plug in their own `MatchRepository`.
- **`MatchId` can be created by callers.** It is a public record so it can live in the API package. A hand-made id
  is simply unknown to the board and is rejected with `MatchNotFoundException`.

## Additional operation

The task asks for exactly one additional operation, introduced in its own commit. It is developed on a separate
branch, so this branch contains only the four core operations; the feature and the reasons for choosing it are
documented here when that branch is merged.
