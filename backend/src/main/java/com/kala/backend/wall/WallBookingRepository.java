
package com.kala.backend.wall;

import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WallBookingRepository
        extends JpaRepository<WallBooking, Long> {

    List<WallBooking> findByCustomerIdOrderByBookingDateDesc(
            Long customerId);

    List<WallBooking> findAllByOrderByBookingDateAsc();

    boolean existsByBookingDateAndStatusIn(LocalDate bookingDate, List<WallBooking.Status> statuses);

    List<WallBooking> findByBookingDateBetweenAndStatusInOrderByBookingDateAsc(
            LocalDate from, LocalDate to, List<WallBooking.Status> statuses);
}
