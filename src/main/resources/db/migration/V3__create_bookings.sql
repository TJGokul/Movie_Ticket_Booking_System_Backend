CREATE TABLE bookings (
    booking_id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount_paid NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_bookings_show FOREIGN KEY (show_id) REFERENCES shows(show_id),
    CONSTRAINT fk_bookings_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id)
);

CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_show_seat ON bookings(show_id, seat_id);
