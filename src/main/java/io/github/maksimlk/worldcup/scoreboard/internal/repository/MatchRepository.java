package io.github.maksimlk.worldcup.scoreboard.internal.repository;

import io.github.maksimlk.worldcup.scoreboard.api.MatchId;
import io.github.maksimlk.worldcup.scoreboard.internal.domain.Match;
import io.github.maksimlk.worldcup.scoreboard.internal.domain.TeamName;
import java.util.Collection;
import java.util.Optional;

/** Stores live matches. {@code Scoreboard} depends on this abstraction, not on a storage mechanism. */
public interface MatchRepository {

    /** Returns a new id, greater than every id issued before. */
    MatchId nextId();

    /** Stores the match, replacing any match with the same id. */
    void save(Match match);

    Optional<Match> findById(MatchId id);

    /** Finds the match the team plays in, on either side. */
    Optional<Match> findByTeam(TeamName team);

    /** Returns all stored matches, in no particular order. */
    Collection<Match> findAll();

    /** Removes the match and returns whether it was stored. */
    boolean remove(MatchId id);
}
