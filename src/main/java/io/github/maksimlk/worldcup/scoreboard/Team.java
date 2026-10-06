package io.github.maksimlk.worldcup.scoreboard;

import java.util.Locale;
import java.util.Objects;

/**
 * A team name as entered by the caller, trimmed. Two teams are equal when their names match
 * ignoring case, so "Brazil", " brazil " and "BRAZIL" are the same team.
 */
final class Team {

    private final String name;
    private final String key;

    private Team(String name) {
        this.name = name;
        this.key = name.toLowerCase(Locale.ROOT);
    }

    /**
     * @param rawName   the name as given by the caller
     * @param parameter the parameter name, used in error messages
     */
    static Team of(String rawName, String parameter) {
        Objects.requireNonNull(rawName, parameter);
        String name = rawName.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException(parameter + " must not be blank");
        }
        return new Team(name);
    }

    String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Team team && key.equals(team.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
