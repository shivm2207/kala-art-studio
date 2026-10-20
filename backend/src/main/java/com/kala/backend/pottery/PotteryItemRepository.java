
package com.kala.backend.pottery;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PotteryItemRepository
        extends JpaRepository<PotteryItem, String> {
}
