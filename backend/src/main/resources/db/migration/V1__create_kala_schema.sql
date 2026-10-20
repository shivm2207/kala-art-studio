
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  role VARCHAR(10) NOT NULL CHECK (role IN ('CUSTOMER','ADMIN')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE paintings (
  id BIGSERIAL PRIMARY KEY,
  seed_id VARCHAR(20) UNIQUE,
  title VARCHAR(200) NOT NULL,
  artist VARCHAR(120) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  price NUMERIC(12,2) NOT NULL CHECK (price > 0),
  image_url VARCHAR(255),
  status VARCHAR(10) NOT NULL CHECK (status IN ('AVAILABLE','SOLD')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE painting_orders (
  id BIGSERIAL PRIMARY KEY,
  painting_id BIGINT NOT NULL REFERENCES paintings(id),
  customer_id BIGINT NOT NULL REFERENCES users(id),
  status VARCHAR(10) NOT NULL CHECK (status IN ('PLACED','CANCELLED')),
  price_at_purchase NUMERIC(12,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  cancelled_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_one_placed_order_per_painting
  ON painting_orders (painting_id) WHERE status = 'PLACED';

CREATE INDEX idx_orders_customer ON painting_orders (customer_id);

CREATE TABLE mural_rate (
  id SMALLINT PRIMARY KEY CHECK (id = 1),
  rate_per_sqft NUMERIC(10,2) NOT NULL CHECK (rate_per_sqft > 0)
);

CREATE TABLE wall_bookings (
  id BIGSERIAL PRIMARY KEY,
  customer_id BIGINT NOT NULL REFERENCES users(id),
  booking_date DATE NOT NULL,
  wall_size_sqft NUMERIC(7,2) NOT NULL CHECK (wall_size_sqft BETWEEN 10 AND 500),
  rate_applied NUMERIC(10,2) NOT NULL,
  total_price NUMERIC(12,2) NOT NULL,
  service_address VARCHAR(500) NOT NULL,
  status VARCHAR(10) NOT NULL CHECK (status IN ('REQUESTED','CONFIRMED','DECLINED','CANCELLED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_one_active_booking_per_date
  ON wall_bookings (booking_date) WHERE status IN ('REQUESTED','CONFIRMED');

CREATE INDEX idx_bookings_customer ON wall_bookings (customer_id);

CREATE TABLE pottery_items (
  id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(100) NOT NULL
);

CREATE TABLE glazes (
  id VARCHAR(10) PRIMARY KEY,
  name VARCHAR(100) NOT NULL
);

CREATE TABLE pottery_requests (
  id BIGSERIAL PRIMARY KEY,
  customer_id BIGINT NOT NULL REFERENCES users(id),
  item_id VARCHAR(10) NOT NULL REFERENCES pottery_items(id),
  glaze_id VARCHAR(10) NOT NULL REFERENCES glazes(id),
  quantity SMALLINT NOT NULL CHECK (quantity BETWEEN 1 AND 12),
  needed_by DATE NOT NULL,
  status VARCHAR(10) NOT NULL CHECK (status IN ('REQUESTED','ACCEPTED','DECLINED','CANCELLED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_pottery_customer ON pottery_requests (customer_id);
