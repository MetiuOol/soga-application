package pl.kurs.sogaapplication.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO reprezentujące pojedynczy dokument zakupu (FZ lub PZ) do wyświetlenia w raporcie.
 */
public record DokumentZakupuDto(
        Long idDok,
        String typDok,
        Long idPochodzenia,
        String nrOryginalny,
        Integer idFirmy,
        LocalDate dataWst,
        String calyNumer,
        BigDecimal wartNu
) { }

