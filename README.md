# ShopEasy Returns Management

Gruppenprojekt im Modul Digitalisierung von Geschäftsprozessen (WS 26/27).
Wir digitalisieren über das Semester den Retourenprozess der ShopEasy GmbH.

Team 3: Selma & Halima

## Projektstruktur

- `src/main/java/de/shopeasy/returnsmanagement/` – Spring-Boot-Anwendung
  - `model/` – Datenmodelle und `ReturnStatus`
  - `repository/` – Testdaten (10 Retouren, 10 Bestellungen)
  - `service/` – Logik, z. B. Prüfung der Statusübergänge
  - `controller/` – REST-Endpunkte
  - `client/` – REST-Client
  - `exception/` – Fehlerbehandlung
  - `messaging/` – RabbitMQ (Konfiguration, Producer, Consumer)
- `src/main/proto/article_condition_service.proto` – vorgegebene gRPC-Schnittstelle (nicht verändert)
- `src/main/java/ArticleCondition*.java` – gRPC-Service, Server und Client
- `src/test/` – Tests für den REST-Client
- `docker-compose.yml` – startet RabbitMQ

Die gRPC-Klassen liegen im Standard-Package, weil die vorgegebene `.proto`-Datei kein Java-Package festlegt.

## Sprint 2

Wir haben drei Komponenten gebaut, die wir später im Camunda-Workflow nutzen wollen:

1. **REST-API** für die Retouren von Amazon und Zalando
2. **gRPC-Service**, der den Zustand eines zurückgeschickten Artikels bewertet
3. **Messaging mit RabbitMQ** für Statusänderungen

Stand:
- [x] REST-API mit Client und Tests
- [x] Statusübergänge werden geprüft
- [x] gRPC-Service, Server und Client
- [x] RabbitMQ mit Producer und Consumer

## Statusmodell

Normaler Ablauf:

```
NEW -> APPROVED -> LABEL_CREATED -> RECEIVED -> INSPECTED -> ACCEPTED -> REFUNDED
```

Erlaubte Übergänge (siehe `ReturnStatus.java`):

| Von | Nach |
| --- | --- |
| NEW | APPROVED, REJECTED, CANCELLED |
| APPROVED | LABEL_CREATED, CANCELLED |
| LABEL_CREATED | RECEIVED, CANCELLED |
| RECEIVED | INSPECTED |
| INSPECTED | ACCEPTED, REJECTED, MANUAL_REVIEW |
| MANUAL_REVIEW | ACCEPTED, REJECTED |
| ACCEPTED | REFUNDED |

REJECTED, REFUNDED und CANCELLED sind Endstatus.

Warum diese Abzweigungen:
- NEW -> REJECTED: Retoure ist nicht zulässig
- NEW/APPROVED -> CANCELLED: Kunde zieht die Retoure zurück
- LABEL_CREATED -> CANCELLED: Rücksendefrist ist abgelaufen
- INSPECTED -> MANUAL_REVIEW: Grenzfall, jemand muss manuell entscheiden

Bei LABEL_CREATED wird automatisch eine Label-URL erzeugt.

Nicht erlaubte Übergänge geben `409 Conflict`, ein unbekannter Status `400 Bad Request` und eine unbekannte Retoure `404 Not Found`.

Die Daten liegen nur im Arbeitsspeicher. Nach einem Neustart sind wieder die Testdaten da.

## REST-API

```
GET   /returns
GET   /returns?status=NEW&marketplace=AMAZON&reason=DEFECTIVE
GET   /returns/{returnId}
GET   /orders/{orderId}
PATCH /returns/{returnId}/status
```

Die Filter (status, marketplace, reason) kann man einzeln oder kombiniert nutzen.

`GET /returns` gibt die Liste wie im Beispiel der Aufgabe im Feld `data` zurück:

```json
{ "data": [ { "returnId": "RET-2026-00457", "status": "NEW", ... } ] }
```

Für die Statusänderung schickt man JSON:

```json
{ "status": "APPROVED" }
```

## gRPC-Service

Umgesetzt sind alle drei Methoden aus der `.proto`-Datei: `CheckCondition`, `CheckConditionBatch` und `GetCheckResult`.

Regeln für die Bewertung:
- Neuwertig mit Etiketten und Originalverpackung -> ACCEPT
- Neuwertig, aber Etiketten oder Verpackung fehlen -> MANUAL_REVIEW
- Beschädigt, deutlich getragen oder Teile fehlen, mit Foto -> REJECT
- Das Gleiche ohne Foto -> MANUAL_REVIEW, weil eine Ablehnung belegt sein soll
- Leichte Gebrauchsspuren oder fehlende Etiketten -> MANUAL_REVIEW

