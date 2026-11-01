import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class ArticleConditionClient {

    public static void main(String[] args) {

        // Verbindung zum gRPC-Server herstellen
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        try {
            var client = ArticleConditionServiceGrpc.newBlockingStub(channel);

            // Szenario: Neuwertiger Artikel wird angenommen
            var newItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00457")
                    .setSku("SH-BLZ-042-M")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.AS_NEW)
                    .setTagsPresent(true)
                    .setOriginalPackaging(true)
                    .build();

            var accepted = client.checkCondition(newItem);
            System.out.println("Neuwertig: " + accepted.getDecision());

            // Szenario: Beschädigter Artikel wird abgelehnt
            var damagedItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00458")
                    .setSku("SH-TSHIRT-001")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.DAMAGED)
                    .build();

            var rejected = client.checkCondition(damagedItem);
            System.out.println("Beschädigt: " + rejected.getDecision());

            // Szenario: Leichte Gebrauchsspuren benötigen manuelle Prüfung
            var wornItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00459")
                    .setSku("SH-JACKET-001")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.MINOR_WEAR)
                    .build();

            var manual = client.checkCondition(wornItem);
            System.out.println("Leichte Gebrauchsspuren: " + manual.getDecision());
            System.out.println("Manuelle Prüfung nötig: "
                    + manual.getRequiresManualReview());

            // Gespeichertes Ergebnis erneut abrufen
            var savedRequest = ArticleConditionServiceOuterClass.GetCheckResultRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00457")
                    .setSku("SH-BLZ-042-M")
                    .build();

            var saved = client.getCheckResult(savedRequest);
            System.out.println("Gespeichertes Ergebnis: " + saved.getDecision());

            // Mehrere gültige Artikel gleichzeitig prüfen
            var batchRequest = ArticleConditionServiceOuterClass.ConditionCheckBatchRequest
                    .newBuilder()
                    .addItems(newItem)
                    .addItems(damagedItem)
                    .addItems(wornItem)
                    .build();

            var batchResponse = client.checkConditionBatch(batchRequest);

            for (var result : batchResponse.getResultsList()) {
                System.out.println("Batch: " + result.getSku()
                        + " -> " + result.getDecision());
            }

            // Fehlerfall: Retouren-ID fehlt
            var invalidItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setSku("SH-INVALID-001")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.AS_NEW)
                    .build();

            try {
                client.checkCondition(invalidItem);
            } catch (StatusRuntimeException exception) {
                System.out.println("Fehler Einzelprüfung: "
                        + exception.getStatus().getCode());
            }

            // Fehlerfall: Sammelanfrage enthält einen ungültigen Artikel
            var invalidBatch = ArticleConditionServiceOuterClass.ConditionCheckBatchRequest
                    .newBuilder()
                    .addItems(newItem)
                    .addItems(invalidItem)
                    .build();

            try {
                client.checkConditionBatch(invalidBatch);
            } catch (StatusRuntimeException exception) {
                System.out.println("Fehler Stapelprüfung: "
                        + exception.getStatus().getCode());
            }

            // Fehlerfall: Prüfergebnis existiert nicht
            var unknownResult = ArticleConditionServiceOuterClass.GetCheckResultRequest
                    .newBuilder()
                    .setReturnId("RET-UNKNOWN")
                    .setSku("UNKNOWN-SKU")
                    .build();

            try {
                client.getCheckResult(unknownResult);
            } catch (StatusRuntimeException exception) {
                System.out.println("Fehler Ergebnisabruf: "
                        + exception.getStatus().getCode());
            }

        } finally {
            // Verbindung schließen
            channel.shutdown();
        }
    }
}
