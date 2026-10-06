package io.github.maksimlk.worldcup.scoreboard.service;

import static io.github.maksimlk.worldcup.scoreboard.api.ScoreChange.GOAL;
import static io.github.maksimlk.worldcup.scoreboard.api.ScoreChange.GOAL_CANCELLED;
import static io.github.maksimlk.worldcup.scoreboard.api.Side.AWAY;
import static io.github.maksimlk.worldcup.scoreboard.api.Side.HOME;
import static io.github.maksimlk.worldcup.scoreboard.service.ScoreboardFixture.UNKNOWN_MATCH_ID;
import static io.github.maksimlk.worldcup.scoreboard.service.ScoreboardFixture.startWithScore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.maksimlk.worldcup.scoreboard.api.MatchId;
import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.api.event.MatchFinished;
import io.github.maksimlk.worldcup.scoreboard.api.event.MatchStarted;
import io.github.maksimlk.worldcup.scoreboard.api.event.ScoreChanged;
import io.github.maksimlk.worldcup.scoreboard.api.event.ScoreboardEvent;
import io.github.maksimlk.worldcup.scoreboard.api.event.Subscription;
import io.github.maksimlk.worldcup.scoreboard.exception.ListenerFailedException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Specifies {@link Scoreboard#subscribe}, the additional operation.
 *
 * <p>Rules covered:
 * <ul>
 *   <li>each successful start, score update and finish publishes one event carrying the match
 *       as it is after the change;</li>
 *   <li>rejected calls publish nothing;</li>
 *   <li>listeners receive only later events, synchronously and in subscription order;</li>
 *   <li>a cancelled subscription receives nothing, even when cancelled during delivery;</li>
 *   <li>a failing listener does not stop the others; failures are reported afterwards.</li>
 * </ul>
 */
class SubscribeTest {

    private final Scoreboard scoreboard = new Scoreboard();
    private final List<ScoreboardEvent> events = new ArrayList<>();

    @Test
    void startedMatchIsPublished() {
        scoreboard.subscribe(events::add);

        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        assertThat(events).containsExactly(new MatchStarted(new MatchSnapshot(id, "Mexico", "Canada", 0, 0)));
    }

    @Test
    void goalIsPublishedWithNewScore() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");
        scoreboard.subscribe(events::add);

        scoreboard.updateScore(id, AWAY, GOAL);

        assertThat(events).containsExactly(
                new ScoreChanged(new MatchSnapshot(id, "Mexico", "Canada", 0, 1), AWAY, GOAL));
    }

    @Test
    void cancelledGoalIsPublishedWithNewScore() {
        MatchId id = startWithScore(scoreboard, "Mexico", "Canada", 1, 0);
        scoreboard.subscribe(events::add);

        scoreboard.updateScore(id, HOME, GOAL_CANCELLED);

        assertThat(events).containsExactly(
                new ScoreChanged(new MatchSnapshot(id, "Mexico", "Canada", 0, 0), HOME, GOAL_CANCELLED));
    }

    @Test
    void finishedMatchIsPublishedWithFinalScore() {
        MatchId id = startWithScore(scoreboard, "Mexico", "Canada", 2, 1);
        scoreboard.subscribe(events::add);

        scoreboard.finishMatch(id);

        assertThat(events).containsExactly(new MatchFinished(new MatchSnapshot(id, "Mexico", "Canada", 2, 1)));
    }

    @Test
    void rejectedCallsPublishNothing() {
        MatchId id = scoreboard.startMatch("Spain", "Brazil");
        scoreboard.subscribe(events::add);

        catchThrowable(() -> scoreboard.startMatch("Spain", "Germany"));
        catchThrowable(() -> scoreboard.updateScore(UNKNOWN_MATCH_ID, HOME, GOAL));
        catchThrowable(() -> scoreboard.updateScore(id, HOME, GOAL_CANCELLED));
        catchThrowable(() -> scoreboard.finishMatch(UNKNOWN_MATCH_ID));

        assertThat(events).isEmpty();
    }

    @Test
    void listenerReceivesOnlyEventsAfterSubscribing() {
        scoreboard.startMatch("Mexico", "Canada");
        scoreboard.subscribe(events::add);

        MatchId spainBrazil = scoreboard.startMatch("Spain", "Brazil");

        assertThat(events).extracting(event -> event.match().id()).containsExactly(spainBrazil);
    }

    @Test
    void listenersAreNotifiedInSubscriptionOrder() {
        List<String> calls = new ArrayList<>();
        scoreboard.subscribe(event -> calls.add("first"));
        scoreboard.subscribe(event -> calls.add("second"));

        scoreboard.startMatch("Mexico", "Canada");

        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void cancelledSubscriptionReceivesNothing() {
        Subscription subscription = scoreboard.subscribe(events::add);

        subscription.cancel();
        scoreboard.startMatch("Mexico", "Canada");

        assertThat(events).isEmpty();
    }

    @Test
    void cancellingTwiceDoesNotAffectOtherSubscriptions() {
        Subscription cancelled = scoreboard.subscribe(event -> { });
        scoreboard.subscribe(events::add);

        cancelled.cancel();
        cancelled.cancel();
        scoreboard.startMatch("Mexico", "Canada");

        assertThat(events).hasSize(1);
    }

    @Test
    void listenerCanCancelItselfDuringDelivery() {
        List<ScoreboardEvent> received = new ArrayList<>();
        Subscription[] self = new Subscription[1];
        self[0] = scoreboard.subscribe(event -> {
            received.add(event);
            self[0].cancel();
        });
        scoreboard.subscribe(events::add);

        scoreboard.startMatch("Mexico", "Canada");
        scoreboard.startMatch("Spain", "Brazil");

        assertThat(received).hasSize(1);
        assertThat(events).hasSize(2);
    }

    @Test
    void listenerCancelledDuringDeliveryMissesTheCurrentEvent() {
        Subscription[] later = new Subscription[1];
        scoreboard.subscribe(event -> later[0].cancel());
        later[0] = scoreboard.subscribe(events::add);

        scoreboard.startMatch("Mexico", "Canada");

        assertThat(events).isEmpty();
    }

    @Test
    void failingListenerDoesNotStopOthersAndIsReported() {
        RuntimeException failure = new IllegalStateException("dashboard down");
        scoreboard.subscribe(event -> {
            throw failure;
        });
        scoreboard.subscribe(events::add);

        assertThatThrownBy(() -> scoreboard.startMatch("Mexico", "Canada"))
                .isInstanceOf(ListenerFailedException.class)
                .hasCause(failure);
        assertThat(events).hasSize(1);
        assertThat(scoreboard.getSummary()).hasSize(1);
    }

    @Test
    void furtherListenerFailuresAreSuppressed() {
        RuntimeException first = new IllegalStateException("first");
        RuntimeException second = new IllegalStateException("second");
        scoreboard.subscribe(event -> {
            throw first;
        });
        scoreboard.subscribe(event -> {
            throw second;
        });

        Throwable thrown = catchThrowable(() -> scoreboard.startMatch("Mexico", "Canada"));

        assertThat(thrown).isInstanceOf(ListenerFailedException.class).hasCause(first);
        assertThat(thrown.getSuppressed()).containsExactly(second);
    }

    @Test
    void sameListenerSubscribedTwiceReceivesEachEventTwice() {
        scoreboard.subscribe(events::add);
        scoreboard.subscribe(events::add);

        scoreboard.startMatch("Mexico", "Canada");

        assertThat(events).hasSize(2);
    }

    @Test
    void rejectsNullListener() {
        assertThatNullPointerException().isThrownBy(() -> scoreboard.subscribe(null));
    }
}
