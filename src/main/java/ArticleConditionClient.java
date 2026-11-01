import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ArticleConditionClient {

    public static void main(String[] args) {

        // Verbindung zum gRPC-Server herstellen
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        try {
            var client = ArticleConditionServiceGrpc.newBlockingStub(channel);

            // Neuwertiger Blazer aus der REST-Testdatenbank
            var newItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00457")
                    .setSku("SH-BLZ-042-M")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.AS_NEW)
                    .setTagsPresent(true)
                    .setOriginalPackaging(true)
                    .setInspectedAt(Instant.now().toString())
                    .build();

            var accepted = client.checkCondition(newItem);
            System.out.println("Neuwertig: " + accepted.getDecision());

            // Beschädigtes T-Shirt mit Fotodokumentation
            var damagedItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00458")
                    .setSku("SH-TSH-101-L")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.DAMAGED)
                    .addImageUrls("https://images.example.com/tshirt-damage.jpg")
                    .build();

            var rejected = client.checkCondition(damagedItem);
            System.out.println("Beschädigt mit Foto: " + rejected.getDecision());

            // Jeans mit leichten Gebrauchsspuren
            var wornItem = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00459")
                    .setSku("SH-JNS-220-32")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.MINOR_WEAR)
                    .build();

            var manual = client.checkCondition(wornItem);
            System.out.println("Leichte Gebrauchsspuren: " + manual.getDecision());
            System.out.println("Manuelle Prüfung nötig: "
                    + manual.getRequiresManualReview());

            // B2: Neuwertiger Artikel ohne Originalverpackung
            var withoutPackaging = newItem.toBuilder()
                    .setReturnId("RET-B2-PACKAGING")
                    .setOriginalPackaging(false)
                    .build();

            var packagingResult = client.checkCondition(withoutPackaging);
            System.out.println("Neuwertig ohne Verpackung: "
                    + packagingResult.getDecision());

            // B2: Beschädigter Artikel ohne Foto
            var withoutPhoto = damagedItem.toBuilder()
                    .setReturnId("RET-B2-NO-PHOTO")
                    .clearImageUrls()
                    .build();

            var noPhotoResult = client.checkCondition(withoutPhoto);
            System.out.println("Beschädigt ohne Foto: "
                    + noPhotoResult.getDecision());

            // B2: Sichtbar getragen ohne Foto
            var visiblyWorn = damagedItem.toBuilder()
                    .setReturnId("RET-B2-VISIBLY-WORN")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.VISIBLY_WORN)
                    .clearImageUrls()
                    .build();

            var visiblyWornResult = client.checkCondition(visiblyWorn);
            System.out.println("Sichtbar getragen ohne Foto: "
                    + visiblyWornResult.getDecision());

            // B2: Fehlende Teile ohne Foto
            var missingParts = damagedItem.toBuilder()
                    .setReturnId("RET-B2-MISSING-PARTS")
                    .setObservedCondition(
                            ArticleConditionServiceOuterClass.ObservedCondition.MISSING_PARTS)
                    .clearImageUrls()
                    .build();

            var missingPartsResult = client.checkCondition(missingParts);
            System.out.println("Fehlende Teile ohne Foto: "
                    + missingPartsResult.getDecision());

            // Gespeichertes Ergebnis abrufen
            var savedRequest = ArticleConditionServiceOuterClass.GetCheckResultRequest
                    .newBuilder()
                    .setReturnId("RET-2026-00457")
                    .setSku("SH-BLZ-042-M")
                    .build();

            var saved = client.getCheckResult(savedRequest);
            System.out.println("Gespeichertes Ergebnis: " + saved.getDecision());

            // Mehrere Artikel in einer gültigen Sammelanfrage prüfen
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

            expectError(client, invalidItem, "Fehlende Retouren-ID");

            // Fehlerfall: Artikelzustand fehlt
            var missingCondition = ArticleConditionServiceOuterClass.ConditionCheckRequest
                    .newBuilder()
                    .setReturnId("RET-B3-MISSING-CONDITION")
                    .setSku("SH-BLZ-042-M")
                    .build();

            expectError(client, missingCondition, "Fehlender Artikelzustand");

            // Fehlerfall: Ungültiger Zeitstempel
            var invalidTimestamp = newItem.toBuilder()
                    .setReturnId("RET-B3-INVALID-TIME")
                    .setInspectedAt("kein-gueltiges-datum")
                    .build();

            expectError(client, invalidTimestamp, "Ungültiger Zeitstempel");

            // Fehlerfall: Prüfzeitpunkt liegt in der Zukunft
            var futureTimestamp = newItem.toBuilder()
                    .setReturnId("RET-B3-FUTURE-TIME")
                    .setInspectedAt(Instant.now().plus(1, ChronoUnit.DAYS).toString())
                    .build();

            expectError(client, futureTimestamp, "Zukünftiger Zeitstempel");

            // Fehlerfall: Sammelanfrage enthält einen ungültigen Artikel
            var invalidBatch = ArticleConditionServiceOuterClass.ConditionCheckBatchRequest
                    .newBuilder()
                    .addItems(newItem)
                    .addItems(invalidItem)
                    .build();

            try {
                client.checkConditionBatch(invalidBatch);
                throw new IllegalStateException(
                        "Ungültiger Batch wurde fälschlicherweise akzeptiert");
            } catch (StatusRuntimeException exception) {
                if (exception.getStatus().getCode() != Status.Code.INVALID_ARGUMENT) {
                    throw exception;
                }
                System.out.println("Fehler Stapelprüfung: "
                        + exception.getStatus().getCode());
            }

            // Fehlerfall: Nicht vorhandenes Prüfergebnis
            var unknownResult = ArticleConditionServiceOuterClass.GetCheckResultRequest
                    .newBuilder()
                    .setReturnId("RET-UNKNOWN")
                    .setSku("UNKNOWN-SKU")
                    .build();

            try {
                client.getCheckResult(unknownResult);
                throw new IllegalStateException(
                        "Nicht vorhandenes Ergebnis wurde fälschlicherweise gefunden");
            } catch (StatusRuntimeException exception) {
                if (exception.getStatus().getCode() != Status.Code.NOT_FOUND) {
                    throw exception;
                }
                System.out.println("Fehler Ergebnisabruf: "
                        + exception.getStatus().getCode());
            }

            // B1: Neuer gültiger Artikel für den Batch-Speichertest
            var batchTestItem = newItem.toBuilder()
                    .setReturnId("RET-B1-TEST")
                    .setSku("SH-B1-UNIQUE")
                    .build();

            var batchTestResult = ArticleConditionServiceOuterClass.GetCheckResultRequest
                    .newBuilder()
                    .setReturnId("RET-B1-TEST")
                    .setSku("SH-B1-UNIQUE")
                    .build();

            // Vor dem Test darf kein Ergebnis vorhanden sein
            try {
                client.getCheckResult(batchTestResult);
                throw new IllegalStateException(
                        "B1-Test nicht möglich: Ergebnis bereits gespeichert");
            } catch (StatusRuntimeException exception) {
                if (exception.getStatus().getCode() != Status.Code.NOT_FOUND) {
                    throw exception;
                }
            }

            // Gültiger und ungültiger Artikel gemeinsam im Batch
            var b1InvalidBatch = ArticleConditionServiceOuterClass.ConditionCheckBatchRequest
                    .newBuilder()
                    .addItems(batchTestItem)
                    .addItems(invalidItem)
                    .build();

            try {
                client.checkConditionBatch(b1InvalidBatch);
                throw new IllegalStateException(
                        "B1-Test fehlgeschlagen: Batch wurde akzeptiert");
            } catch (StatusRuntimeException exception) {
                if (exception.getStatus().getCode() != Status.Code.INVALID_ARGUMENT) {
                    throw exception;
                }
                System.out.println("B1 Batch-Fehler: "
                        + exception.getStatus().getCode());
            }

            // Sicherstellen, dass der gültige Artikel nicht gespeichert wurde
            try {
                client.getCheckResult(batchTestResult);
                throw new IllegalStateException(
                        "B1-Test fehlgeschlagen: Artikel wurde gespeichert");
            } catch (StatusRuntimeException exception) {
                if (exception.getStatus().getCode() != Status.Code.NOT_FOUND) {
                    throw exception;
                }
                System.out.println("B1 Speicherung verhindert: "
                        + exception.getStatus().getCode());
                System.out.println("B1 TEST BESTANDEN");
            }

        } finally {
            // Verbindung schließen
            channel.shutdown();
        }
    }

    // Erwartet INVALID_ARGUMENT bei einer fehlerhaften Einzelprüfung
    private static void expectError(
            ArticleConditionServiceGrpc.ArticleConditionServiceBlockingStub client,
            ArticleConditionServiceOuterClass.ConditionCheckRequest request,
            String description) {

        try {
            client.checkCondition(request);
            throw new IllegalStateException(
                    "Fehlerfall wurde nicht erkannt: " + description);
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() != Status.Code.INVALID_ARGUMENT) {
                throw exception;
            }

            System.out.println(description + ": "
                    + exception.getStatus().getCode());
        }
    }
}