# Система бронирования ресурсов

Система для управления бронированием ресурсов (переговорные комнаты, оборудование, автомобили) с интеграцией оплаты через различные платежные системы.

## Технологии

- **Java 21**
- **Spring Boot 3.5.10**
- **Spring Data JPA** - работа с базой данных
- **Spring Security** - аутентификация и авторизация
- **Spring GraphQL** - GraphQL API
- **PostgreSQL** - база данных
- **Lombok** - упрощение кода
- **MapStruct** - маппинг объектов
- **Flyway** - миграции базы данных
- **JUnit/Mockito** - тестирование

## Функциональность

### 1. Управление ресурсами
- Создание, редактирование, удаление ресурсов
- Активация и деактивирование ресурсов
- Просмотр списка ресурсов с фильтрацией по активности

### 2. Управление бронированием
- Создание черновика бронирования (статус DRAFT)
- Перевод бронирования в статус ожидания оплаты (WAITING_PAYMENT)
- Автоматическое подтверждение после успешной оплаты (CONFIRMED)
- Отмена бронирования
- Проверка конфликтов времени бронирования
- Просмотр своих бронирований и всех бронирований (для администраторов)

### 3. Интеграция оплаты
- Поддержка нескольких платежных провайдеров (PayPal, банковские карты)
- Моментальные и отложенные платежи
- Обработка успешных и неудачных транзакций
- Автоматическое подтверждение бронирования после успешной оплаты

### 4. Управление пользователями
- Роли: ADMIN и USER
- Администраторы могут управлять всеми ресурсами, бронированиями и платежами
- Пользователи могут бронировать ресурсы, оплачивать бронирования и просматривать свою историю

### 5. Журналирование и аудит
- Логирование всех действий через SLF4J
- Аудит создания и изменения записей (created_at, created_by, updated_at, updated_by)
- Сохранение информации о том, кто и когда оплатил бронирование (paid_at, paid_by)

## Требования

- Java 21 или выше
- Maven 3.6+
- PostgreSQL 12+ (или Docker для запуска через docker-compose)

## Установка и запуск

### Вариант 1: Использование Docker Compose (рекомендуется)

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd resource-booking
```

2. Запустите PostgreSQL через Docker Compose:
```bash
docker-compose up -d
```

3. Дождитесь запуска базы данных (несколько секунд)

4. Запустите приложение:
```bash
./mvnw spring-boot:run
```

Или на Windows:
```bash
mvnw.cmd spring-boot:run
```

Приложение будет доступно по адресу: `http://localhost:8080`

### Вариант 2: Локальная установка PostgreSQL

1. Установите и запустите PostgreSQL

2. Создайте базу данных:
```sql
CREATE DATABASE resource_booking;
```

3. Обновите настройки подключения в `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/resource_booking
spring.datasource.username=your_username
spring.datasource.password=your_password
```

4. Запустите приложение:
```bash
./mvnw spring-boot:run
```

## Тестовые пользователи

После запуска приложения в базе данных создаются тестовые пользователи:

### Администратор
- **Email:** `admin@test.com`
- **Пароль:** `admin`
- **Роль:** ADMIN

### Пользователь
- **Email:** `user@test.com`
- **Пароль:** `user`
- **Роль:** USER

## Использование API

### GraphQL Endpoint

- **URL:** `http://localhost:8080/graphql`
- **GraphiQL UI:** `http://localhost:8080/graphiql`

### Аутентификация

API использует HTTP Basic Authentication. При каждом запросе необходимо передавать заголовки:

```
Authorization: Basic <base64(email:password)>
```

Например, для пользователя `user@test.com` с паролем `user`:
```
Authorization: Basic dXNlckB0ZXN0LmNvbTp1c2Vy
```

### Примеры запросов

#### 1. Получить список активных ресурсов

```graphql
query {
  resources(active: true) {
    id
    name
    description
    active
  }
}
```

#### 2. Создать черновик бронирования

```graphql
mutation {
  createBookingDraft(input: {
    resourceId: "resource-uuid"
    startTime: "2026-02-15T10:00:00Z"
    endTime: "2026-02-15T11:00:00Z"
  }) {
    id
    userId
    resourceId
    startTime
    endTime
    status
  }
}
```

