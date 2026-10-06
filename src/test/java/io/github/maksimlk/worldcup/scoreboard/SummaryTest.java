package io.github.maksimlk.worldcup.scoreboard;

import static io.github.maksimlk.worldcup.scoreboard.ScoreboardFixture.startWithScore;
import static io.github.maksimlk.worldcup.scoreboard.model.ScoreChange.GOAL;
import static io.github.maksimlk.worldcup.scoreboard.model.ScoreChange.GOAL_CANCELLED;
import static io.github.maksimlk.worldcup.scoreboard.model.Side.HOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.MatchSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Specifies {@link Scoreboard#getSummary()}.
 *
 * <p>The summary lists live matches by total score, highest first; ties go to the most
 * recently started match. It is an unmodifiable point-in-time snapshot.
 */
class SummaryTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void summaryOfEmptyBoardIsEmpty() {
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    /** The example scenario from the task specification, verbatim. */
    @Test
    void ordersExampleScenarioFromSpecification() {
        startWithScore(scoreboard, "Mexico", "Canada", 0, 5);
        startWithScore(scoreboard, "Spain", "Brazil", 10, 2);
        startWithScore(scoreboard, "Germany", "France", 2, 2);
        startWithScore(scoreboard, "Uruguay", "Italy", 6, 6);
        startWithScore(scoreboard, "Argentina", "Australia", 3, 1);

        assertThat(scoreboard.getSummary())
                .extracting(SummaryTest::asScoreLine)
                .containsExactly(
                        "Uruguay 6 - Italy 6",
                        "Spain 10 - Brazil 2",
                        "Mexico 0 - Canada 5",
                        "Argentina 3 - Australia 1",
                        "Germany 2 - France 2");
    }

    private static String asScoreLine(MatchSnapshot match) {
        return match.homeTeam() + " " + match.homeScore() + " - " + match.awayTeam() + " " + match.awayScore();
    }

    @Test
    void tiedTotalsPutMostRecentlyStartedFirst() {
        MatchId first = scoreboard.startMatch("Mexico", "Canada");
        MatchId second = scoreboard.startMatch("Spain", "Brazil");
        MatchId third = scoreboard.startMatch("Germany", "France");

        assertThat(scoreboard.getSummary())
                .extracting(MatchSnapshot::id)
                .containsExactly(third, second, first);
    }

    /** Both start tied on 2, so the newer Spain-Brazil leads; a cancelled goal drops it to 1, below Mexico-Canada. */
    @Test
    void orderingFollowsCancelledGoals() {
        MatchId older = startWithScore(scoreboard, "Mexico", "Canada", 1, 1);
        MatchId newer = startWithScore(scoreboard, "Spain", "Brazil", 2, 0);

        scoreboard.updateScore(newer, HOME, GOAL_CANCELLED);

        assertThat(scoreboard.getSummary())
                .extracting(MatchSnapshot::id)
                .containsExactly(older, newer);
    }

    @Test
    void returnedSummaryIsUnmodifiable() {
        scoreboard.startMatch("Mexico", "Canada");

        List<MatchSnapshot> summary = scoreboard.getSummary();

        assertThatThrownBy(summary::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void returnedSummaryIsNotAffectedByLaterChanges() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");
        List<MatchSnapshot> before = scoreboard.getSummary();

        scoreboard.updateScore(id, HOME, GOAL);
        scoreboard.startMatch("Spain", "Brazil");

        assertThat(before).containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 0, 0));
    }
}
