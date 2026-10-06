# AI usage

## Summary

The library was built in pair-programming style with an AI coding agent. I drove the design through questions and
decisions; the agent proposed options, wrote tests and code, and ran the build. Every design decision below was
made or approved by me, often after pushing back on the agent's first proposal.

How the work was split:

- **Requirements analysis:** the agent read the task PDF and listed the open questions (match identity, score update
  style, team validation, thread safety). I answered each one.
- **Design:** discussed in the conversation before any code was written. See the [decision log](#decision-log).
- **TDD:** the test suite was written first, from the agreed API, and confirmed failing against stubs before any
  production logic existed.
- **Review:** I asked the agent to critique its own work against clean code and TDD principles, and against the task
  itself. This led to concrete fixes: split tests, a bug fixed in a test, and thread safety dropped.

## Tools and context

| Item | Detail |
|---|---|
| Agent | Claude Code (CLI), model Claude Opus 5.5 |
| IDE | IntelliJ IDEA 2026.2 |
| Plugins | `superpowers` (its `test-driven-development` skill guided the red-green-refactor workflow), `caveman` (short, plain replies in the chat) |
| Input artifact | Task specification PDF from Sportradar (not committed to the repository) |

## Decision log

The last column lists options the AI agent proposed during the design discussion that I decided against.

| # | Decision | Reasoning | Alternatives suggested by AI (rejected) |
|---|---|---|---|
| 1 | Java 21, Maven, JUnit 5 + AssertJ, no other dependencies | Current LTS; test-only dependencies keep the library footprint at zero | Java 17 |
| 2 | Matches are identified by a `MatchId` returned from `startMatch` | The same two teams can meet more than once in a tournament (group stage and knockout) | Identifying a match by its team pair |
| 3 | Score changes are events: `updateScore(id, side, GOAL \| GOAL_CANCELLED)` | Fits live monitoring: each goal is pushed as it happens. Cancellation covers VAR. An enum prevents meaningless inputs such as `+7` | Absolute `updateScore(home, away)`; separate `goalScored`/`goalCancelled` methods; signed integer delta |
| 4 | A score can never go below zero | Cancelling a goal that never happened is a caller error | — |
| 5 | Domain errors use custom exceptions (`MatchNotFoundException`, `TeamAlreadyPlayingException`, `NoGoalToCancelException`) under one `ScoreboardException` base; argument errors use standard Java exceptions | Callers can catch all rule violations in one place; consistent rule for when a custom type is used | `IllegalStateException` for cancelling a goal at zero (first version) |
| 6 | Team names are trimmed and compared ignoring case; a team cannot play itself or be in two live matches at once | Prevents duplicate or impossible live matches caused by input noise | — |
| 7 | One counter provides both `MatchId` and the start order used for tie-breaks | Fewer moving parts. A clock can tie within the same millisecond; a counter never ties | Separate schedule and start operations, making scheduling the one extra feature |
| 8 | A finished match is removed from the board | The spec only asks for matches in progress | Keeping it with a `FINISHED` status |
| 9 | One package per responsibility under `io.github.maksimlk.worldcup.scoreboard`: `service` (`Scoreboard`), `domain` (`Match`, `TeamName`), `repository`, `api` (the contract: `MatchId`, `MatchSnapshot`, `Side`, `ScoreChange`), `exception` | Package name matches the project; the folder structure mirrors the SOLID split. `api` holds what callers use, `domain` how the rules work, so each class's role is visible from its location | A single flat package (favouring package-private encapsulation); keeping the internals package-private in the root package; naming the contract package `model`, which blurred it with `domain` |
| 10 | One test class per operation, plus a shared test fixture | Smaller files and focused test classes | One large test class with `@Nested` groups |
| 11 | Tests are documented at class level (rules covered), with comments only where the intent is not obvious | Test names already describe behaviour; per-test Javadoc would duplicate them and drift | `@DisplayName` on every test |
| 12 | **Not thread-safe** (for now) | The task asks for a *simple* library; "multiple simultaneous matches" means many live matches, not many threads. My first choice, lock-free concurrent maps, forced reserve-then-rollback logic, and race tests are non-deterministic | `synchronized` methods or a decorator. Kept in the README as the upgrade path |
| 13 | SOLID split: `Scoreboard` (service) only orchestrates the four core operations; it depends on a `MatchRepository` interface (storage and id sequence) injected through a public constructor, with `InMemoryMatchRepository` as the default; `Match` (domain) holds the score rules and summary order; `TeamName` (domain) validates and compares names | Single responsibility per class, dependency inversion for storage, and a small `Scoreboard`. Since the types live in separate packages they are public, so callers can also plug in their own repository | All logic and the match map inside `Scoreboard` (an intermediate "minimal code" version); team names as plain strings validated by a helper in `Scoreboard`; a JPMS `module-info.java` (added, then removed: it hid nothing) |
| 14 | Additional operation: `subscribe(listener)` publishing a sealed `ScoreboardEvent` (`MatchStarted`, `ScoreChanged`, `MatchFinished`), synchronously, in subscription order, only for changes after subscribing; a failing listener does not stop the others and is reported afterwards as `ListenerFailedException`; cancellation via the returned `Subscription` | Live data is consumed by push, not polling; a sealed event type keeps listeners lambda-friendly and lets the compiler check exhaustive handling | Other features: `findLiveMatch`, `correctScore`, pluggable summary ordering, a goal timeline, finished-match history. A listener interface with one method per event; ignoring listener failures or letting the first one propagate; replaying the current state to new subscribers |

## Brainstorming the additional operation

The additional operation was chosen in a structured brainstorm with the agent, on its own branch after the core
operations were merged.

**Candidates considered**, grouped by what each would demonstrate:

| Theme | Candidates |
|---|---|
| Practical API | `findLiveMatch(team)` (look up a live match by team name), `correctScore(id, home, away)` (absolute correction), `getTopMatches(n)`, `formatSummary()` |
| Live-football domain | `subscribe(listener)` (push updates), `getMatchEvents(id)` (goal timeline), `pauseMatch(id)` (half-time), `abandonMatch(id)`, penalty shootouts, red cards |
| Design (SOLID) | `getSummary(SummaryOrder)` (pluggable ordering, open/closed principle), `startMatches(fixtures)` (all-or-nothing batch) |
| History and operations | `getFinishedMatches()`, `undoLastEvent(id)`, `exportState()`/`restore()` |

**Shortlist.** `getSummary(SummaryOrder)` as the best fit for the SOLID story, `findLiveMatch` as the most practical
for an id-based API, and `subscribe` as the strongest fit for a live sports-data company.

**Choice: `subscribe`.** Live data is consumed by push, not polling, so it adds the most value for real consumers,
and it plugs into the design without changing the core operations.

**Design questions, each decided before any code was written:**

| Question | Options | Decision |
|---|---|---|
| Shape of the events | A sealed `ScoreboardEvent` with records, consumed by a `Consumer`; or a listener interface with one method per event | Sealed events: lambda-friendly, and the compiler checks that a `switch` handles every type |
| A listener throws | Isolate and report afterwards; isolate and ignore; let the first failure propagate | Isolate and report: every listener gets the event, then `ListenerFailedException` with the rest suppressed |
| A new subscriber and live matches | Only future events; or replay the current state as events | Only future events; `getSummary()` gives the current state |

Smaller rules agreed with the design: synchronous delivery in subscription order, rejected calls publish nothing,
`cancel()` is safe to repeat and to call during delivery, and subscribing the same listener twice creates two
subscriptions. The full design was presented in the chat and approved before the failing tests were written.

## Prompt history

The instructions that shaped the work, with a short note on what came out of each.

1. > You are a principal software engineer. Our current task is under /docs/odds-and-data-coding-task-sportradar.pdf.
   > We need to implement core operations for live world cup scoreboard. Let's follow TDD and start with creating tests
   > first to later come up with the best design possible. Ask me any questions and discuss your design decisions with
   > me.

   The agent read the PDF and the empty repo, proposed a first API, and asked 6 questions: match identity, update
   style, decreasing scores, team rules, thread safety, Maven wrapper.

2. > Answer to questions: 1. by matchId, it can happen that same teams play the match again 2. let's do
   > goalScored, because we are doing real-time monitoring of the match and want to update information on the match as
   > soon as possible 3. yes, your decision is correct 4. correct 5. let's do threadsafe, but we should use thread-safe data structures instead to
   > make it simpler 6. I already configured the project and maven

   The agent completed `pom.xml` and refined the design. It flagged a conflict: `goalScored` alone cannot lower a
   score.

3. > 3. it should accept an enum and based on the operation selected, either increase or decrease the score.

   The agent agreed one method is fine and proposed an enum (`GOAL`/`GOAL_CANCELLED`). It explored a
   schedule-then-start lifecycle and warned it could count as the one extra operation.

4. > In this case, let's not separate matchId and starting time since it would make implementation more complex

   One counter for id and start order (decision 7). The agent wrote the full test suite plus compile-only stubs, and
   confirmed every test failed.

5. > Don't store all project files under one package. Create packages according to the purpose of the file and store them there.
   > Also, don't cram all the tests in a single file. Split them according to purpose.

   This led to the `model`/`exception` subpackages, a package name matching the project, and one test class per
   operation (decisions 9 and 10).

6. > Your code does not follow clean code and SOLID principles. There should be a separate repository that would
   > contain liveMatches. Make sure you follow SOLID

   The agent extracted `MatchRepository` and `InMemoryMatchRepository`, injected into `Scoreboard` through its
   constructor, keeping the public API and all tests unchanged (decision 13).

7. > Please put each class into a corresponding folder according to SOLID

   The agent moved the classes into `service`, `domain` and `repository` packages next to `model` (later renamed `api`) and `exception`,
   made the cross-package types public, and moved the `Scoreboard` tests to match (decision 9).

8. > scoreboard class contains too many methods and is too overcomplicated

   The public methods are required by the task, so the agent moved everything else out: name validation into a
   `TeamName` value type, the summary order and argument checks into `Match`, leaving `Scoreboard` as a thin
   orchestrator. Tests unchanged (decision 13).

9. > let's do 3. subscribe. Don't forget that we do TDD

   The agent asked three design questions (event shape, listener failures, replay of current state), presented a
   short design, then wrote the failing `SubscribeTest` first and implemented `EventPublisher` (decision 14).
