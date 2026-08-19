create table subscriptions (
    id bigserial primary key,
    user_id bigint not null check (user_id > 0),
    type varchar(16) not null check (type in ('BASIC', 'PRO')),
    activation_date date not null,
    deactivation_date date
);

create unique index uk_subscriptions_one_active_user
    on subscriptions (user_id) where deactivation_date is null;
create index idx_subscriptions_active_activation on subscriptions (activation_date) where deactivation_date is null;

create table invoices (
    id bigserial primary key,
    subscription_id bigint not null references subscriptions(id),
    user_id bigint not null,
    billing_date date not null,
    activation_date date not null,
    subscription_title varchar(32) not null,
    price_rubles integer not null check (price_rubles > 0),
    constraint uk_invoice_subscription_billing_date unique (subscription_id, billing_date)
);
create index idx_invoices_user_billing_date on invoices (user_id, billing_date desc);

create table outbox_events (
    id uuid primary key,
    type varchar(64) not null,
    payload text not null,
    created_at timestamp with time zone not null,
    available_at timestamp with time zone not null,
    published_at timestamp with time zone,
    locked_by varchar(64),
    locked_until timestamp with time zone
);
create index idx_outbox_unpublished_lease
    on outbox_events (available_at, locked_until, created_at) where published_at is null;

create sequence subscription_event_sequence;
