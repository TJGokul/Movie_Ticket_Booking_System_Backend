CREATE TABLE seats (
    seat_id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    is_booked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_seats_show FOREIGN KEY (show_id) REFERENCES shows(show_id) ON DELETE CASCADE,
    CONSTRAINT uq_show_seat_number UNIQUE (show_id, seat_number)
);

CREATE INDEX idx_seats_show_is_booked ON seats(show_id, is_booked);
