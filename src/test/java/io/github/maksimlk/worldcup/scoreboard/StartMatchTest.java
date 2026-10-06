package io.github.maksimlk.worldcup.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.github.maksimlk.worldcup.scoreboard.exception.TeamAlreadyPlayingException;
import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.MatchSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Specifies {@link Scoreboard#startMatch(String, String)}.
 *
 * <p>Rules covered:
 * <ul>
 *   <li>a started match is live immediately with a 0-0 score and a unique id;</li>
 *   <li>team names must be non-null and non-blank, and are trimmed;</li>
 *   <li>teams are compared ignoring case and surrounding whitespace;</li>
 *   <li>a team cannot play itself or be in two live matches at once, whichever side it plays on;</li>
 *   <li>a rejected start leaves the board unchanged.</li>
 * </ul>
 */
class StartMatchTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void startedMatchAppearsInSummaryWithZeroScore() {
        MatchId id = scoreboard.startMatch("Mexico", "Canada");

        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(id, "Mexico", "Canada", 0, 0));
    }

    @Test
    void eachStartedMatchGetsDistinctId() {
        MatchId first = scoreboard.startMatch("Mexico", "Canada");
        MatchId second = scoreboard.startMatch("Spain", "Brazil");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void teamNamesAreTrimmed() {
        scoreboard.startMatch("  Mexico ", "\tCanada\n");

        assertThat(scoreboard.getSummary())
                .extracting(MatchSnapshot::homeTeam, MatchSnapshot::awayTeam)
                .containsExactly(tuple("Mexico", "Canada"));
    }

    @Test
    void rejectsNullHomeTeam() {
        assertThatNullPointerException().isThrownBy(() -> scoreboard.startMatch(null, "Canada"));
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    @Test
    void rejectsNullAwayTeam() {
        assertThatNullPointerException().isThrownBy(() -> scoreboard.startMatch("Mexico", null));
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void rejectsBlankHomeTeam(String blank) {
        assertThatThrownBy(() -> scoreboard.startMatch(blank, "Canada"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void rejectsBlankAwayTeam(String blank) {
        assertThatThrownBy(() -> scoreboard.startMatch("Mexico", blank))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Spain", "spain", "SPAIN", " Spain "})
    void rejectsSameTeamOnBothSides(String sameTeam) {
        assertThatThrownBy(() -> scoreboard.startMatch("Spain", sameTeam))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(scoreboard.getSummary()).isEmpty();
    }

    @Test
    void rejectsTeamAlreadyPlayingAtHome() {
        MatchId live = scoreboard.startMatch("Spain", "Brazil");

        assertThatThrownBy(() -> scoreboard.startMatch("Germany", "Spain"))
                .isInstanceOf(TeamAlreadyPlayingException.class);
        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(live, "Spain", "Brazil", 0, 0));
    }

    @Test
    void rejectsTeamAlreadyPlayingAway() {
        MatchId live = scoreboard.startMatch("Spain", "Brazil");

        assertThatThrownBy(() -> scoreboard.startMatch("Brazil", "Germany"))
                .isInstanceOf(TeamAlreadyPlayingException.class);
        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(live, "Spain", "Brazil", 0, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"brazil", "BRAZIL", " Brazil "})
    void teamAvailabilityCheckIgnoresCaseAndWhitespace(String sameTeam) {
        scoreboard.startMatch("Spain", "Brazil");

        assertThatThrownBy(() -> scoreboard.startMatch("Germany", sameTeam))
                .isInstanceOf(TeamAlreadyPlayingException.class);
    }

    /** Germany is free but Brazil is not: the failed start must not leave Germany reserved. */
    @Test
    void rejectedStartDoesNotReserveTheOtherTeam() {
        scoreboard.startMatch("Spain", "Brazil");
        assertThatThrownBy(() -> scoreboard.startMatch("Germany", "Brazil"))
                .isInstanceOf(TeamAlreadyPlayingException.class);

        MatchId germanyFrance = scoreboard.startMatch("Germany", "France");

        assertThat(scoreboard.getSummary())
                .extracting(MatchSnapshot::id)
                .contains(germanyFrance);
    }

    /** Teams can meet more than once in a tournament, e.g. group stage and knockout. */
    @Test
    void samePairingCanPlayAgainAfterFinishing() {
        MatchId groupStage = scoreboard.startMatch("Spain", "Brazil");
        scoreboard.finishMatch(groupStage);

        MatchId knockout = scoreboard.startMatch("Brazil", "Spain");

        assertThat(knockout).isNotEqualTo(groupStage);
        assertThat(scoreboard.getSummary())
                .containsExactly(new MatchSnapshot(knockout, "Brazil", "Spain", 0, 0));
    }
}
