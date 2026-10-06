package io.github.maksimlk.worldcup.scoreboard.internal.repository;

import io.github.maksimlk.worldcup.scoreboard.api.MatchId;
import io.github.maksimlk.worldcup.scoreboard.internal.domain.Match;
import io.github.maksimlk.worldcup.scoreboard.internal.domain.TeamName;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Keeps matches in a map. Not thread-safe. */
public final class InMemoryMatchRepository implements MatchRepository {

    private final Map<MatchId, Match> matches = new HashMap<>();
    private long lastId;

    @Override
    public MatchId nextId() {
        return new MatchId(++lastId);
    }

    @Override
    public void save(Match match) {
        matches.put(match.id(), match);
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        return Optional.ofNullable(matches.get(id));
    }

    @Override
    public Optional<Match> findByTeam(TeamName team) {
        return matches.values().stream().filter(match -> match.involves(team)).findFirst();
    }

    @Override
    public Collection<Match> findAll() {
        return List.copyOf(matches.values());
    }

    @Override
    public boolean remove(MatchId id) {
        return matches.remove(id) != null;
    }
}
