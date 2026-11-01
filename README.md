# ShopEasy Returns Management

Gruppenprojekt im Modul **Digitalisierung von Geschäftsprozessen (WS 26/27)**.
Wir digitalisieren über das Semester den Retourenprozess der ShopEasy GmbH.

**Team 3:** Selma & Halima

## Projektstruktur

- `src/main/java/de/shopeasy/returnsmanagement/` – Spring-Boot-Anwendung
  - `model/` – Datenmodelle (`ReturnRequest`, `ReturnItem`, `Customer`, `Order`) und `ReturnStatus`
  - `repository/` – In-Memory-Testdaten und Datenzugriff
  - `service/` – Retouren- und Bestelllogik, einschließlich Statusprüfung
  - `controller/` – REST-Endpunkte
  - `client/` – REST-API-Client
  - `exception/` – Fehlerbehandlung für REST-Anfragen
- `src/main/proto/article_condition_service.proto` – vorgegebene, unveränderte gRPC-Schnittstelle
- `src/main/java/ArticleConditionServiceImpl.java` – Implementierung der gRPC-Zustandsprüfung
- `src/main/java/ArticleConditionServer.java` – gRPC-Server
- `src/main/java/ArticleConditionClient.java` – gRPC-Testclient
- `src/test/` – automatisierte Tests, unter anderem für den REST-Client
- RabbitMQ-Messaging – noch umzusetzen

Die Java-Klassen für gRPC werden beim Maven-Build aus der `.proto`-Datei generiert. Da die vorgegebene Datei kein Java-Package definiert, liegen die selbst geschriebenen gRPC-Klassen derzeit ebenfalls im Standard-Package.

## Sprint 2 – Integration

In Sprint 2 entwickeln wir drei Komponenten, die später im Camunda-Workflow verwendet werden können:

1. **Mock-REST-API (Spring Boot):** Retouren von Amazon und Zalando abrufen, filtern und bearbeiten sowie Bestelldaten abfragen.
2. **gRPC-Service:** Zustand zurückgesendeter Artikel bewerten und `ACCEPT`, `REJECT` oder `MANUAL_REVIEW` empfehlen. Grundlage ist die bereitgestellte `.proto`-Datei.
3. **Messaging (RabbitMQ, noch offen):** Statusänderungen über Nachrichten versenden und von einem Consumer auf Retouren anwenden.

### Aktueller Stand

- [x] Datenmodell und In-Memory-Testdaten mit zehn Retouren von Amazon und Zalando
- [x] REST-Endpunkte für Retourenliste, Filter, Einzelretoure, Bestellung und Statusänderung
- [x] REST-API-Client und erfolgreiche Client-Tests
- [x] Statusübergänge über `ReturnStatus` geprüft
- [x] gRPC-Service, Server und Client implementiert
- [x] gRPC-Einzelprüfung, Stapelprüfung und Ergebnisabruf manuell erfolgreich getestet
- [x] gRPC-Szenarien `ACCEPT`, `REJECT`, `MANUAL_REVIEW` sowie Fehlerfälle getestet
- [ ] RabbitMQ-Broker, Producer und Consumer implementieren und testen
- [ ] Dokumentation nach Abschluss der Messaging-Komponente ergänzen

## Statusmodell

Der reguläre Bearbeitungsweg lautet:

```text
NEW -> APPROVED -> LABEL_CREATED -> RECEIVED -> INSPECTED -> ACCEPTED -> REFUNDED
```

Zusätzlich sind **genau diese Abzweigungen** in `ReturnStatus.java` erlaubt:

| Aktueller Status | Erlaubte Folgestatus |
| --- | --- |
| `NEW` | `APPROVED`, `CANCELLED` |
| `APPROVED` | `LABEL_CREATED`, `CANCELLED` |
| `LABEL_CREATED` | `RECEIVED` |
| `RECEIVED` | `INSPECTED` |
| `INSPECTED` | `ACCEPTED`, `REJECTED`, `MANUAL_REVIEW` |
| `MANUAL_REVIEW` | `ACCEPTED`, `REJECTED` |
| `ACCEPTED` | `REFUNDED` |
| `REJECTED` | keine (Endstatus) |
| `REFUNDED` | keine (Endstatus) |
| `CANCELLED` | keine (Endstatus) |

`MANUAL_REVIEW` steht für eine fachliche Prüfung eines Grenzfalls. `REJECTED`, `REFUNDED` und `CANCELLED` sind Endstatus.

Die REST-API prüft Statusänderungen anhand dieser Tabelle. Nicht erlaubte Übergänge werden mit **HTTP 409 Conflict** abgelehnt. Ein unbekannter Zielstatus führt zu **HTTP 400 Bad Request**, eine nicht vorhandene Retoure zu **HTTP 404 Not Found**.

**Hinweis:** Der fachlich ebenfalls denkbare Übergang `LABEL_CREATED -> CANCELLED` bei abgelaufener Rücksendefrist ist aktuell **nicht implementiert**. Wenn wir ihn später aufnehmen, ändern wir die Übergangstabelle und diese Dokumentation gemeinsam. Auch `NEW -> REJECTED` ist derzeit nicht erlaubt.

