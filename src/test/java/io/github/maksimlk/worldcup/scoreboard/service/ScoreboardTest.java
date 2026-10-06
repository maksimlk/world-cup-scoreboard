package io.github.maksimlk.worldcup.scoreboard.service;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/** Specifies how a {@link Scoreboard} is created. */
class ScoreboardTest {

    @Test
    void rejectsNullRepository() {
        assertThatNullPointerException().isThrownBy(() -> new Scoreboard(null));
    }
}
