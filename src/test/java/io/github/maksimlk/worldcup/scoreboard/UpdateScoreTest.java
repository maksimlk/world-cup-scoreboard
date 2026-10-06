package io.github.maksimlk.worldcup.scoreboard;

import static io.github.maksimlk.worldcup.scoreboard.ScoreboardFixture.UNKNOWN_MATCH_ID;
import static io.github.maksimlk.worldcup.scoreboard.ScoreboardFixture.startWithScore;
import static io.github.maksimlk.worldcup.scoreboard.model.ScoreChange.GOAL;
import static io.github.maksimlk.worldcup.scoreboard.model.ScoreChange.GOAL_CANCELLED;
import static io.github.maksimlk.worldcup.scoreboard.model.Side.AWAY;
import static io.github.maksimlk.worldcup.scoreboard.model.Side.HOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maksimlk.worldcup.scoreboard.exception.MatchNotFoundException;
import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.model.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.model.Side;
import org.junit.jupiter.api.Test;

/**
 * Specifies {@link Scoreboard#updateScore(MatchId, Side, ScoreChange)}.
 *
 * <p>Scores change one event at a time: a {@code GOAL} adds one to a side, a
 * {@code GOAL_CANCELLED} (e.g. overturned by VAR) removes one. A score never drops below zero.
 * Only live matches can be updated, and a rejected update leaves the score unchanged.
 */
class UpdateScoreTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void goalForHomeTeamIncrementsHomeScore() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        scoreboard.updateScore(id, HOME, GOAL);

        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 1, 0));
    }

    @Test
    void goalForAwayTeamIncrementsAwayScore() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        scoreboard.updateScore(id, AWAY, GOAL);

        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 0, 1));
    }

    @Test
    void cancelledHomeGoalDecrementsOnlyHomeScore() {
        MatchId id = startWithScore(scoreboard, "Mexico", "Canada", 2, 1);

        scoreboard.updateScore(id, HOME, GOAL_CANCELLED);

        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 1, 1));
    }

    @Test
    void cancelledAwayGoalDecrementsOnlyAwayScore() {
        MatchId id = startWithScore(scoreboard, "Mexico", "Canada", 2, 1);

        scoreboard.updateScore(id, AWAY, GOAL_CANCELLED);

        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 2, 0));
    }

    @Test
    void cancellingGoalAtZeroIsRejectedAndScoreIsUnchanged() {
        MatchId id = startWithScore(scoreboard, "Mexico", "Canada", 0, 3);

        assertThatThrownBy(() -> scoreboard.updateScore(id, HOME, GOAL_CANCELLED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 0, 3));
    }

    @Test
    void rejectsUnknownMatch() {
        assertThatThrownBy(() -> scoreboard.updateScore(UNKNOWN_MATCH_ID, HOME, GOAL))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void rejectsFinishedMatch() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");
        scoreboard.finishMatch(id);

        assertThatThrownBy(() -> scoreboard.updateScore(id, HOME, GOAL))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void rejectsNullMatchId() {
        assertThatNullPointerException().isThrownBy(() -> scoreboard.updateScore(null, HOME, GOAL));
    }

    @Test
    void rejectsNullSideAndScoreIsUnchanged() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        assertThatNullPointerException().isThrownBy(() -> scoreboard.updateScore(id, null, GOAL));
        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 0, 0));
    }

    @Test
    void rejectsNullChangeAndScoreIsUnchanged() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        assertThatNullPointerException().isThrownBy(() -> scoreboard.updateScore(id, HOME, null));
        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 0, 0));
    }
}
