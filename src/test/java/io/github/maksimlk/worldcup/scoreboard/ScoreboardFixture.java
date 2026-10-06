package io.github.maksimlk.worldcup.scoreboard;

import static io.github.maksimlk.worldcup.scoreboard.model.ScoreChange.GOAL;
import static io.github.maksimlk.worldcup.scoreboard.model.Side.AWAY;
import static io.github.maksimlk.worldcup.scoreboard.model.Side.HOME;

import io.github.maksimlk.worldcup.scoreboard.model.MatchId;

final class ScoreboardFixture {

    /** An id no fresh scoreboard has handed out. */
    static final MatchId UNKNOWN_MATCH_ID = new MatchId(Long.MAX_VALUE);

    private ScoreboardFixture() {
    }

    static MatchId startWithScore(Scoreboard scoreboard, String home, String away, int homeGoals, int awayGoals) {
        MatchId id = scoreboard.startMatch(home, away);
        for (int i = 0; i < homeGoals; i++) {
            scoreboard.updateScore(id, HOME, GOAL);
        }
        for (int i = 0; i < awayGoals; i++) {
            scoreboard.updateScore(id, AWAY, GOAL);
        }
        return id;
    }
}
