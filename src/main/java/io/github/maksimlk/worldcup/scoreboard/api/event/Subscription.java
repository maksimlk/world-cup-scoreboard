package io.github.maksimlk.worldcup.scoreboard.api.event;

/** A listener's registration with a scoreboard, returned by {@code Scoreboard.subscribe}. */
public interface Subscription {

    /**
     * Stops delivering events to the listener. Safe to call more than once, and from within the
     * listener itself while an event is being delivered.
     */
    void cancel();
}
