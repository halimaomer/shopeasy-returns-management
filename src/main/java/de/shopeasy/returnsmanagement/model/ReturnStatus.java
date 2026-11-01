package de.shopeasy.returnsmanagement.model;

import java.util.Map;
import java.util.Set;

public enum ReturnStatus {

    NEW,
    APPROVED,
    LABEL_CREATED,
    RECEIVED,
    INSPECTED,
    MANUAL_REVIEW,
    ACCEPTED,
    REJECTED,
    REFUNDED,
    CANCELLED;

    // Definiert, welche Statusänderungen erlaubt sind
    private static final Map<ReturnStatus, Set<ReturnStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    NEW, Set.of(APPROVED, CANCELLED),
                    APPROVED, Set.of(LABEL_CREATED, CANCELLED),
                    LABEL_CREATED, Set.of(RECEIVED),
                    RECEIVED, Set.of(INSPECTED),
                    INSPECTED, Set.of(ACCEPTED, REJECTED, MANUAL_REVIEW),
                    MANUAL_REVIEW, Set.of(ACCEPTED, REJECTED),
                    ACCEPTED, Set.of(REFUNDED),
                    REJECTED, Set.of(),
                    REFUNDED, Set.of(),
                    CANCELLED, Set.of()
            );

    // Prüft, ob ein Wechsel zum nächsten Status erlaubt ist
    public boolean canTransitionTo(ReturnStatus nextStatus) {
        return ALLOWED_TRANSITIONS.get(this).contains(nextStatus);
    }
}
