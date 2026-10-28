# catalog-sync

A Spring Boot service that imports a product catalogue from an ERP over a
message queue, and copes with the fact that the ERP does not promise order.

## The problem

Products reference categories. The ERP sends both as separate messages and makes
no guarantee about which arrives first, so on a full export a product usually
turns up before the category it belongs to. Three ways to handle that:

1. **Reject the product.** The ERP will resend eventually. This loses data if it
   does not, and gives nobody anything to look at in the meantime.
2. **Retry until the category appears.** Works until the category is never sent,
   at which point the queue fills with messages that will never succeed.
3. **Accept it against a placeholder.** The product is stored immediately,
   attached to a placeholder category created on the spot, and the raw payload
   is parked in `staged_messages` with the key that could not be resolved.

This service does the third.

## Why a placeholder rather than a parking lot

The important property is that the placeholder is a **real row with a real id**.
When the category finally arrives, that row is filled in rather than replaced,
so every product already pointing at it stays pointed at it. Nothing has to be
re-linked, and there is no window where a product is attached to a category that
is about to be deleted.

The second property is that a placeholder is **visible**. `GET /api/placeholders`
lists them with what is sitting underneath, and `POST /api/placeholders/move`
reassigns a product by hand. A record that cannot be resolved automatically is
not stuck in a queue nobody reads; it is an item of work somebody can see and
deal with. This matters because the "never arrives" case is real, and the only
thing that resolves it is a person.

`staged_messages` rows are kept after resolution. They are the record of why a
placeholder was created and what the payload was, which is what a replay or an
argument with the ERP team needs.

## Redelivery

RabbitMQ promises at-least-once delivery. The "at most once" half is the
consumer's job, so every applied message id is recorded in `processed_messages`
in the same transaction as the write, and a message that has already been
applied is dropped. Consumers that fail after their retries go to a dead-letter
queue rather than being requeued forever.

## Running it

    docker compose up --build

Postgres on 5432, RabbitMQ on 5672 with the management UI on 15672, the service
on 8080. Health at `/actuator/health`.

## Tests

    ./mvnw verify

Integration tests run against real Postgres and RabbitMQ containers through
Testcontainers, so no test runs against an in-memory database that behaves
differently from production. The cases covered are the ones the design exists
for: in-order arrival, a product arriving before its category, the placeholder
being promoted in place, redelivery, update rather than duplicate, and manual
reassignment.

## Stack

Java 21, Spring Boot 4.1, Spring Data JPA, Flyway, PostgreSQL, Spring AMQP,
Testcontainers, Maven.
