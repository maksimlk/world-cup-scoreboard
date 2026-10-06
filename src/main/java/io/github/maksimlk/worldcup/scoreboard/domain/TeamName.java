package io.github.maksimlk.worldcup.scoreboard.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * A team name, trimmed and never blank. Two names are equal when they match ignoring case, so
 * "Brazil", " brazil " and "BRAZIL" denote the same team; {@link #value()} keeps the caller's
 * capitalisation.
 */
public final class TeamName {

    private final String value;
    private final String key;

    /**
     * @throws NullPointerException if the name is {@code null}
     * @throws IllegalArgumentException if the name is blank
     */
    public TeamName(String rawName) {
        String trimmed = Objects.requireNonNull(rawName, "team name").strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("team name must not be blank");
        }
        this.value = trimmed;
        this.key = trimmed.toLowerCase(Locale.ROOT);
    }

    /** The trimmed name with the caller's capitalisation. */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TeamName name && key.equals(name.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
