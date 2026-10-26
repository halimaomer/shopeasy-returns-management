# ShopEasy Returns Management

Gruppenprojekt im Modul Digitalisierung von Geschäftsprozessen (WS 26/27).
Wir digitalisieren über das Semester den Retourenprozess der ShopEasy GmbH.

Team: Selma, Halima

## Ordner

- `docs/` – BPMN-Modelle aus Sprint 1
- `rest-api/` – Mock-REST-API für Retouren von Amazon/Zalando
- `grpc-service/` – gRPC-Service für die Zustandsprüfung im Lager
- `messaging/` – Status-Updates über RabbitMQ

## Sprint 2 – Integration

In Sprint 2 bauen wir drei Komponenten, die später im Camunda-Workflow verwendet werden:

1. **REST-API** (Spring Boot): Liefert Retourenmeldungen der Marktplätze, filterbar nach Status und Marktplatz. Außerdem kann man eine einzelne Retoure, die Bestelldaten und den Status ändern.
2. **gRPC-Service**: Bewertet den Zustand eines zurückgeschickten Artikels und empfiehlt ACCEPT, REJECT oder MANUAL_REVIEW. Grundlage ist die vorgegebene `article_condition_service.proto`.
3. **Messaging** (RabbitMQ): Ein Producer verschickt Statusänderungen, ein Consumer liest sie und aktualisiert den Status über die REST-API.

### Statusmodell

```
NEW -> APPROVED -> LABEL_CREATED -> RECEIVED -> INSPECTED -> ACCEPTED -> REFUNDED
```

Abzweigungen:
- NEW -> REJECTED (Retoure nicht zulässig)
- LABEL_CREATED -> CANCELLED (Rücksendefrist abgelaufen)
- INSPECTED -> REJECTED (Zustand nicht akzeptabel)

Andere Übergänge sind nicht erlaubt und werden von der API mit `409 Conflict` abgelehnt.

### REST-Endpunkte

```
GET   /returns?status=NEW&marketplace=ZALANDO
GET   /returns/{returnId}
GET   /orders/{orderId}
PATCH /returns/{returnId}/status
```

### Messaging

- Exchange `returns.status` (Typ topic)
- Routing-Key: `return.status.<status>`, z. B. `return.status.received`
- Queues: `status-updater`, `customer-notification`, `status-dead-letter`

### Warum welche Technik?

- **REST** für die Marktplätze, weil das externe Partner sind und REST der gängige Standard ist.
- **gRPC** für die Zustandsprüfung, weil es ein interner Service ist und der Prozess direkt auf das Ergebnis wartet.
- **Messaging** für Status-Updates, weil keine direkte Antwort nötig ist und mehrere Systeme die gleiche Info brauchen.

## Starten

Voraussetzungen: Java 21, Maven (bzw. Maven Wrapper), Docker

```
# 1. RabbitMQ
cd messaging
docker compose up -d

# 2. REST-API (Port 8080)
cd rest-api
mvnw.cmd spring-boot:run

# 3. gRPC-Server (Port 50051)
# folgt
```

## KI-Nutzung

Wir haben Claude (Anthropic) für die Planung, das Statusmodell und erste Code-Grundgerüste genutzt. Den Code haben wir selbst geprüft und angepasst. Details ergänzen wir laufend.
