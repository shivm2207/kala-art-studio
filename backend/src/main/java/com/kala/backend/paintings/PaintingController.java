package com.kala.backend.paintings;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaintingController {
    private final PaintingRepository paintings;

    public PaintingController(PaintingRepository paintings) {
        this.paintings = paintings;
    }

    @GetMapping("/api/paintings")
    public List<PaintingResponse> listAvailable() {
        return paintings.findByStatusOrderByIdDesc(Painting.Status.AVAILABLE)
                .stream().map(PaintingResponse::from).toList();
    }

    @GetMapping("/api/paintings/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return paintings.findById(id)
                .filter(p -> p.getStatus() == Painting.Status.AVAILABLE)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(PaintingResponse.from(p)))
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Painting not found."));
    }

    @GetMapping("/api/admin/paintings")
    public List<PaintingResponse> listAll() {
        return paintings.findAll().stream().map(PaintingResponse::from).toList();
    }

    @PostMapping("/api/admin/paintings")
    public ResponseEntity<?> create(@Valid @RequestBody PaintingRequest request) {
        Painting painting = Painting.create(request.title().trim(), request.artist().trim(),
                request.description().trim(), request.price(), request.imageUrl());
        // New paintings always start AVAILABLE; status is never accepted from the client.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaintingResponse.from(paintings.save(painting)));
    }

    @PutMapping("/api/admin/paintings/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id,
            @Valid @RequestBody PaintingRequest request) {
        Painting painting = paintings.findByIdForUpdate(id).orElse(null);
        if (painting == null) {
            return error(HttpStatus.NOT_FOUND, "Painting not found.");
        }
        if (painting.getStatus() != Painting.Status.AVAILABLE) {
            return error(HttpStatus.CONFLICT, "Only available paintings can be edited.");
        }
        painting.updateDetails(request.title().trim(), request.artist().trim(),
                request.description().trim(), request.price(), request.imageUrl(),
                Painting.Status.AVAILABLE);
        return ResponseEntity.ok(PaintingResponse.from(painting));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
