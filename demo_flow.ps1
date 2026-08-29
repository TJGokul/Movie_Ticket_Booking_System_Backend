# 1. Create a Movie Show
Write-Host "`n=== 1. CREATING SHOW ===" -ForegroundColor Cyan
$showPayload = @{
    movieName = "Oppenheimer IMAX"
    showTime = "2026-10-31T18:30:00Z"
    totalSeats = 10
    pricePerSeat = 300.00
} | ConvertTo-Json

$show = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/shows" -Method Post -ContentType "application/json" -Body $showPayload
$show | Format-List showId, movieName, totalSeats, pricePerSeat

# 2. Check Available Seats
Write-Host "`n=== 2. CHECKING AVAILABLE SEATS ===" -ForegroundColor Cyan
$seats = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/shows/$($show.showId)/available-seats"
$seats | Format-Table seatId, seatNumber, isBooked

$targetSeat = $seats[0]
Write-Host "Targeting Seat: $($targetSeat.seatNumber) (ID: $($targetSeat.seatId))" -ForegroundColor Yellow

# 3. Book the Seat (Success)
Write-Host "`n=== 3. BOOKING SEAT $($targetSeat.seatNumber) ===" -ForegroundColor Cyan
$bookingPayload = @{
    showId = $show.showId
    seatId = $targetSeat.seatId
    customerId = "CUST-POWERSHELL-1"
} | ConvertTo-Json

$booking = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/bookings" -Method Post -ContentType "application/json" -Body $bookingPayload
$booking | Format-List bookingId, movieName, seatNumber, customerId, status, amountPaid

# 4. Attempt Double-Booking the SAME Seat (Should Fail with 409 Conflict)
Write-Host "`n=== 4. ATTEMPTING CONFLICT / DOUBLE-BOOKING ON SEAT $($targetSeat.seatNumber) ===" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/v1/bookings" -Method Post -ContentType "application/json" -Body (@{
        showId = $show.showId
        seatId = $targetSeat.seatId
        customerId = "CUST-ANOTHER-USER"
    } | ConvertTo-Json)
} catch {
    Write-Host "Expected Conflict Caught (HTTP 409):" -ForegroundColor Red
    $_.ErrorDetails.Message
}

# 5. Cancel Booking & Issue Full Refund
Write-Host "`n=== 5. CANCELLING BOOKING $($booking.bookingId) ===" -ForegroundColor Cyan
$refund = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/bookings/$($booking.bookingId)/cancel" -Method Post
$refund | Format-List bookingId, seatNumber, status, refundAmount, message

# 6. Verify Seat is Available Again
Write-Host "`n=== 6. VERIFYING SEAT IS RELEASED ===" -ForegroundColor Cyan
$availableSeatsAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/shows/$($show.showId)/available-seats"
$availableSeatsAfter | Format-Table seatId, seatNumber, isBooked
