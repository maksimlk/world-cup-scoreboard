package io.github.maksimlk.worldcup.scoreboard.api;

/**
 * Identifies a match on a {@code Scoreboard}.
 *
 * <p>Ids are issued by the scoreboard's repository ({@code MatchRepository.nextId()}), whose contract
 * is that each new id is greater than every id issued before. A higher value therefore means a more
 * recently started match, which the summary relies on to break ties. An id created by hand is simply
 * unknown to the scoreboard.
 *
 * @param value the numeric id
 */
public record MatchId(long value) {
}
