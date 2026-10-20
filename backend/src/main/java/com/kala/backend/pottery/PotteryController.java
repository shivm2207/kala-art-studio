
package com.kala.backend.pottery;

import com.kala.backend.user.User;
import com.kala.backend.user.UserRepository;

import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class PotteryController {

    private final PotteryItemRepository items;
    private final GlazeRepository glazes;
    private final PotteryRequestRepository requests;
    private final UserRepository users;
    private final Clock clock;

    public PotteryController(
            PotteryItemRepository items,
            GlazeRepository glazes,
            PotteryRequestRepository requests,
            UserRepository users,
            Clock clock) {
        this.items = items;
        this.glazes = glazes;
        this.requests = requests;
        this.users = users;
        this.clock = clock;
    }

    @GetMapping("/api/pottery/options")
    public Map<String, Object> options() {
        return Map.of(
                "items", items.findAll(),
                "glazes", glazes.findAll()
        );
    }

    @PostMapping("/api/pottery/requests")
    @Transactional
    public ResponseEntity<?> create(
            @Valid @RequestBody PotteryRequestInput input,
            Authentication authentication) {

        if (input.neededBy().isBefore(LocalDate.now(clock).plusDays(14))) {
            return error(HttpStatus.BAD_REQUEST,
            "The required date must be at least 14 days from today.");
        }

        if (!items.existsById(input.itemId())
                || !glazes.existsById(input.glazeId())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Select a valid pottery item and glaze.");
        }

        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        PotteryRequest request = new PotteryRequest(
                customer.getId(),
                input.itemId(),
                input.glazeId(),
                input.quantity(),
                input.neededBy());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toMap(requests.save(request)));
    }

    @GetMapping("/api/pottery/requests/me")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine(
            Authentication authentication) {

        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        return requests.findByCustomerIdOrderByIdDesc(customer.getId())
                .stream().map(this::toMap).toList();
    }

    @PostMapping("/api/pottery/requests/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(
            @PathVariable Long id,
            Authentication authentication) {

        User customer = users.findByEmail(authentication.getName())
                .orElseThrow();

        PotteryRequest request = requests.findById(id).orElse(null);

        if (request == null) {
            return error(HttpStatus.NOT_FOUND, "Request not found.");
        }

        if (!request.getCustomerId().equals(customer.getId())) {
            return error(HttpStatus.NOT_FOUND, "Request not found.");
        }

        if (request.getStatus() != PotteryRequest.Status.REQUESTED) {
            return error(HttpStatus.CONFLICT,
                    "Only pending requests can be cancelled.");
        }

        request.changeStatus(PotteryRequest.Status.CANCELLED);
        return ResponseEntity.ok(toMap(request));
    }

    @PutMapping("/api/pottery/requests/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id,
            @Valid @RequestBody PotteryRequestInput input,
            Authentication authentication) {
        User customer = users.findByEmail(authentication.getName()).orElseThrow();
        PotteryRequest request = requests.findById(id).orElse(null);
        if (request == null || !request.getCustomerId().equals(customer.getId())) {
            return error(HttpStatus.NOT_FOUND, "Request not found.");
        }
        if (request.getStatus() != PotteryRequest.Status.REQUESTED) {
            return error(HttpStatus.CONFLICT, "Only pending requests can be edited.");
        }
        if (input.neededBy().isBefore(LocalDate.now(clock).plusDays(14))) {
            return error(HttpStatus.BAD_REQUEST,
                    "The required date must be at least 14 days from today.");
        }
        if (!items.existsById(input.itemId()) || !glazes.existsById(input.glazeId())) {
            return error(HttpStatus.BAD_REQUEST, "Select a valid pottery item and glaze.");
        }
        request.updateDetails(input.itemId(), input.glazeId(), input.quantity(), input.neededBy());
        return ResponseEntity.ok(toMap(request));
    }

    @GetMapping("/api/admin/pottery/requests")
    public List<Map<String, Object>> allRequests() {
        return requests.findAllByOrderByIdDesc()
                .stream().map(this::toMap).toList();
    }

    @PostMapping("/api/admin/pottery/requests/{id}/decision")
    @Transactional
    public ResponseEntity<?> decide(
            @PathVariable Long id,
            @RequestBody DecisionInput input) {

        PotteryRequest request = requests.findById(id).orElse(null);

        if (request == null) {
            return error(HttpStatus.NOT_FOUND, "Request not found.");
        }

        if (request.getStatus() != PotteryRequest.Status.REQUESTED) {
            return error(HttpStatus.CONFLICT,
                    "Only pending requests can be decided.");
        }

        if (!"ACCEPTED".equals(input.status())
                && !"DECLINED".equals(input.status())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Status must be ACCEPTED or DECLINED.");
        }

        request.changeStatus(PotteryRequest.Status.valueOf(input.status()));
        return ResponseEntity.ok(toMap(request));
    }

    @PostMapping("/api/admin/pottery/items")
    public ResponseEntity<?> addItem(@RequestBody OptionInput input) {
        String id = clean(input.id());
        String name = clean(input.name());

        if (id == null || id.length() > 10
                || name == null || name.length() > 100) {
            return error(HttpStatus.BAD_REQUEST,
                    "Provide an ID (max 10 characters) and a name (max 100 characters).");
        }

        if (items.existsById(id)) {
            return error(HttpStatus.CONFLICT, "That item ID already exists.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(items.save(new PotteryItem(id, name)));
    }

    @PostMapping("/api/admin/pottery/glazes")
    public ResponseEntity<?> addGlaze(@RequestBody OptionInput input) {
        String id = clean(input.id());
        String name = clean(input.name());

        if (id == null || id.length() > 10
                || name == null || name.length() > 100) {
            return error(HttpStatus.BAD_REQUEST,
                    "Provide an ID (max 10 characters) and a name (max 100 characters).");
        }

        if (glazes.existsById(id)) {
            return error(HttpStatus.CONFLICT, "That glaze ID already exists.");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(glazes.save(new Glaze(id, name)));
    }

    private Map<String, Object> toMap(PotteryRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", request.getId());
        result.put("customerId", request.getCustomerId());
        result.put("itemId", request.getItemId());
        result.put("glazeId", request.getGlazeId());
        result.put("quantity", request.getQuantity());
        result.put("neededBy", request.getNeededBy());
        result.put("status", request.getStatus());
        result.put("createdAt", request.getCreatedAt());
        return result;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    public record DecisionInput(String status) {}
    public record OptionInput(String id, String name) {}
}
