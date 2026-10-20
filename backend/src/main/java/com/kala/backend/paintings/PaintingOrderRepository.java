
package com.kala.backend.paintings;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaintingOrderRepository
        extends JpaRepository<PaintingOrder, Long> {

    List<PaintingOrder> findByCustomerIdOrderByIdDesc(
            Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PaintingOrder o where o.id = :id")
    Optional<PaintingOrder> findByIdForUpdate(@Param("id") Long id);
    boolean existsByPaintingId(Long paintingId);
}
