import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ArticleConditionClient {

    public static void main(String[] args) {

        // Verbindet den Client mit unserem gRPC-Server
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        try {
            // Erstellt einen Client für unseren gRPC-Service
            var client = ArticleConditionServiceGrpc.newBlockingStub(channel);

            // Simuliert einen neuwertigen retournierten Artikel
            var request = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00457")
                    .setSku("SH-BLZ-042-M")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.AS_NEW)
                    .setTagsPresent(true)
                    .setOriginalPackaging(true)
                    .build();

            // Schickt die Anfrage an den Server
            var response = client.checkCondition(request);

            // Zeigt das Ergebnis im Terminal an
            System.out.println("Retouren-ID: " + response.getReturnId());
            System.out.println("Artikel: " + response.getSku());
            System.out.println("Entscheidung: " + response.getDecision());
            System.out.println("Begründung: " + response.getReason());

        } finally {
            // Schließt die Verbindung
            channel.shutdown();
        }
    }
}
