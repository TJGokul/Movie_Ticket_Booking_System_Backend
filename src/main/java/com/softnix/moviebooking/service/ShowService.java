package com.softnix.moviebooking.service;

import com.softnix.moviebooking.dto.request.CreateShowRequest;
import com.softnix.moviebooking.dto.response.SeatResponse;
import com.softnix.moviebooking.dto.response.ShowResponse;
import com.softnix.moviebooking.entity.Seat;
import com.softnix.moviebooking.entity.Show;
import com.softnix.moviebooking.exception.ShowNotFoundException;
import com.softnix.moviebooking.repository.SeatRepository;
import com.softnix.moviebooking.repository.ShowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShowService {

    private static final Logger log = LoggerFactory.getLogger(ShowService.class);

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowService(ShowRepository showRepository, SeatRepository seatRepository) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {
        log.info("Creating new show for movie: '{}', showTime: {}, seats: {}, price: {}",
                request.movieName(), request.showTime(), request.totalSeats(), request.pricePerSeat());

        Show show = new Show();
        show.setMovieName(request.movieName().trim());
        show.setShowTime(request.showTime());
        show.setTotalSeats(request.totalSeats());
        show.setPricePerSeat(request.pricePerSeat());

        // Generate seats deterministically (A1..A10, B1..B10, etc.)
        List<Seat> generatedSeats = generateSeatsForShow(show, request.totalSeats());
        for (Seat seat : generatedSeats) {
            show.addSeat(seat);
        }

        Show savedShow = showRepository.save(show);
        log.info("Show created successfully with ID: {} and {} seats.", savedShow.getShowId(), savedShow.getSeats().size());

        List<SeatResponse> seatResponses = savedShow.getSeats().stream()
                .map(this::mapToSeatResponse)
                .toList();

        return new ShowResponse(
                savedShow.getShowId(),
                savedShow.getMovieName(),
                savedShow.getShowTime(),
                savedShow.getTotalSeats(),
                (long) savedShow.getTotalSeats(),
                savedShow.getPricePerSeat(),
                savedShow.getCreatedAt(),
                seatResponses
        );
    }

    public ShowResponse getShowById(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ShowNotFoundException(showId));

        long availableCount = seatRepository.countAvailableSeatsByShowId(showId);

        return new ShowResponse(
                show.getShowId(),
                show.getMovieName(),
                show.getShowTime(),
                show.getTotalSeats(),
                availableCount,
                show.getPricePerSeat(),
                show.getCreatedAt(),
                null
        );
    }

    public List<ShowResponse> getAllShows() {
        return showRepository.findAll().stream()
                .map(show -> {
                    long availableCount = seatRepository.countAvailableSeatsByShowId(show.getShowId());
                    return new ShowResponse(
                            show.getShowId(),
                            show.getMovieName(),
                            show.getShowTime(),
                            show.getTotalSeats(),
                            availableCount,
                            show.getPricePerSeat(),
                            show.getCreatedAt(),
                            null
                    );
                })
                .toList();
    }

    public List<SeatResponse> getAvailableSeats(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ShowNotFoundException(showId);
        }

        List<Seat> availableSeats = seatRepository.findAvailableSeatsByShowId(showId);
        log.debug("Found {} available seats for show {}", availableSeats.size(), showId);

        return availableSeats.stream()
                .map(this::mapToSeatResponse)
                .toList();
    }

    public List<SeatResponse> getAllSeatsForShow(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ShowNotFoundException(showId);
        }

        return seatRepository.findAllByShowId(showId).stream()
                .map(this::mapToSeatResponse)
                .toList();
    }

    private List<Seat> generateSeatsForShow(Show show, int totalSeats) {
        List<Seat> seats = new ArrayList<>(totalSeats);
        int seatsPerRow = 10;
        for (int i = 0; i < totalSeats; i++) {
            char rowChar = (char) ('A' + (i / seatsPerRow));
            int seatNumInRow = (i % seatsPerRow) + 1;
            String seatNumber = "" + rowChar + seatNumInRow;

            Seat seat = new Seat(show, seatNumber, false);
            seats.add(seat);
        }
        return seats;
    }

    private SeatResponse mapToSeatResponse(Seat seat) {
        Long showId = seat.getShow() != null ? seat.getShow().getShowId() : null;
        return new SeatResponse(
                seat.getSeatId(),
                showId,
                seat.getSeatNumber(),
                seat.getIsBooked()
        );
    }
}