#### 3. Перевести бронирование в статус ожидания оплаты

```graphql
mutation {
  markBookingWaitingPayment(id: "booking-uuid") {
    id
    status
  }
}
```

#### 4. Начать оплату

```graphql
mutation {
  startPayment(input: {
    bookingId: "booking-uuid"
    provider: CARD
    type: INSTANT
    amount: "100.00"
    currency: "USD"
    payloadJson: "{\"cardNumber\":\"4111111111111111\",\"cvv\":\"123\"}"
  }) {
    id
    bookingId
    provider
    type
    status
    amount
    currency
  }
}
```

#### 5. Получить свои бронирования

```graphql
query {
  myBookings {
    id
    resourceId
    startTime
    endTime
    status
  }
}
```

#### 6. Получить платежи по бронированию

```graphql
query {
  paymentsByBooking(bookingId: "booking-uuid") {
    id
    provider
    type
    status
    amount
    currency
  }
}
```

### Полная документация API

Полная документация API доступна в GraphQL схеме (`src/main/resources/graphql/schema.graphqls`). 
Вы также можете использовать GraphiQL UI (`http://localhost:8080/graphiql`) для интерактивного изучения API.

## Postman Collection

Для удобного тестирования API создана Postman коллекция: `Resource-Booking.postman_collection.json`

Импортируйте коллекцию в Postman и используйте переменные окружения:
- `baseUrl`: `http://localhost:8080`
- `adminEmail`: `admin@test.com`
- `adminPassword`: `admin`
- `userEmail`: `user@test.com`
- `userPassword`: `user`

## Тестирование

Запуск всех тестов:
```bash
./mvnw test
```

Запуск конкретного теста:
```bash
./mvnw test -Dtest=BookingServiceTest
```

## Структура проекта

```
src/
├── main/
│   ├── java/com/ramil/booking/resource_booking/
│   │   ├── api/graphql/          # GraphQL контроллеры
│   │   ├── config/                # Конфигурация (Security, Audit)
│   │   ├── domain/
│   │   │   ├── booking/          # Домен бронирований
│   │   │   ├── payment/          # Домен платежей
│   │   │   ├── resource/         # Домен ресурсов
│   │   │   └── user/              # Домен пользователей
│   │   └── ResourceBookingApplication.java
│   └── resources/
│       ├── db/migration/         # Flyway миграции
│       ├── graphql/
│       │   └── schema.graphqls    # GraphQL схема
│       └── application.properties # Конфигурация приложения
└── test/
    └── java/                      # Юнит-тесты
```

## Особенности реализации

### ACID для платежей

Все операции с платежами выполняются в транзакциях (`@Transactional`):
- Создание платежа и изменение статуса бронирования атомарны
- При ошибке все изменения откатываются
- Используется блокировка ресурсов для предотвращения конфликтов

### Проверка конфликтов бронирования

Система использует PostgreSQL EXCLUDE constraint для предотвращения пересечения бронирований:
- Проверка выполняется на уровне базы данных
- Учитываются только активные бронирования (WAITING_PAYMENT, CONFIRMED)
- Черновики (DRAFT) не учитываются при проверке конфликтов

### Обработка null в DTO

Все DTO используют Java records с явной проверкой на null через `Objects.requireNonNull()` в сервисных слоях.

### Аудит

Все сущности наследуются от `AuditableEntity`, который автоматически заполняет:
- `createdAt` - время создания
- `createdBy` - пользователь, создавший запись
- `updatedAt` - время последнего обновления
- `updatedBy` - пользователь, обновивший запись

Для платежей дополнительно сохраняются:
- `paidAt` - время оплаты
- `paidBy` - email пользователя, выполнившего оплату

## Разработка

### Сборка проекта

```bash
./mvnw clean package
```

### Запуск в режиме разработки

```bash
./mvnw spring-boot:run
```

### Проверка кода

```bash
./mvnw checkstyle:check
```

## Лицензия

Этот проект создан в рамках учебного задания.

## Контакты

Для вопросов и предложений обращайтесь к разработчику проекта.
