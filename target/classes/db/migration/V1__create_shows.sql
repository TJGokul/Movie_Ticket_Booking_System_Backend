CREATE TABLE shows (
    show_id BIGSERIAL PRIMARY KEY,
    movie_name VARCHAR(255) NOT NULL,
    show_time TIMESTAMP WITH TIME ZONE NOT NULL,
    total_seats INT NOT NULL,
    price_per_seat NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_shows_show_time ON shows(show_time);
