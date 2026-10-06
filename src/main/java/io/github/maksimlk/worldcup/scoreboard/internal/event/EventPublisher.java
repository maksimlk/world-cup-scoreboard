package io.github.maksimlk.worldcup.scoreboard.internal.event;

import io.github.maksimlk.worldcup.scoreboard.api.event.ScoreboardEvent;
import io.github.maksimlk.worldcup.scoreboard.api.event.Subscription;
import io.github.maksimlk.worldcup.scoreboard.exception.ListenerFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Delivers scoreboard events to subscribed listeners, synchronously and in subscription order.
 * Not thread-safe.
 */
public final class EventPublisher {

    private final List<Registration> registrations = new ArrayList<>();

    /**
     * Registers a listener. Subscribing the same listener twice creates two independent
     * subscriptions.
     *
     * @throws NullPointerException if the listener is {@code null}
     */
    public Subscription subscribe(Consumer<ScoreboardEvent> listener) {
        Registration registration = new Registration(Objects.requireNonNull(listener, "listener"));
        registrations.add(registration);
        return registration;
    }

    /**
     * Delivers the event to every active listener, even if some of them fail.
     *
     * @throws ListenerFailedException after delivery, if any listener threw
     */
    public void publish(ScoreboardEvent event) {
        List<RuntimeException> failures = new ArrayList<>();
        for (Registration registration : List.copyOf(registrations)) {
            if (registration.active) {
                try {
                    registration.listener.accept(event);
                } catch (RuntimeException failure) {
                    failures.add(failure);
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new ListenerFailedException(failures);
        }
    }

    /** A listener's subscription; once cancelled, it receives nothing more, even mid-delivery. */
    private final class Registration implements Subscription {

        private final Consumer<ScoreboardEvent> listener;
        private boolean active = true;

        private Registration(Consumer<ScoreboardEvent> listener) {
            this.listener = listener;
        }

        @Override
        public void cancel() {
            active = false;
            registrations.remove(this);
        }
    }
}
