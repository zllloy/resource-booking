-- USERS
create table app_user (
  id uuid primary key,
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  role varchar(32) not null, -- ADMIN / USER

  created_at timestamptz not null,
  created_by varchar(255) not null,
  updated_at timestamptz not null,
  updated_by varchar(255) not null
);

-- RESOURCES
create table resource (
  id uuid primary key,
  name varchar(120) not null,
  description text,
  active boolean not null default true,

  created_at timestamptz not null,
  created_by varchar(255) not null,
  updated_at timestamptz not null,
  updated_by varchar(255) not null
);

create index idx_resource_active on resource(active);

-- BOOKINGS
create table booking (
  id uuid primary key,

  user_id uuid not null references app_user(id),
  resource_id uuid not null references resource(id),

  start_time timestamptz not null,
  end_time timestamptz not null,

  status varchar(32) not null, -- DRAFT / WAITING_PAYMENT / CONFIRMED / CANCELED / EXPIRED

  created_at timestamptz not null,
  created_by varchar(255) not null,
  updated_at timestamptz not null,
  updated_by varchar(255) not null,

  constraint chk_booking_time_range check (end_time > start_time)
);

create index idx_booking_resource_time on booking(resource_id, start_time, end_time);
create index idx_booking_user on booking(user_id);

-- PAYMENTS
create table payment (
  id uuid primary key,

  booking_id uuid not null references booking(id),

  provider varchar(32) not null,     -- PAYPAL / CARD
  type varchar(32) not null,         -- INSTANT / DEFERRED
  status varchar(32) not null,       -- NEW / SUCCESS / FAILED / CANCELED

  amount numeric(12,2) not null,
  currency varchar(3) not null,

  provider_payload jsonb,

  created_at timestamptz not null,
  created_by varchar(255) not null,
  updated_at timestamptz not null,
  updated_by varchar(255) not null
);

create index idx_payment_booking on payment(booking_id);
