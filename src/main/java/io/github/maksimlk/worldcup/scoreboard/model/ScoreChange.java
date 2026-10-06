package io.github.maksimlk.worldcup.scoreboard.model;

/** A single score event in a live match. */
public enum ScoreChange {
    /** A goal is scored: the side's score goes up by one. */
    GOAL,
    /** A goal is overturned, e.g. by VAR: the side's score goes down by one. */
    GOAL_CANCELLED
}
