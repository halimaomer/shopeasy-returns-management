# ShopEasy Returns Management

Gruppenprojekt im Modul Digitalisierung von Geschäftsprozessen (WS 26/27).
Wir digitalisieren über das Semester den Retourenprozess der ShopEasy GmbH.

Team 3: Selma & Halima

## Projektstruktur

- `src/` – Mock-REST-API für Retouren von Amazon/Zalando (Spring Boot)
  - `model/` – Datenmodell (`ReturnRequest`, `ReturnItem`, `Customer`)
  - `repository/` – Testdaten und Zugriff auf die Retouren
  - `service/` – Logik 
  - `controller/` – REST-Endpunkte
- `grpc-service/` – gRPC-Service für die Zustandsprüfung im Lager (folgt)
- `messaging/` – Status-Updates über RabbitMQ (folgt)

## Sprint 2 – Integration

In Sprint 2 bauen wir drei Komponenten, die später im Camunda-Workflow verwendet werden:

1. **REST-API** (Spring Boot): Liefert Retourenmeldungen der Marktplätze, filterbar nach Status, Marktplatz und Grund. Später kann man außerdem eine einzelne Retoure und die Bestelldaten abfragen und den Status ändern.
2. **gRPC-Service**: Bewertet den Zustand eines zurückgeschickten Artikels und empfiehlt ACCEPT, REJECT oder MANUAL_REVIEW. Grundlage ist die vorgegebene `article_condition_service.proto`.
3. **Messaging** (RabbitMQ): Ein Producer verschickt Statusänderungen, ein Consumer liest sie und aktualisiert den Status über die REST-API.

### Aktueller Stand

- [x] Datenmodell und Testdaten (10 Retouren, Amazon und Zalando)
- [x] `GET /returns` mit Filtern nach Status, Marktplatz und Grund


### Statusmodell

```
NEW -> APPROVED -> LABEL_CREATED -> RECEIVED -> INSPECTED -> ACCEPTED -> REFUNDED
```

Abzweigungen:
- NEW -> REJECTED (Retoure nicht zulässig)
- LABEL_CREATED -> CANCELLED (Rücksendefrist abgelaufen)
- INSPECTED -> REJECTED (Zustand nicht akzeptabel)

Andere Übergänge sollen nicht erlaubt sein und von der API mit `409 Conflict` abgelehnt werden.

### REST-Endpunkte

Fertig:
```
GET   /returns
GET   /returns?status=NEW
GET   /returns?marketplace=ZALANDO
GET   /returns?reason=DEFECTIVE
GET   /returns?status=NEW&marketplace=AMAZON&reason=DEFECTIVE
```

Geplant:
```
GET   /returns/{returnId}
GET   /orders/{orderId}
PATCH /returns/{returnId}/status
```

### Messaging (geplant)

- Exchange `returns.status` (Typ topic)
- Routing-Key: `return.status.<status>`, z. B. `return.status.received`
- Queues: `status-updater`, `customer-notification`, `status-dead-letter`

### Warum welche Technik?

- **REST** für die Marktplätze, weil das externe Partner sind und REST der gängige Standard ist.
- **gRPC** für die Zustandsprüfung, weil es ein interner Service ist und der Prozess direkt auf das Ergebnis wartet.
- **Messaging** für Status-Updates, weil keine direkte Antwort nötig ist und mehrere Systeme die gleiche Info brauchen.

## Starten

Voraussetzungen: Java 21, Docker (für RabbitMQ, folgt)

REST-API (Port 8080), im Hauptordner:
```
.\mvnw.cmd spring-boot:run      # Windows
```
Danach im Browser: `http://localhost:8080/returns`
Fürs Filter nach Marktplatz: http://localhost:8080/returns?marketplace=ZALANDO

## KI-Nutzung

Wir haben Claude für die Planung, das Statusmodell und erste Code-Grundgerüste genutzt.
Außerdem haben wir uns von Claude die Testdaten generieren lassen.
