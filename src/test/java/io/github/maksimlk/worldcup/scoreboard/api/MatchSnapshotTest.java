package io.github.maksimlk.worldcup.scoreboard.api;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/** {@link MatchSnapshot} is public API, so it guards its own invariants. */
class MatchSnapshotTest {

    private static final MatchId ID = new MatchId(1);

    @Test
    void rejectsNullFields() {
        assertThatNullPointerException().isThrownBy(() -> new MatchSnapshot(null, "Mexico", "Canada", 0, 0));
        assertThatNullPointerException().isThrownBy(() -> new MatchSnapshot(ID, null, "Canada", 0, 0));
        assertThatNullPointerException().isThrownBy(() -> new MatchSnapshot(ID, "Mexico", null, 0, 0));
    }

    @Test
    void rejectsNegativeScores() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MatchSnapshot(ID, "Mexico", "Canada", -1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new MatchSnapshot(ID, "Mexico", "Canada", 0, -1));
    }
}
