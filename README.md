# Mongo User Items POC

## Descrizione del progetto
Proof of Concept (POC) di un'API REST CRUD basata su **Java 21**, **Spring Boot 4.1.2**, **Spring Data MongoDB** e **Maven**. L'applicazione gestisce utenti e relativi item personalizzati memorizzati come documenti embedded in MongoDB, con documentazione interattiva esposta tramite OpenAPI/Swagger UI.

## Funzionalità principali
- Gestione completa (CRUD) degli utenti.
- Gestione completa (CRUD) degli item associati a un utente.
- Modello dati documentale: gli item sono incorporati (`embedded documents`) nel documento `User` nella collection `users`.
- Validazione dei payload in ingresso tramite Jakarta Bean Validation.
- Gestione centralizzata degli errori HTTP con `ProblemDetail`:
  - `404 Not Found` per risorse inesistenti.
  - `400 Bad Request` per errori di validazione.
  - `201 Created` per le creazioni.
  - `204 No Content` per le cancellazioni.
- Documentazione e test manuale degli endpoint tramite Swagger UI.
- Auditing MongoDB abilitato (`@CreatedDate`, `@LastModifiedDate`, `@Version`).
- Popolamento iniziale del database al primo avvio (solo se la collection è vuota), con utenti e item di esempio di tipologie diverse (`TASK`, `EXPENSE`, `RECIPE`, `NOTE`, `TRAVEL`).

## Modello dati
Gli item fanno parte del ciclo di vita dell'utente: la cancellazione di un utente comporta la rimozione anche dei suoi item.

```text
users (collection)
 ├── _id (String, generato da MongoDB)
 ├── version (Long, optimistic locking)
 ├── name (String)
 ├── email (String)
 ├── createdAt (Instant)
 ├── updatedAt (Instant)
 └── items[] (embedded UserItem)
      ├── id (String, UUID)
      ├── type (String, indicizzato)
      ├── title (String)
      ├── createdAt (Instant)
      └── data (Map<String, Object>, contenuto flessibile per tipo)
```

Questa struttura è adatta al POC perché lettura e scrittura avvengono insieme al documento padre. In scenari con molti item per utente, aggiornamenti concorrenti frequenti o query indipendenti sugli item, può essere preferibile una collection separata con riferimento all'utente.

## Requisiti tecnici
- **JDK**: 21 o superiore
- **Maven**: 3.9 o superiore
- **Docker** e **Docker Compose**: per l'esecuzione di MongoDB
- **MongoDB**: immagine `mongo:latest` avviata tramite `docker-compose.yml`
- **Spring Boot**: 4.1.2 (parent Maven)
- **Librerie principali**:
  - `spring-boot-starter-webmvc`
  - `spring-boot-starter-data-mongodb`
  - `spring-boot-starter-validation`
  - `springdoc-openapi-starter-webmvc-ui` (3.2.0)

## Installazione e avvio

### 1. Avvio di MongoDB
Dalla directory del progetto:

```bash
docker compose up -d
```

Su Windows è disponibile anche lo script:

```bat
start.bat
```

Il servizio MongoDB espone la porta `27017` con credenziali di default configurate in `docker-compose.yml` (`root` / `example`, database iniziale `test`). I dati sono persistiti nel percorso locale `.docker` (bind mount).

### 2. Avvio dell'applicazione
Con Maven:

```bash
mvn spring-boot:run
```

Oppure tramite JAR:

```bash
mvn clean package
java -jar target/mongo-user-items-poc-1.0.0.jar
```

L'applicazione è disponibile di default su `http://localhost:8080`.

## Configurazione
Le principali impostazioni si trovano in `src/main/resources/application.yml`:

| Proprietà | Valore predefinito | Descrizione |
|---|---|---|
| `spring.mongodb.uri` | `mongodb://root:example@localhost:27017/test?authSource=admin` | URI di connessione a MongoDB (sovrascrivibile con `MONGODB_URI`) |
| `spring.data.mongodb.auto-index-creation` | `true` | Creazione automatica degli indici |
| `server.port` | `8080` | Porta HTTP dell'applicazione |

Esempio di override della connessione:

