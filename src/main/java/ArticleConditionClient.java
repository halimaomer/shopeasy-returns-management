import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ArticleConditionClient {

    public static void main(String[] args) {

        // Verbindung zum gRPC-Server aufbauen
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        try {
            // gRPC-Client erstellen
            var client = ArticleConditionServiceGrpc.newBlockingStub(channel);

            // Ersten Artikel für die Einzelprüfung erstellen
            var request = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00457")
                    .setSku("SH-BLZ-042-M")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.AS_NEW)
                    .setTagsPresent(true)
                    .setOriginalPackaging(true)
                    .build();

            // Einzelnen Artikel prüfen
            var response = client.checkCondition(request);

            System.out.println("Retouren-ID: " + response.getReturnId());
            System.out.println("Artikel: " + response.getSku());
            System.out.println("Entscheidung: " + response.getDecision());
            System.out.println("Begründung: " + response.getReason());

            // Gespeichertes Prüfergebnis erneut abrufen
            var savedRequest = ArticleConditionServiceOuterClass.GetCheckResultRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00457")
                    .setSku("SH-BLZ-042-M")
                    .build();

            var savedResponse = client.getCheckResult(savedRequest);

            System.out.println("Gespeichertes Ergebnis: "
                    + savedResponse.getDecision());

            // Zweiten Artikel mit beschädigtem Zustand erstellen
            var secondItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00458")
                    .setSku("SH-TSHIRT-001")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.DAMAGED)
                    .setTagsPresent(true)
                    .build();

            // Sammelanfrage mit zwei Artikeln erstellen
            var batchRequest = ArticleConditionServiceOuterClass.ConditionCheckBatchRequest
                    .newBuilder()
                    .addItems(request)
                    .addItems(secondItem)
                    .build();

            // Beide Artikel gleichzeitig prüfen
            var batchResponse = client.checkConditionBatch(batchRequest);

            // Ergebnisse der Sammelprüfung ausgeben
            for (var result : batchResponse.getResultsList()) {
                System.out.println("Batch: " + result.getSku()
                        + " -> " + result.getDecision());
            }

        } finally {
            // Verbindung zum Server schließen
            channel.shutdown();
        }
    }
}
