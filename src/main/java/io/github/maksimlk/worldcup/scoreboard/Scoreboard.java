package io.github.maksimlk.worldcup.scoreboard;

import io.github.maksimlk.worldcup.scoreboard.model.MatchId;
import io.github.maksimlk.worldcup.scoreboard.model.MatchSnapshot;
import io.github.maksimlk.worldcup.scoreboard.model.ScoreChange;
import io.github.maksimlk.worldcup.scoreboard.model.Side;
import java.util.List;

public final class Scoreboard {

    public MatchId startMatch(String homeTeam, String awayTeam) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void updateScore(MatchId matchId, Side side, ScoreChange change) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void finishMatch(MatchId matchId) {
        throw new UnsupportedOperationException("not implemented");
    }

    public List<MatchSnapshot> getSummary() {
        throw new UnsupportedOperationException("not implemented");
    }
}