### Beispiel für die Statusprüfung

```text
NEW -> REFUNDED   : 409 Conflict (nicht erlaubt)
NEW -> APPROVED   : 200 OK (erlaubt)
```

Die Retouren werden für Sprint 2 nur im Arbeitsspeicher gehalten. Nach einem Neustart der Anwendung stehen wieder die initialen Testdaten zur Verfügung.

## REST-Endpunkte

Alle folgenden Endpunkte sind implementiert:

```http
GET   /returns
GET   /returns?status=NEW
GET   /returns?marketplace=ZALANDO
GET   /returns?reason=DEFECTIVE
GET   /returns?status=NEW&marketplace=AMAZON&reason=DEFECTIVE
GET   /returns/{returnId}
GET   /orders/{orderId}
PATCH /returns/{returnId}/status
```

Für `PATCH` wird der neue Status als einfacher Text im Request-Body übergeben (`Content-Type: text/plain`), beispielsweise `APPROVED`.

## gRPC-Service

Der Service implementiert die drei RPC-Methoden der mitgelieferten Datei `article_condition_service.proto`:

- `CheckCondition`: einen Artikel bewerten
- `CheckConditionBatch`: mehrere Artikel in einer Anfrage bewerten
- `GetCheckResult`: ein bereits gespeichertes Ergebnis erneut abrufen

Die Bewertung wird durch einfache, nachvollziehbare Regeln simuliert:

| Beispielzustand | Entscheidung |
| --- | --- |
| `AS_NEW` mit vorhandenen Etiketten | `ACCEPT` |
| `DAMAGED`, `VISIBLY_WORN` oder `MISSING_PARTS` | `REJECT` |
| `MINOR_WEAR`, `MISSING_TAGS` oder `AS_NEW` ohne Etiketten | `MANUAL_REVIEW` |

Die Antworten enthalten eine Entscheidung, eine Begründung, einen Confidence-Score und einen Zeitstempel. Ergebnisse werden vorübergehend im Arbeitsspeicher gespeichert.

**Fehlerbehandlung:** Fehlende Retouren-ID oder SKU führen zu gRPC `INVALID_ARGUMENT`. Eine leere oder ungültige Stapelanfrage wird ebenfalls abgelehnt. Für nicht gespeicherte Prüfergebnisse liefert `GetCheckResult` `NOT_FOUND`.

Die drei RPC-Methoden und die genannten Entscheidungsszenarien sowie Fehlerfälle wurden über den gRPC-Client manuell erfolgreich ausgeführt.

## Messaging mit RabbitMQ (geplant)

Für den Nachrichtenaustausch sind vorgesehen:

- Ein **Producer**, der eine Statusänderung als Nachricht veröffentlicht
- Ein **RabbitMQ-Broker**, der Nachrichten über eine Queue vermittelt
- Ein **Consumer**, der Nachrichten empfängt und den Retourenstatus über die bestehende Statuslogik aktualisiert

Die konkrete Exchange-, Routing-Key- und Queue-Konfiguration wird bei der Implementierung festgelegt. Der Consumer soll dieselben erlaubten Statusübergänge wie die REST-API beachten.

## Warum welche Technik?

- **REST** für die Anbindung externer Marktplätze und den einfachen Abruf von Retouren- und Bestelldaten.
- **gRPC** für die synchrone interne Zustandsprüfung im Lager.
- **Messaging** für asynchrone Statusupdates, bei denen Sender und Empfänger nicht unmittelbar miteinander kommunizieren müssen.

## Starten und testen

**Voraussetzungen:** Java 21 und Maven. Für RabbitMQ wird später ein laufender Broker benötigt, beispielsweise über Docker.

### REST-API starten

Im Projektordner:

```powershell
mvn spring-boot:run
```

Die API ist anschließend unter `http://localhost:8080/returns` erreichbar.

Beispiel zum Filtern:

```text
http://localhost:8080/returns?marketplace=ZALANDO
```

Beispiel für eine Statusänderung unter Windows (PowerShell):

```powershell
curl.exe -i -X PATCH "http://localhost:8080/returns/RET-2026-00457/status" -H "Content-Type: text/plain" --data-raw "APPROVED"
```

### gRPC starten

1. Projekt mit `mvn compile` kompilieren.
2. `ArticleConditionServer.java` in VS Code über **Run Java** starten. Der Server läuft auf Port `9090`.
3. Den Server geöffnet lassen und `ArticleConditionClient.java` über **Run Java** starten.

Der Client demonstriert Annahme, Ablehnung, manuelle Prüfung, Stapelverarbeitung, Ergebnisabruf und Fehlerbehandlung.

### Automatisierte Tests

```powershell
mvn test
```

## KI-Nutzung

Wir haben Claude für die Planung, das Statusmodell und erste Code-Grundgerüste genutzt. Außerdem haben wir uns von Claude die Testdaten generieren lassen.