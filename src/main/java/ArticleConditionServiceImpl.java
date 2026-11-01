import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ArticleConditionServiceImpl
        extends ArticleConditionServiceGrpc.ArticleConditionServiceImplBase {

    // Speichert Prüfergebnisse im Arbeitsspeicher
    private final Map<String, ArticleConditionServiceOuterClass.ConditionCheckResponse>
            results = new ConcurrentHashMap<>();

    // Prüft einen einzelnen retournierten Artikel
    @Override
    public void checkCondition(
            ArticleConditionServiceOuterClass.ConditionCheckRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckResponse> responseObserver) {

        try {
            var response = evaluateCondition(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }

    // Prüft mehrere Artikel in einer Sammelanfrage
    @Override
    public void checkConditionBatch(
            ArticleConditionServiceOuterClass.ConditionCheckBatchRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckBatchResponse> responseObserver) {

        try {
            if (request.getItemsCount() == 0) {
                throw new IllegalArgumentException(
                        "Die Sammelanfrage muss mindestens einen Artikel enthalten");
            }

            var batchResponse = ArticleConditionServiceOuterClass.ConditionCheckBatchResponse
                    .newBuilder();

            // Alle Artikel prüfen und Ergebnisse sammeln
            for (var item : request.getItemsList()) {
                batchResponse.addResults(evaluateCondition(item));
            }

            responseObserver.onNext(batchResponse.build());
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

        String key = request.getReturnId() + ":" + request.getSku();
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

    // Gemeinsame Bewertungslogik für Einzel- und Sammelprüfung
    private ArticleConditionServiceOuterClass.ConditionCheckResponse evaluateCondition(
            ArticleConditionServiceOuterClass.ConditionCheckRequest request) {

        // Pflichtfelder überprüfen
        if (request.getReturnId().isBlank() || request.getSku().isBlank()) {
            throw new IllegalArgumentException(
                    "Return-ID und SKU dürfen nicht leer sein");
        }

        var decision = ArticleConditionServiceOuterClass.Decision.MANUAL_REVIEW;
        double confidence = 0.60;
        String reason = "Zustand muss manuell geprüft werden";

        // Bewertung anhand des erfassten Artikelzustands
        switch (request.getObservedCondition()) {

            case AS_NEW:
                if (request.getTagsPresent()) {
                    decision = ArticleConditionServiceOuterClass.Decision.ACCEPT;
                    confidence = 0.98;
                    reason = "Artikel ist neuwertig und Etiketten sind vorhanden";
                } else {
                    confidence = 0.80;
                    reason = "Artikel ist neuwertig, aber Etiketten fehlen";
                }
                break;

            case MINOR_WEAR:
                confidence = 0.75;
                reason = "Leichte Gebrauchsspuren erfordern eine manuelle Prüfung";
                break;

            case VISIBLY_WORN:
            case DAMAGED:
            case MISSING_PARTS:
                decision = ArticleConditionServiceOuterClass.Decision.REJECT;
                confidence = 0.95;
                reason = "Artikelzustand erfüllt die Rücknahmebedingungen nicht";
                break;

            case MISSING_TAGS:
                confidence = 0.85;
                reason = "Etiketten fehlen, manuelle Entscheidung erforderlich";
                break;

            default:
                reason = "Artikelzustand wurde nicht eindeutig angegeben";
                break;
        }

        // Antwort mit Entscheidung und Begründung erstellen
        var response = ArticleConditionServiceOuterClass.ConditionCheckResponse
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

        // Ergebnis für späteren Abruf speichern
        results.put(request.getReturnId() + ":" + request.getSku(), response);

        return response;
    }
}