package com.kala.backend.paintings;

import java.math.BigDecimal;

public record PaintingResponse(Long id, String title, String artist,
        String description, BigDecimal price, String imageUrl,
        Painting.Status status) {
    public static PaintingResponse from(Painting painting) {
        return new PaintingResponse(painting.getId(), painting.getTitle(),
                painting.getArtist(), painting.getDescription(), painting.getPrice(),
                painting.getImageUrl(), painting.getStatus());
    }
}
