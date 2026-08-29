-- Enforce active booking uniqueness at database layer (Defense-in-depth)
CREATE UNIQUE INDEX uq_active_booking_show_seat
ON bookings(show_id, seat_id)
WHERE status IN ('CREATED', 'CONFIRMED');
