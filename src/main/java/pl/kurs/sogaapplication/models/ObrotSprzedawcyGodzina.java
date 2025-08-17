package pl.kurs.sogaapplication.models;

import java.math.BigDecimal;

public record ObrotSprzedawcyGodzina(
        Integer sellerId,
        String sellerName,
        Short godzina,
        BigDecimal suma
) {}
