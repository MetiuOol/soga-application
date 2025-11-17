package pl.kurs.sogaapplication.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Podsumowanie zakupów magazynu z wybranego okresu.
 */
public record KitchenPurchasesSummary(
        LocalDate from,
        LocalDate to,
        String warehouseName,
        List<Integer> warehouseIds,
        BigDecimal purchasesFzNet,
        BigDecimal purchasesPzNet,
        BigDecimal purchasesKfzNet,
        BigDecimal purchasesMmpNet,
        BigDecimal purchasesMmNet,
        BigDecimal purchasesTotalNet,
        List<DokumentZakupuDto> dokumenty
) { }

