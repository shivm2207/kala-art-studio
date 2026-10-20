
package com.kala.backend.wall;

import com.kala.backend.user.User;
import com.kala.backend.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
public class WallBookingController {

    private final WallBookingRepository bookings;
    private final MuralRateRepository rates;
    private final UserRepository users;
    private final Clock clock;

    public WallBookingController(
            WallBookingRepository bookings,
            MuralRateRepository rates,
            UserRepository users,
            Clock clock) {
        this.bookings = bookings;
        this.rates = rates;
        this.users = users;
        this.clock = clock;
    }

    @PostMapping("/api/wall-bookings")
    public ResponseEntity<?> create(
            @Valid @RequestBody WallBookingRequest request,
            Authentication authentication) {

        LocalDate today = LocalDate.now(clock);

        if (request.bookingDate().isBefore(today.plusDays(7)) || request.bookingDate().isAfter(today.plusDays(90))) {
             return error(HttpStatus.BAD_REQUEST,
            "Booking dates must be between 7 and 90 days from today.");
        }

        if (bookings.existsByBookingDateAndStatusIn(request.bookingDate(),
                List.of(WallBooking.Status.REQUESTED, WallBooking.Status.CONFIRMED))) {
            return error(HttpStatus.CONFLICT,
                    "That date is already booked. Please choose another date.");
        }

        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        MuralRate rate = rates.findById((short) 1).orElse(null);
        if (rate == null) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "Wall booking prices have not been configured yet.");
        }

        BigDecimal size = request.wallSizeSqft()
                .setScale(2, RoundingMode.UNNECESSARY);

        BigDecimal total = size.multiply(rate.getRatePerSqft())
                .setScale(2, RoundingMode.HALF_UP);

        WallBooking booking = new WallBooking(
                customer.getId(),
                request.bookingDate(),
                size,
                rate.getRatePerSqft(),
                total,
                request.serviceAddress().trim());

        try {
            // Repository save runs in its own transaction; the unique-index error is caught after rollback.
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toMap(bookings.saveAndFlush(booking)));
        } catch (DataIntegrityViolationException ex) {
            return error(HttpStatus.CONFLICT,
                    "That date is already booked. Please choose another date.");
        }
    }

    @GetMapping("/api/wall-bookings/me")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine(
            Authentication authentication) {
        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        return bookings.findByCustomerIdOrderByBookingDateDesc(
                        customer.getId())
                .stream().map(this::toMap).toList();
    }

    @PostMapping("/api/wall-bookings/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(
            @PathVariable Long id,
            Authentication authentication) {
        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        WallBooking booking = bookings.findById(id).orElse(null);
        if (booking == null) {
            return error(HttpStatus.NOT_FOUND, "Booking not found.");
        }
        if (!booking.getCustomerId().equals(customer.getId())) {
            return error(HttpStatus.NOT_FOUND, "Booking not found.");
        }
        if (booking.getStatus() != WallBooking.Status.REQUESTED
        && booking.getStatus() != WallBooking.Status.CONFIRMED) {
            return error(HttpStatus.CONFLICT,
            "This booking cannot be cancelled.");
        }

        if (booking.getBookingDate().isBefore(LocalDate.now(clock).plusDays(3))) {
             return error(HttpStatus.CONFLICT,
            "Bookings can only be cancelled at least 3 days before the booking date.");
        }

        booking.changeStatus(WallBooking.Status.CANCELLED);
        return ResponseEntity.ok(toMap(booking));
    }

    @GetMapping("/api/bookings/availability")
    public ResponseEntity<?> availability(@RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        if (to.isBefore(from)) {
            return error(HttpStatus.BAD_REQUEST, "The end date must be on or after the start date.");
        }
        List<LocalDate> dates = bookings
                .findByBookingDateBetweenAndStatusInOrderByBookingDateAsc(from, to,
                        List.of(WallBooking.Status.REQUESTED, WallBooking.Status.CONFIRMED))
                .stream().map(WallBooking::getBookingDate).distinct().toList();
        return ResponseEntity.ok(Map.of("dates", dates));
    }

    @GetMapping("/api/admin/wall-bookings")
    public List<Map<String, Object>> allBookings() {
        return bookings.findAllByOrderByBookingDateAsc()
                .stream().map(this::toMap).toList();
    }

    @PostMapping("/api/admin/wall-bookings/{id}/decision")
    @Transactional
    public ResponseEntity<?> decide(
            @PathVariable Long id,
            @RequestBody DecisionRequest request) {
        WallBooking booking = bookings.findById(id).orElse(null);
        if (booking == null) {
            return error(HttpStatus.NOT_FOUND, "Booking not found.");
        }
        if (booking.getStatus() != WallBooking.Status.REQUESTED) {
            return error(HttpStatus.CONFLICT,
                    "Only requested bookings can be decided.");
        }
        if (!"CONFIRMED".equals(request.status())
                && !"DECLINED".equals(request.status())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Status must be CONFIRMED or DECLINED.");
        }

        booking.changeStatus(
                WallBooking.Status.valueOf(request.status()));
        return ResponseEntity.ok(toMap(booking));
    }

    @PutMapping("/api/admin/wall-bookings/rate")
    @Transactional
    public ResponseEntity<?> setRate(@Valid @RequestBody RateRequest request) {
        MuralRate rate = rates.findById((short) 1)
                .orElseGet(() -> new MuralRate(request.ratePerSqft()));

        rate.changeRate(request.ratePerSqft());
        return ResponseEntity.ok(rates.save(rate));
    }

    private Map<String, Object> toMap(WallBooking booking) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", booking.getId());
        result.put("customerId", booking.getCustomerId());
        result.put("bookingDate", booking.getBookingDate());
        result.put("wallSizeSqft", booking.getWallSizeSqft());
        result.put("rateApplied", booking.getRateApplied());
        result.put("totalPrice", booking.getTotalPrice());
        result.put("serviceAddress", booking.getServiceAddress());
        result.put("status", booking.getStatus());
        result.put("createdAt", booking.getCreatedAt());
        return result;
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    public record DecisionRequest(String status) {}

    public record RateRequest(
    @DecimalMin(value = "1.00", message = "Rate must be at least ₹1 per sq ft")
    @DecimalMax(value = "1000.00", message = "Rate cannot exceed ₹1,000 per sq ft")
    BigDecimal ratePerSqft
) {}
}
