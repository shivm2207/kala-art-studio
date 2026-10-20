
package com.kala.backend.paintings;

import com.kala.backend.user.User;
import com.kala.backend.user.UserRepository;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaintingOrderController {

    private final PaintingRepository paintings;
    private final PaintingOrderRepository orders;
    private final UserRepository users;
    private final Clock clock;

    public PaintingOrderController(
            PaintingRepository paintings,
            PaintingOrderRepository orders,
            UserRepository users,
            Clock clock) {
        this.paintings = paintings;
        this.orders = orders;
        this.users = users;
        this.clock = clock;
    }

    @PostMapping("/api/paintings/{paintingId}/orders")
    @Transactional
    public ResponseEntity<?> placeOrder(
            @PathVariable Long paintingId,
            Authentication authentication) {

        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        Painting painting = paintings.findByIdForUpdate(paintingId)
                .orElse(null);

        if (painting == null) {
            return error(HttpStatus.NOT_FOUND,
                    "Painting not found.");
        }

        if (painting.getStatus() != Painting.Status.AVAILABLE) {
            return error(HttpStatus.CONFLICT,
                    "This painting is no longer available.");
        }

        PaintingOrder order = new PaintingOrder(
                painting.getId(),
                customer.getId(),
                painting.getPrice());

        painting.updateDetails(
                painting.getTitle(),
                painting.getArtist(),
                painting.getDescription(),
                painting.getPrice(),
                painting.getImageUrl(),
                Painting.Status.SOLD);

        orders.saveAndFlush(order);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderResponse(order));
    }

    @GetMapping("/api/orders/me")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myOrders(
            Authentication authentication) {

        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        return orders.findByCustomerIdOrderByIdDesc(customer.getId())
                .stream()
                .map(this::orderResponse)
                .toList();
    }

    @PostMapping("/api/orders/{orderId}/cancel")
    @Transactional
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        PaintingOrder order = orders.findByIdForUpdate(orderId)
                .orElse(null);

        if (order == null) {
            return error(HttpStatus.NOT_FOUND, "Order not found.");
        }

        if (!order.getCustomerId().equals(customer.getId())) {
            return error(HttpStatus.NOT_FOUND, "Order not found.");
        }

        if (order.getStatus() != PaintingOrder.Status.PLACED) {
            return error(HttpStatus.CONFLICT,
                    "This order has already been cancelled.");
        }

        Painting painting = paintings.findByIdForUpdate(
                order.getPaintingId()).orElse(null);

        if (painting == null) {
            return error(HttpStatus.CONFLICT,
                    "The painting associated with this order no longer exists.");
        }

        order.cancel(clock.instant()
                .atZone(clock.getZone())
                .toOffsetDateTime());

        painting.updateDetails(
                painting.getTitle(),
                painting.getArtist(),
                painting.getDescription(),
                painting.getPrice(),
                painting.getImageUrl(),
                Painting.Status.AVAILABLE);

        return ResponseEntity.ok(orderResponse(order));
    }


    @GetMapping("/api/admin/orders")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> allOrders() {
        return orders.findAll().stream().map(order -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", order.getId());
            result.put("customerId", order.getCustomerId());
            result.put("customerEmail", users.findById(order.getCustomerId())
                    .map(User::getEmail).orElse("Unknown"));
            result.put("paintingId", order.getPaintingId());
            result.put("paintingTitle", paintings.findById(order.getPaintingId())
                    .map(Painting::getTitle).orElse("Painting no longer exists"));
            result.put("status", order.getStatus());
            result.put("priceAtPurchase", order.getPriceAtPurchase());
            result.put("createdAt", order.getCreatedAt());
            return result;
        }).toList();
    }

    private Map<String, Object> orderResponse(PaintingOrder order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("paintingId", order.getPaintingId());
        result.put("customerId", order.getCustomerId());
        result.put("status", order.getStatus());
        result.put("priceAtPurchase", order.getPriceAtPurchase());
        result.put("createdAt", order.getCreatedAt());
        result.put("cancelledAt", order.getCancelledAt());
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
}
