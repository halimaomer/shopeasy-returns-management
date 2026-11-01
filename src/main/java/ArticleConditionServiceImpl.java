
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ArticleConditionServiceImpl
        extends ArticleConditionServiceGrpc.ArticleConditionServiceImplBase {

    // Speichert die Ergebnisse bereits durchgeführter Prüfungen
    private final Map<String, ArticleConditionServiceOuterClass.ConditionCheckResponse>
            results = new ConcurrentHashMap<>();

    // Bewertet den Zustand eines einzelnen retournierten Artikels
    @Override
    public void checkCondition(
            ArticleConditionServiceOuterClass.ConditionCheckRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckResponse> responseObserver) {

        if (request.getReturnId().isBlank() || request.getSku().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Return-ID und SKU dürfen nicht leer sein")
                    .asRuntimeException());
            return;
        }

        var decision = ArticleConditionServiceOuterClass.Decision.MANUAL_REVIEW;
        double confidence = 0.60;
        String reason = "Zustand muss manuell geprüft werden";

        // Entscheidung anhand des vom Lager erfassten Artikelzustands
        switch (request.getObservedCondition()) {
            case AS_NEW:
                if (request.getTagsPresent()) {
                    decision = ArticleConditionServiceOuterClass.Decision.ACCEPT;
                    confidence = 0.98;
                    reason = "Artikel ist neuwertig und Etiketten sind vorhanden";
                } else {
                    reason = "Artikel ist neuwertig, aber Etiketten fehlen";
                }
                break;

            case MINOR_WEAR:
                reason = "Leichte Gebrauchsspuren erfordern eine manuelle Prüfung";
                confidence = 0.75;
                break;

            case VISIBLY_WORN:
            case DAMAGED:
            case MISSING_PARTS:
                decision = ArticleConditionServiceOuterClass.Decision.REJECT;
                confidence = 0.95;
                reason = "Artikelzustand erfüllt die Rücknahmebedingungen nicht";
                break;

            case MISSING_TAGS:
                reason = "Etiketten fehlen, manuelle Entscheidung erforderlich";
                confidence = 0.85;
                break;

            default:
                reason = "Artikelzustand wurde nicht eindeutig angegeben";
                break;
        }

        // Baut die Antwort für den gRPC-Client zusammen
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

        // Speichert das Ergebnis für einen späteren Abruf
        results.put(request.getReturnId() + ":" + request.getSku(), response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Bewertet mehrere Artikel innerhalb einer Anfrage
    @Override
    public void checkConditionBatch(
            ArticleConditionServiceOuterClass.ConditionCheckBatchRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckBatchResponse> responseObserver) {

        var batchResponse = ArticleConditionServiceOuterClass.ConditionCheckBatchResponse
                .newBuilder();

        for (var item : request.getItemsList()) {
            var resultObserver = new StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckResponse>() {

                @Override
                public void onNext(ArticleConditionServiceOuterClass.ConditionCheckResponse value) {
                    batchResponse.addResults(value);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Fehlerhafte Artikel werden separat behandelt
                }

                @Override
                public void onCompleted() {
                }
            };

            checkCondition(item, resultObserver);
        }

        responseObserver.onNext(batchResponse.build());
        responseObserver.onCompleted();
    }

    // Ruft das Ergebnis einer früheren Artikelprüfung ab
    @Override
    public void getCheckResult(
            ArticleConditionServiceOuterClass.GetCheckResultRequest request,
            StreamObserver<ArticleConditionServiceOuterClass.ConditionCheckResponse> responseObserver) {

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
}
