package it.davideangelone.mongopoc.config;

import it.davideangelone.mongopoc.model.User;
import it.davideangelone.mongopoc.model.UserItem;
import it.davideangelone.mongopoc.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDatabase(UserRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            User davide = new User(null, "Davide Rossi", "davide.rossi@example.com",
                    List.of(
                            task("Lista della spesa", "Acquisti settimanali", "HIGH", false, List.of("CASA", "PERSONALE")),
                            expense("Spesa supermercato", 125.50, "EUR", "Esselunga", "CREDIT_CARD" ),
                            recipe("Pasta alla carbonara", 25, "EASY", 2, List.of("Pasta", "Uova", "Guanciale", "Pecorino")),
                            note("Appunti Spring Boot 4", "Spring Boot 4 utilizza Spring Framework 7", List.of("JAVA", "SPRING"))
                    )
            );

            User luca = new User(null, "Luca Bianchi", "luca.bianchi@example.com",
                    List.of(
                            task("Pianificare sprint", "Definire le attività della settimana", "MEDIUM", false, List.of("LAVORO")),
                            expense("Acquisto monitor", 349.90, "EUR", "Amazon", "PAYPAL"),
                            travel("Viaggio a Parigi", "2027-04-10", "2027-04-14", "Parigi", "Hotel Eiffel")
                    )
            );

            User sara = new User( null, "Sara Verdi", "sara.verdi@example.com",
                    List.of(
                            recipe("Tiramisù", 40, "MEDIUM", 6, List.of("Mascarpone", "Uova", "Zucchero", "Caffè", "Savoiardi")),
                            note("Documentazione MongoDB", "Approfondire aggregation pipeline e indici", List.of("MONGODB", "DATABASE"))
                    )
            );

            repository.saveAll(List.of(davide, luca, sara));
        };
    }

    private UserItem task(String title, String description, String priority, boolean completed, List<String> tags) {
        return item("TASK", title,
                Map.of(
                "description", description,
                "priority", priority,
                "completed", completed,
                "tags", tags
                )
        );
    }

    private UserItem expense(String title, double amount, String currency, String merchant, String paymentMethod) {
        return item("EXPENSE", title,
                Map.of(
                "amount", amount,
                "currency", currency,
                "merchant", merchant,
                "payment", Map.of(
                                "method", paymentMethod,
                                "installments", 1
                                )
                )
        );
    }

    private UserItem recipe(String title, int preparationMinutes, String difficulty, int servings, List<String> ingredients) {
        return item("RECIPE", title,
                Map.of(
                "preparationMinutes", preparationMinutes,
                "difficulty", difficulty,
                "servings", servings,
                "ingredients", ingredients,
                "steps", List.of(
                                 "Preparare gli ingredienti",
                                 "Eseguire la preparazione",
                                 "Servire"
                            )
                )
        );
    }

    private UserItem note(String title, String content, List<String> tags) {
        return item("NOTE", title,
                Map.of(
                "content", content,
                "format", "TEXT",
                "tags", tags
                )
        );
    }

    private UserItem travel(String title, String departure, String returnDate, String destination, String hotelName) {
        return item("TRAVEL", title,
                Map.of(
                "destination", destination,
                "departure", departure,
                "return", returnDate,
                "hotel", Map.of(
                             "name", hotelName,
                              "rating", 4
                            ),
                "flights",
                List.of(
                        Map.of(
                                "type", "OUTBOUND",
                                "airline", "ITA Airways"
                        ),
                        Map.of(
                                "type", "RETURN",
                                "airline", "ITA Airways"
                        )
                    )
                )
        );
    }

    private UserItem item(String type, String title, Map<String, Object> data) {
        return new UserItem(UUID.randomUUID().toString(), type, title, Instant.now(), data);
    }
}