```bash
set MONGODB_URI=mongodb://utente:password@host:27017/nome_db?authSource=admin
mvn spring-boot:run
```

## Modalità di utilizzo

### Documentazione API (Swagger UI)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Da Swagger UI è possibile esplorare e invocare direttamente tutti gli endpoint.

### Endpoint - Utenti (`/api/users`)

| Metodo | Endpoint | Descrizione | Risposta |
|---|---|---|---|
| `GET` | `/api/users` | Elenca tutti gli utenti con i rispettivi item | `200 OK` |
| `GET` | `/api/users/{userId}` | Recupera un utente per ID | `200 OK` / `404` |
| `POST` | `/api/users` | Crea un nuovo utente | `201 Created` |
| `PUT` | `/api/users/{userId}` | Aggiorna nome ed email dell'utente | `200 OK` / `404` |
| `DELETE` | `/api/users/{userId}` | Elimina l'utente e tutti i suoi item | `204 No Content` / `404` |

Esempio richiesta `POST /api/users`:

```json
{
  "name": "Mario Rossi",
  "email": "mario.rossi@example.com"
}
```

Vincoli `UserRequest`: `name` obbligatorio (max 100 caratteri), `email` obbligatoria, valida e max 160 caratteri.

### Endpoint - Item utente (`/api/users/{userId}/items`)

| Metodo | Endpoint | Descrizione | Risposta |
|---|---|---|---|
| `GET` | `/api/users/{userId}/items` | Elenca gli item dell'utente | `200 OK` / `404` |
| `GET` | `/api/users/{userId}/items/{itemId}` | Recupera un item specifico | `200 OK` / `404` |
| `POST` | `/api/users/{userId}/items` | Crea un item per l'utente | `201 Created` / `404` |
| `PUT` | `/api/users/{userId}/items/{itemId}` | Aggiorna un item (mantiene `id` e `createdAt` originali) | `200 OK` / `404` |
| `DELETE` | `/api/users/{userId}/items/{itemId}` | Elimina un item dell'utente | `204 No Content` / `404` |

Esempio richiesta `POST /api/users/{userId}/items`:

```json
{
  "type": "TASK",
  "title": "Leggere documentazione MongoDB",
  "data": {
    "description": "Approfondire document model e query",
    "priority": "HIGH",
    "completed": false,
    "tags": ["FORMAZIONE"]
  }
}
```

Vincoli `ItemRequest`: `type` e `title` obbligatori (`@NotBlank`), `data` è una mappa opzionale a struttura libera.

## Struttura del codice
```text
src/main/java/it/davideangelone/mongopoc/
├── MongoUserItemsApplication.java   # Entry point Spring Boot
├── config/
│   ├── DataInitializer.java         # Seed iniziale dei dati (CommandLineRunner)
│   ├── MongoConfig.java             # Abilitazione auditing MongoDB
│   └── OpenApiConfig.java           # Configurazione OpenAPI/Swagger
├── controller/
│   ├── UserController.java          # Endpoint REST per utenti
│   └── ItemController.java          # Endpoint REST per item utente
├── dto/
│   ├── UserRequest.java             # Payload input utente con validazione
│   └── ItemRequest.java             # Payload input item con validazione
├── exception/
│   ├── ApiExceptionHandler.java     # Gestione centralizzata errori HTTP
│   └── ResourceNotFoundException.java
├── model/
│   ├── User.java                    # Document MongoDB (collection "users")
│   └── UserItem.java                # Embedded document (record)
├── repository/
│   └── UserRepository.java          # MongoRepository<User, String>
└── service/
    └── UserService.java             # Logica applicativa utenti e item
```

## Note e limitazioni
- Gli ID degli utenti sono generati da MongoDB; gli ID degli item sono UUID generati dall'applicazione.
- Il POC non implementa autenticazione/autorizzazione, paginazione, ricerca avanzata o multi-tenancy.
- Il seed dati è idempotente: se la collection contiene già documenti, non vengono inseriti dati di esempio.
- Il database utilizza un volume persistente. Per azzerare i dati (operazione distruttiva): `docker compose down -v` ed eliminare, se necessario, la directory `.docker`.
