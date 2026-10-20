
package com.kala.backend.pottery;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PotteryRequestRepository
        extends JpaRepository<PotteryRequest, Long> {

    List<PotteryRequest> findByCustomerIdOrderByIdDesc(Long customerId);

    List<PotteryRequest> findAllByOrderByIdDesc();
}
