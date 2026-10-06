package io.github.maksimlk.worldcup.scoreboard.model;

public record MatchSnapshot(MatchId id, String homeTeam, String awayTeam, int homeScore, int awayScore) {
}
