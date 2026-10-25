# rynek-channel-service

`channel-service` — kanał sprzedawcy i obserwowanie

> Status: **kanały i obserwowanie działają**: tworzenie, odczyt (po id i handle), edycja, „O nas”, dane firmy, status weryfikacji, obserwuj / przestań / dzwonek, lista obserwowanych i oba endpointy `/internal`. Awatar, baner i zgłoszenie weryfikacji czekają na media-service i zwracają `501 Not Implemented` (RFC 7807).

## Uruchomienie

Wymaga PostgreSQL z `rynek-infra/local` (baza `channel`) i identity-service (JWKS, przypinanie kanału do konta):

```bash
docker compose -f ../rynek-infra/local/docker-compose.yml up -d
./mvnw spring-boot:run   # port 8082, zmienna PORT
./mvnw verify            # testy; Testcontainers uruchamia własny PostgreSQL, wymaga Dockera
```

| Zmienna | Domyślnie | Znaczenie |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/channel`, `channel`, `channel` | Baza serwisu |
| `IDENTITY_SERVICE_URL` | `http://localhost:8081` | identity-service: JWKS i `PUT /internal/users/{id}/channel` |
| `JWT_ISSUER` | `rynek-identity` | Oczekiwany claim `iss` |

Schemat zakłada Flyway (`src/main/resources/db/migration`). Hibernate tylko sprawdza zgodność encji (`ddl-auto: validate`).

- Swagger UI: http://localhost:8082/swagger-ui.html
- Zdrowie: http://localhost:8082/actuator/health

## Zakres i endpointy

_Skopiowane z `rynek-docs/architektura/rynek-endpointy-mikroserwisy.md`. Od teraz to repozytorium jest właścicielem tej specyfikacji._

Ekrany: 5 Profil sprzedawcy jako kanał. Zakładki kanału korzystają z innych serwisów: **Oferty** → `catalog-service` (`GET /offers?channelId=`), **Filmy** → `tiktok-integration-service`, **Opinie** → `engagement-service`.

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/v1/channels` | Utworzenie kanału (po wyborze roli usługodawca/merchant) | user |
| GET | `/api/v1/channels/{channelId}` | Dane kanału: nazwa, handle, opis, liczba obserwujących, liczba ofert, ocena, status weryfikacji | pub |
| GET | `/api/v1/channels/by-handle/{handle}` | Kanał po handle (`@elektromarket`) | pub |
| PATCH | `/api/v1/channels/{channelId}` | Edycja nazwy, opisu, danych kontaktowych | seller |
| PUT | `/api/v1/channels/{channelId}/avatar` | Ustawienie awatara (`mediaId` z media-service) | seller |
| PUT | `/api/v1/channels/{channelId}/banner` | Ustawienie banera (`mediaId`) | seller |
| GET | `/api/v1/channels/{channelId}/about` | Zakładka „O nas”: opis, adres, godziny, dane firmy | pub |
| PUT | `/api/v1/channels/{channelId}/business-details` | NIP, nazwa firmy, adres rejestrowy | seller |
| POST | `/api/v1/channels/{channelId}/verification` | Zgłoszenie weryfikacji (dokumenty) | seller |
| GET | `/api/v1/channels/{channelId}/verification` | Status weryfikacji | seller |
| POST | `/api/v1/channels/{channelId}/follow` | Obserwuj | user |
| DELETE | `/api/v1/channels/{channelId}/follow` | Przestań obserwować | user |
| PATCH | `/api/v1/channels/{channelId}/follow` | Włącz/wyłącz powiadomienia (dzwonek) | user |
| GET | `/api/v1/users/me/following` | Lista obserwowanych kanałów (boczne menu, „Obserwowane”) | user |
| GET | `/internal/channels/{channelId}` | Dane kanału dla innych serwisów | int |
| GET | `/internal/channels?ids=` | Wsadowo (karty ofert) | int |

## Zachowanie

- **Tworzenie kanału:** jedno konto ma jeden kanał. Handle zapisuje się bez „@” i małymi literami: 3–30 znaków, litery bez polskich znaków, cyfry, `.`, `-`, `_`. Zajęty handle daje 409. Po zapisie channel-service woła `PUT /internal/users/{userId}/channel` w identity, co tymczasowo zastępuje zdarzenie `channel.created`. Dzięki temu `GET /users/me` zwraca `channelId` i `channelHandle`. Rolę usługodawcy lub merchanta sprawdza identity, bo access token tuż po onboardingu ma jeszcze stare role. Odmowa identity (403/409) przechodzi do klienta i wycofuje kanał. Gdy identity nie działa, wynikiem jest 503.
- **Endpointy `seller`:** tylko właściciel kanału, inaczej 403.
- **Endpointy `pub`:** token nie jest wymagany. Ważny token dodaje do kanału `following` i `notificationsOn`, a brak lub zły token oznacza gościa (wtedy oba pola są `null`).
- **Obserwowanie:** `POST`/`DELETE /follow` są idempotentne (204). Dzwonek jest domyślnie wyłączony. `PATCH /follow` bez obserwowania daje 404. Obserwowanie własnego kanału daje 409.
- **Dane firmy:** NIP z cyfrą kontrolną. Separatory są pomijane, a zapisuje się 10 cyfr.
- **Pola z innych serwisów:** `offersCount` (catalog) i `rating` (engagement, `review.created`) są na razie zerowe. `avatarUrl` i `bannerUrl` są puste do czasu media-service. „O nas” zwraca puste `address` i `openingHours`, bo nie ma jeszcze endpointu do ich edycji.
- **`/internal/channels?ids=`:** id rozdzielone przecinkami, najwyżej 100. Nieznane są pomijane, kolejność jest jak w zapytaniu. Kształt odpowiedzi to `ChannelRef`.

## Zdarzenia Kafka

Wiersze z §17, w których występuje `channel` (publikuje lub słucha).

| Zdarzenie | Publikuje | Słuchają |
|---|---|---|
| `user.registered`, `user.roles-changed` | identity | channel, notification |
| `channel.created`, `channel.updated` | channel | search, feed |
| `channel.followed`, `channel.unfollowed` | channel | feed, notification, analytics |
| `review.created` | engagement | channel (ocena), search, notification |

## Dane i zależności

- **Magazyn danych:** PostgreSQL
- **Zależności:** identity-service, media-service (awatar, baner)
- **Kontrakty:** `rynek-contracts/openapi/` i `rynek-contracts/events/`
- **Lokalne uruchomienie:** `rynek-infra/local/`

## Kolejność budowy

Etap z §19: 1. `api-gateway`, `identity-service`, `channel-service` — konta i kanały.

## Wyjątki routingu

Gateway (§2) kieruje ruch po prefiksie ścieżki, a poniższe endpointy leżą pod prefiksem innego serwisu. Gateway obsługuje je trasami szczegółowymi (`order: -1`), które mają pierwszeństwo przed trasami po prefiksie. Ścieżki pozostają bez zmian (decyzja z 2026-10-24). Test `RoutingTests` w `rynek-api-gateway` sprawdza, czy każda z nich trafia do właściwego serwisu.

- `/api/v1/channels/{channelId}/videos`: należy do `tiktok-integration-service` (prefiks wskazuje `channel-service`).
- `/api/v1/channels/{channelId}/reviews`, `/api/v1/channels/{channelId}/rating-summary`: należy do `engagement-service` (prefiks wskazuje `channel-service`).
- `/api/v1/users/me/following`: należy do `channel-service` (prefiks wskazuje `identity-service`).