Fehlerfälle:
- Return-ID oder SKU fehlt -> `INVALID_ARGUMENT`
- Zustand nicht angegeben -> `INVALID_ARGUMENT`
- `inspected_at` ungültig oder in der Zukunft -> `INVALID_ARGUMENT`
- Batch leer oder mit einem ungültigen Artikel -> `INVALID_ARGUMENT` (dann wird nichts gespeichert)
- Ergebnis nicht vorhanden -> `NOT_FOUND`

Der Client probiert alle diese Fälle aus.

## Messaging mit RabbitMQ

Andere Systeme (Marktplatz, Lager) schicken Statusänderungen als Nachricht an RabbitMQ. Ein Consumer in unserer App liest sie und ändert den Status der Retoure.

```
Producer -> Exchange "returns.status" (Topic)
               |-- return.status.#  -> Queue "status-updater" -> Consumer
               |                              | bei Fehler
               |                              v
               |                        "returns.status.dlx" -> Queue "status-dead-letter"
               |
               |-- label_created / rejected / refunded -> Queue "customer-notification"
```

- **returns.status**: Topic-Exchange, verteilt die Nachrichten nach Routing-Key (z. B. `return.status.approved`)
- **status-updater**: bekommt alle Statusänderungen, hier hört der Consumer zu
- **customer-notification**: bekommt nur Änderungen, die für den Kunden wichtig sind (z. B. für ein späteres E-Mail-System)
- **status-dead-letter**: hier landen Nachrichten, die nicht verarbeitet werden konnten

Eine Nachricht sieht so aus:

```json
{
  "messageId": "3b2f6c1e-...",
  "returnId": "RET-2026-00457",
  "newStatus": "APPROVED",
  "source": "MARKETPLACE",
  "occurredAt": "2026-11-01T20:15:00Z"
}
```

Der Consumer benutzt den gleichen `ReturnService` wie die REST-API, deshalb gelten dieselben Statusregeln. Wenn ein Übergang nicht erlaubt ist oder die Retoure nicht existiert, wird die Nachricht abgelehnt und landet in der Dead-Letter-Queue. Wir stellen sie nicht nochmal zu, weil ein ungültiger Übergang beim zweiten Versuch auch nicht gültig wird.

Der Producer kennt nur die Exchange und nicht die Queues. So sind Sender und Empfänger unabhängig voneinander.

## Warum welche Technik?

- **REST** für die Marktplätze, weil das externe Partner sind und REST der Standard ist.
- **gRPC** für die Zustandsprüfung, weil es ein interner Service ist und der Prozess direkt auf das Ergebnis wartet.
- **Messaging** für Status-Updates, weil keine direkte Antwort nötig ist und mehrere Systeme die gleiche Info brauchen.

## Starten

Voraussetzungen: Java 21 und Docker Desktop.

**RabbitMQ starten:**
```powershell
docker compose up -d
```
Weboberfläche: http://localhost:15672 (Login: shopeasy / shopeasy)

**REST-API starten** (Port 8080):
```powershell
.\mvnw.cmd spring-boot:run
```
Dann im Browser z. B. http://localhost:8080/returns oder http://localhost:8080/returns?marketplace=ZALANDO

Status ändern in PowerShell:
```powershell
Invoke-RestMethod -Method Patch -Uri "http://localhost:8080/returns/RET-2026-00457/status" -ContentType "application/json" -Body '{"status": "APPROVED"}'
```

**Messaging testen** (App muss laufen, zweites Terminal):
```powershell
.\mvnw.cmd compile exec:java "-Dexec.mainClass=de.shopeasy.returnsmanagement.messaging.StatusUpdateProducerClient"
```
Danach sollte RET-2026-00466 auf LABEL_CREATED und RET-2026-00460 auf RECEIVED stehen. In `status-dead-letter` liegen 3 Nachrichten und in `customer-notification` 1.

Eine einzelne Nachricht schicken:
```powershell
.\mvnw.cmd exec:java "-Dexec.mainClass=de.shopeasy.returnsmanagement.messaging.StatusUpdateProducerClient" "-Dexec.args=RET-2026-00462 MANUAL_REVIEW WAREHOUSE"
```

**gRPC starten:**
1. `.\mvnw.cmd compile`
2. `ArticleConditionServer.java` in VS Code mit Run Java starten (Port 9090)
3. Danach `ArticleConditionClient.java` starten

**Tests:**
```powershell
.\mvnw.cmd test
```
Vorher die laufende App beenden, weil die Tests auch Port 8080 brauchen.

## KI-Nutzung

Wir haben Claude für die Planung, das Statusmodell und erste Code-Grundgerüste genutzt. Außerdem haben wir uns von Claude die Testdaten generieren lassen.
