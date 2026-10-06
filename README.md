# World Cup Scoreboard

A small in-memory Java library that tracks live football World Cup matches and lists them in a scoreboard summary.

Requires JDK 21 and Maven 3.9+. No runtime dependencies; JUnit 5 and AssertJ are test-only.

```bash
mvn test       # run the tests
mvn package    # build the jar
```

## Usage

```java
Scoreboard scoreboard = new Scoreboard();

MatchId match = scoreboard.startMatch("Mexico", "Canada");             // live, 0-0
scoreboard.updateScore(match, Side.AWAY, ScoreChange.GOAL);           // Mexico 0 - Canada 1
scoreboard.updateScore(match, Side.AWAY, ScoreChange.GOAL_CANCELLED); // VAR: back to 0-0
List<MatchSnapshot> summary = scoreboard.getSummary();                // ordered, read-only
scoreboard.finishMatch(match);                                        // removed from the board
```

| Operation | Behaviour |
|---|---|
| `startMatch(home, away)` | Starts a match at 0-0 and returns its `MatchId`. |
| `updateScore(id, side, change)` | `GOAL` adds one to the `HOME` or `AWAY` score, `GOAL_CANCELLED` removes one. |
| `finishMatch(id)` | Removes the match from the board and frees both teams. |
| `getSummary()` | Live matches by total score, descending; ties go to the most recently started match. |
| `subscribe(listener)` | **Additional operation**, see [below](#additional-operation-subscribe). |

**Errors.** `null` arguments throw `NullPointerException`; blank names or a team playing itself throw
`IllegalArgumentException`. Rule violations throw subclasses of `ScoreboardException`: `TeamAlreadyPlayingException`,
`MatchNotFoundException` (unknown or finished match) and `NoGoalToCancelException`. A rejected call never changes
the board.

## Assumptions

- A match is live as soon as it starts; there is no "scheduled" state.
- The same teams can meet more than once (group stage and knockout), so matches are identified by a `MatchId`.
- A team plays at most one live match at a time and cannot play itself.
- Team names are trimmed and case-insensitive (`" brazil "` = `"Brazil"`); the caller's capitalisation is kept.
- Scores change one goal at a time; goals can be cancelled (VAR) but never below zero.
- Finished matches leave the board; only matches in progress are kept.
- "Multiple simultaneous matches" means many live matches, not many threads.

## Design and reasoning

- **One job per class, one package per role:** `service` (`Scoreboard`, a thin orchestrator), `domain` (`Match`
  with the score rules and summary order; `TeamName`, a case-insensitive value object), `repository`
  (`MatchRepository`, injected through the constructor, with `InMemoryMatchRepository` as the default), `api` (the
  contract callers use), `event` and `api.event` (the additional operation), `exception`.
- **Goal events, not absolute scores.** Live feeds push each goal as it happens; an enum makes inputs like `+7`
  impossible. The task's example scores are reached by one event per goal.
- **Ids double as the start order.** They come from an increasing counter, so tie-breaks never collide and tests
  need no clock.
- **Immutable snapshots out.** `MatchSnapshot` records in an unmodifiable list: callers cannot change the board,
  and a returned summary never changes afterwards.
- **Fail fast and test first.** Invalid calls throw. Every behaviour was specified by a failing test before it was
  implemented.

## Trade-offs

- **Not thread-safe.** The task asks for a simple library. A lock-free version needed reserve-then-rollback logic
  for the two-team check, so it was dropped. If needed, one lock (`synchronized` methods or a decorator) is enough.
- **No absolute score correction.** A score correction has to be sent as goal events.
- **Linear scans.** The team-availability check scans live matches, and the summary is sorted on every read. That is
  fine for a few dozen matches; a team index would need changes only in the repository.
- **Public internals.** Cross-package types must be public, so `Match` and the repository are visible. In return,
  callers can plug in their own `MatchRepository`.
- **In-memory only**, with no history of finished matches.

## Additional operation: `subscribe`

```java
Subscription feed = scoreboard.subscribe(event -> {
    switch (event) {
        case MatchStarted started -> dashboard.add(started.match());
        case ScoreChanged changed -> dashboard.update(changed.match());
        case MatchFinished finished -> dashboard.remove(finished.match().id());
    }
});
feed.cancel();  // stop receiving events
```

After every successful start, score update or finish, the listener receives an event carrying the match as it is
*after* the change. The event type is sealed, so the compiler checks the `switch` handles every case.

- Delivery is synchronous and in subscription order. Only later changes are delivered; call `getSummary()` first
  for the current state. Rejected calls publish nothing.
- `cancel()` takes effect immediately, even mid-delivery, and is safe to repeat.
- A failing listener does not stop the others. Afterwards the operation throws `ListenerFailedException`, but the
  change itself has been applied.
- Trade-off: a slow listener slows the scoreboard down. Listeners that do slow work should hand events to their own
  executor.

**Why this feature.** A live scoreboard exists to be watched. Without subscriptions, every dashboard or betting feed
has to poll `getSummary()` and work out what changed. Pushing events is how live sports data is consumed in
practice, and it plugs in without changing the core operations. Alternatives considered: a team-name lookup, an
absolute score correction, pluggable summary ordering and a goal timeline.
