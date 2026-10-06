package io.github.maksimlk.worldcup.scoreboard.service;

import static io.github.maksimlk.worldcup.scoreboard.service.ScoreboardFixture.UNKNOWN_MATCH_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maksimlk.worldcup.scoreboard.api.MatchId;
import io.github.maksimlk.worldcup.scoreboard.api.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.exception.MatchNotFoundException;
import org.junit.jupiter.api.Test;

/**
 * Specifies {@link Scoreboard#finishMatch(MatchId)}.
 *
 * <p>Finishing removes the match from the board and frees both teams to start a new match.
 * Finishing an unknown or already finished match is rejected without touching live matches.
 */
class FinishMatchTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void finishedMatchLeavesSummaryAndOthersRemain() {
        MatchId mexicoCanada = scoreboard.startMatch("Mexico", "Canada");
        MatchId spainBrazil = scoreboard.startMatch("Spain", "Brazil");

        scoreboard.finishMatch(mexicoCanada);

        assertThat(scoreboard.getSummary())
                .extracting(MatchSnapshot::id)
                .containsExactly(spainBrazil);
    }

    @Test
    void finishingReleasesHomeTeam() {
        MatchId id = scoreboard.startMatch("Spain", "Brazil");
        scoreboard.finishMatch(id);

        MatchId next = scoreboard.startMatch("Spain", "Germany");

        assertThat(scoreboard.getSummary()).extracting(MatchSnapshot::id).containsExactly(next);
    }

    @Test
    void finishingReleasesAwayTeam() {
        MatchId id = scoreboard.startMatch("Spain", "Brazil");
        scoreboard.finishMatch(id);

        MatchId next = scoreboard.startMatch("France", "Brazil");

        assertThat(scoreboard.getSummary()).extracting(MatchSnapshot::id).containsExactly(next);
    }

    @Test
    void rejectsUnknownMatchAndLeavesLiveMatchesUntouched() {
        MatchId live = scoreboard.startMatch("Mexico", "Canada");

        assertThatThrownBy(() -> scoreboard.finishMatch(UNKNOWN_MATCH_ID))
                .isInstanceOf(MatchNotFoundException.class);
        assertThat(scoreboard.getSummary()).extracting(MatchSnapshot::id).containsExactly(live);
    }

    @Test
    void rejectsFinishingTwice() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");
        scoreboard.finishMatch(id);

        assertThatThrownBy(() -> scoreboard.finishMatch(id))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void rejectsNullId() {
        assertThatNullPointerException().isThrownBy(() -> scoreboard.finishMatch(null));
    }
}
