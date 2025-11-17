package pl.kurs.sogaapplication.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FoodCostSummary(
        LocalDate from,
        LocalDate to,
        List<Integer> sellerIds,
        List<Integer> warehouseIds,
        BigDecimal kitchenSalesNet,
        BigDecimal purchasesFzNet,
        BigDecimal purchasesPzNet,
        BigDecimal purchasesTotalNet,
        BigDecimal foodCostPercent
) { }

