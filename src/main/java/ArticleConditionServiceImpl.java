import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ArticleConditionServiceImpl
        extends ArticleConditionServiceGrpc.ArticleConditionServiceImplBase {

    // Speichert bereits durchgeführte Artikelprüfungen
    private final Map<String, ArticleConditionServiceOuterClass.ConditionCheckResponse>
            results = new ConcurrentHashMap<>();

    // Prüft einen einzelnen retournierten Artikel
    @Override
    public void checkCondition(
            ArticleConditionServiceOuterClass.ConditionCheckRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckResponse> responseObserver) {

        try {
            var response = evaluateCondition(request);

            // Erst nach erfolgreicher Bewertung speichern
            results.put(createKey(request.getReturnId(), request.getSku()), response);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }

    // Prüft mehrere Artikel, ohne bei einem Fehler Teilergebnisse zu speichern
    @Override
    public void checkConditionBatch(
            ArticleConditionServiceOuterClass.ConditionCheckBatchRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckBatchResponse> responseObserver) {

        try {
            if (request.getItemsCount() == 0) {
                throw new IllegalArgumentException(
                        "Die Sammelanfrage muss mindestens einen Artikel enthalten");
            }

            // Zunächst alle Ergebnisse nur lokal sammeln
            List<ArticleConditionServiceOuterClass.ConditionCheckResponse>
                    evaluatedResults = new ArrayList<>();

            for (var item : request.getItemsList()) {
                evaluatedResults.add(evaluateCondition(item));
            }

            // Erst speichern, wenn wirklich alle Artikel gültig waren
            synchronized (results) {
                for (var response : evaluatedResults) {
                    results.put(
                            createKey(response.getReturnId(), response.getSku()),
                            response);
                }
            }

            var batchResponse = ArticleConditionServiceOuterClass.ConditionCheckBatchResponse
                    .newBuilder()
                    .addAllResults(evaluatedResults)
                    .build();

            responseObserver.onNext(batchResponse);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }

    // Ruft ein bereits gespeichertes Prüfergebnis ab
    @Override
    public void getCheckResult(
            ArticleConditionServiceOuterClass.GetCheckResultRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckResponse> responseObserver) {

        if (request.getReturnId().isBlank() || request.getSku().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Return-ID und SKU dürfen nicht leer sein")
                    .asRuntimeException());
            return;
        }

        String key = createKey(request.getReturnId(), request.getSku());
        var result = results.get(key);

        if (result == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Kein Prüfergebnis für diesen Artikel vorhanden")
                    .asRuntimeException());
            return;
        }

        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    // Erstellt den Schlüssel für ein gespeichertes Prüfergebnis
    private String createKey(String returnId, String sku) {
        return returnId + ":" + sku;
    }

    // Bewertet einen Artikel, ohne ihn direkt zu speichern
    private ArticleConditionServiceOuterClass.ConditionCheckResponse evaluateCondition(
            ArticleConditionServiceOuterClass.ConditionCheckRequest request) {

        validateRequest(request);

        var decision = ArticleConditionServiceOuterClass.Decision.MANUAL_REVIEW;
        double confidence = 0.60;
        String reason = "Zustand muss manuell geprüft werden";

        // Prüfen, ob mindestens eine Foto-URL übermittelt wurde
        boolean hasPhotos = request.getImageUrlsList().stream()
                .anyMatch(url -> !url.isBlank());

        switch (request.getObservedCondition()) {

            case AS_NEW:
                if (!request.getTagsPresent()) {
                    confidence = 0.80;
                    reason = "Artikel ist neuwertig, aber Etiketten fehlen";

                } else if (!request.getOriginalPackaging()) {
                    confidence = 0.80;
                    reason = "Artikel ist neuwertig, aber Originalverpackung fehlt";

                } else {
                    decision = ArticleConditionServiceOuterClass.Decision.ACCEPT;
                    confidence = 0.98;
                    reason = "Artikel ist neuwertig, Etiketten und Originalverpackung sind vorhanden";
                }
                break;

            case MINOR_WEAR:
                confidence = 0.75;
                reason = "Leichte Gebrauchsspuren erfordern eine manuelle Prüfung";
                break;

            case VISIBLY_WORN:
            case DAMAGED:
            case MISSING_PARTS:
                if (hasPhotos) {
                    decision = ArticleConditionServiceOuterClass.Decision.REJECT;
                    confidence = 0.95;
                    reason = "Artikelzustand erfüllt die Rücknahmebedingungen nicht, Fotodokumentation vorhanden";
                } else {
                    confidence = 0.70;
                    reason = "Artikelzustand ist problematisch, aber Fotodokumentation fehlt";
                }
                break;

            case MISSING_TAGS:
                confidence = 0.85;
                reason = "Etiketten fehlen, manuelle Entscheidung erforderlich";
                break;

            default:
                reason = "Artikelzustand wurde nicht eindeutig angegeben";
                break;
        }

        return ArticleConditionServiceOuterClass.ConditionCheckResponse
                .newBuilder()
                .setReturnId(request.getReturnId())
                .setSku(request.getSku())
                .setDecision(decision)
                .setConfidenceScore(confidence)
                .setReason(reason)
                .setRequiresManualReview(
                        decision == ArticleConditionServiceOuterClass.Decision.MANUAL_REVIEW)
                .setCheckedAt(Instant.now().toString())
                .build();
    }

    // Prüft Pflichtfelder, Artikelzustand und Zeitstempel
    private void validateRequest(
            ArticleConditionServiceOuterClass.ConditionCheckRequest request) {

        if (request.getReturnId().isBlank() || request.getSku().isBlank()) {
            throw new IllegalArgumentException(
                    "Return-ID und SKU dürfen nicht leer sein");
        }

        var condition = request.getObservedCondition();

        if (condition == ArticleConditionServiceOuterClass.ObservedCondition.OBSERVED_CONDITION_UNSPECIFIED
                || condition == ArticleConditionServiceOuterClass.ObservedCondition.UNRECOGNIZED) {
            throw new IllegalArgumentException(
                    "Artikelzustand wurde nicht angegeben (" + request.getSku() + ")");
        }

        // Der Prüfzeitpunkt ist optional, muss aber ISO-8601-konform sein
        if (!request.getInspectedAt().isBlank()) {
            Instant inspectedAt;

            try {
                inspectedAt = Instant.parse(request.getInspectedAt());
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException(
                        "inspected_at ist kein gültiger ISO-8601-Zeitstempel: "
                                + request.getInspectedAt());
            }

            if (inspectedAt.isAfter(Instant.now())) {
                throw new IllegalArgumentException(
                        "inspected_at darf nicht in der Zukunft liegen: "
                                + request.getInspectedAt());
            }
        }
    }
}