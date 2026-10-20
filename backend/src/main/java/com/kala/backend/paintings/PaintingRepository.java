
package com.kala.backend.paintings;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaintingRepository
        extends JpaRepository<Painting, Long> {
    
    boolean existsBySeedId(String seedId);

    List<Painting> findByStatusOrderByIdDesc(
            Painting.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Painting p where p.id = :id")
    Optional<Painting> findByIdForUpdate(@Param("id") Long id);
}
