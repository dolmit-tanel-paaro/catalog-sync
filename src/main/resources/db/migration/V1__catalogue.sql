create table categories (
    id           bigserial primary key,
    external_id  varchar(64)  not null unique,
    name         varchar(255) not null,
    placeholder  boolean      not null default false,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now()
);

create table products (
    id           bigserial primary key,
    external_id  varchar(64)   not null unique,
    name         varchar(255)  not null,
    price        numeric(12,2),
    category_id  bigint        not null references categories (id),
    created_at   timestamptz   not null default now(),
    updated_at   timestamptz   not null default now()
);

create index products_category_idx on products (category_id);

-- Raw payloads that referenced something which did not exist yet. Kept even
-- after resolution: it is the audit trail for why a placeholder was created.
create table staged_messages (
    id             bigserial primary key,
    message_id     varchar(64)  not null,
    external_id    varchar(64)  not null,
    unresolved_key varchar(64)  not null,
    payload        text         not null,
    status         varchar(16)  not null,
    received_at    timestamptz  not null default now(),
    resolved_at    timestamptz
);

create index staged_messages_unresolved_idx on staged_messages (unresolved_key, status);

-- Redelivery guard. The broker promises at-least-once, so the consumer has to
-- supply the "at most once" half itself.
create table processed_messages (
    message_id   varchar(64) primary key,
    processed_at timestamptz not null default now()
);
