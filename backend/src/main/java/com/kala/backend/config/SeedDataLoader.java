
package com.kala.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kala.backend.paintings.Painting;
import com.kala.backend.paintings.PaintingRepository;
import com.kala.backend.pottery.Glaze;
import com.kala.backend.pottery.GlazeRepository;
import com.kala.backend.pottery.PotteryItem;
import com.kala.backend.pottery.PotteryItemRepository;
import com.kala.backend.user.User;
import com.kala.backend.user.UserRepository;
import com.kala.backend.wall.MuralRate;
import com.kala.backend.wall.MuralRateRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "kala.seed.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SeedDataLoader implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(SeedDataLoader.class);

    private final PaintingRepository paintings;
    private final PotteryItemRepository items;
    private final GlazeRepository glazes;
    private final MuralRateRepository rates;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;
    private final String adminEmailOverride;
    private final String adminPassword;

    public SeedDataLoader(
            PaintingRepository paintings,
            PotteryItemRepository items,
            GlazeRepository glazes,
            MuralRateRepository rates,
            UserRepository users,
            PasswordEncoder passwordEncoder,
            ObjectMapper mapper,
            @Value("${KALA_ADMIN_EMAIL:}") String adminEmailOverride,
            @Value("${KALA_ADMIN_PASSWORD:}") String adminPassword) {

        this.paintings = paintings;
        this.items = items;
        this.glazes = glazes;
        this.rates = rates;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.adminEmailOverride = adminEmailOverride;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        JsonNode seed;

        try (InputStream input =
                     new ClassPathResource("seed/seed.json")
                             .getInputStream()) {
            seed = mapper.readTree(input);
        }

        int addedPaintings = 0;

        for (JsonNode paintingData : seed.path("paintings")) {
            String seedId = paintingData.path("id").asText("").trim();

            if (seedId.isEmpty() || paintings.existsBySeedId(seedId)) {
                continue;
            }

            String statusText =
                    paintingData.path("status").asText("available");

            Painting.Status status;

            if ("available".equalsIgnoreCase(statusText)) {
                status = Painting.Status.AVAILABLE;
            } else if ("sold".equalsIgnoreCase(statusText)) {
                status = Painting.Status.SOLD;
            } else {
                throw new IllegalArgumentException(
                        "Invalid painting status in seed data: "
                                + statusText
                );
            }

            paintings.save(Painting.createSeeded(
                    seedId,
                    paintingData.path("title").asText(),
                    paintingData.path("artist").asText(),
                    paintingData.path("description").asText(),
                    paintingData.path("price").decimalValue(),
                    imagePath(
                            paintingData.path("imageUrl").asText(null)
                    ),
                    status
            ));

            addedPaintings++;
        }

        int addedItems = 0;

        for (JsonNode item : seed.path("potteryItems")) {
            String id = item.path("id").asText("").trim();

            if (!id.isEmpty() && !items.existsById(id)) {
                items.save(new PotteryItem(
                        id,
                        item.path("name").asText()
                ));
                addedItems++;
            }
        }

        int addedGlazes = 0;

        for (JsonNode glaze : seed.path("glazes")) {
            String id = glaze.path("id").asText("").trim();

            if (!id.isEmpty() && !glazes.existsById(id)) {
                glazes.save(new Glaze(
                        id,
                        glaze.path("name").asText()
                ));
                addedGlazes++;
            }
        }

        boolean addedRate = false;
        JsonNode rate = seed.path("mural").path("ratePerSqFt");

        if (!rate.isMissingNode()
                && !rate.isNull()
                && !rates.existsById((short) 1)) {

            rates.save(new MuralRate(
                    new BigDecimal(rate.asText())
            ));
            addedRate = true;
        }

        boolean addedAdmin =
                createAdminIfConfigured(seed.path("studioAccount"));

        log.info(
                "Seed data: +{} paintings, +{} pottery items, "
                        + "+{} glazes, mural rate {}, admin {}",
                addedPaintings,
                addedItems,
                addedGlazes,
                addedRate ? "added" : "kept",
                addedAdmin ? "created" : "kept or skipped"
        );
    }

    private boolean createAdminIfConfigured(JsonNode studioAccount) {
        String configuredEmail =
                adminEmailOverride == null
                        ? ""
                        : adminEmailOverride.trim();

        String seedEmail =
                studioAccount.path("email").asText("").trim();

        String email = (
                configuredEmail.isEmpty()
                        ? seedEmail
                        : configuredEmail
        ).toLowerCase(Locale.ROOT);

        if (email.isEmpty()) {
            log.warn(
                    "Admin creation skipped: set KALA_ADMIN_EMAIL "
                            + "or provide studioAccount.email in seed.json."
            );
            return false;
        }

        if (users.existsByEmail(email)) {
            return false;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn(
                    "Admin creation skipped: set KALA_ADMIN_PASSWORD."
            );
            return false;
        }

        users.save(new User(
                "Studio",
                email,
                passwordEncoder.encode(adminPassword),
                User.Role.ADMIN
        ));

        return true;
    }

    private static String imagePath(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        return imageUrl.startsWith("/")
                ? imageUrl
                : "/" + imageUrl;
    }
}
