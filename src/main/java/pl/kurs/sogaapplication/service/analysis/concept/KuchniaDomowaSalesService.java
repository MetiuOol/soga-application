package pl.kurs.sogaapplication.service.analysis.concept;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.models.business.PointOfSale;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;
import pl.kurs.sogaapplication.service.config.PointOfSaleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class KuchniaDomowaSalesService {

    private static final Logger logger = LoggerFactory.getLogger(KuchniaDomowaSalesService.class);

    private final RachunekJpaRepository rachunekRepository;
    private final PointOfSaleService pointOfSaleService;

    public KuchniaDomowaSalesService(RachunekJpaRepository rachunekRepository,
                                    PointOfSaleService pointOfSaleService) {
        this.rachunekRepository = rachunekRepository;
        this.pointOfSaleService = pointOfSaleService;
    }
    
    /**
     * Pobiera punkt sprzedaży Kuchnia Domowa
     */
    private PointOfSale getKuchniaDomowa() {
        return pointOfSaleService.getPointOfSale("KD")
                .orElseThrow(() -> new IllegalStateException("Punkt sprzedaży KD nie został znaleziony"));
    }

    /**
     * Generuje szczegółową analizę sprzedaży dla Kuchni Domowej
     */
    @Transactional(readOnly = true)
    public AnalizaSprzedazyKD getDetalowaAnaliza(LocalDate dataOd, LocalDate dataDo) {
        logger.info("Generowanie szczegółowej analizy Kuchni Domowej od {} do {}", dataOd, dataDo);

        LocalDateTime fromDateTime = dataOd.atStartOfDay();
        LocalDateTime toDateTime = dataDo.atTime(23, 59, 59);

        // TODO: Tutaj będziemy implementować zapytania do bazy
        // Na razie zwracamy pusty rekord

        return new AnalizaSprzedazyKD(
                dataOd, dataDo,
                BigDecimal.ZERO, BigDecimal.ZERO, 0,  // na miejscu
                BigDecimal.ZERO, BigDecimal.ZERO, 0,  // na wynos
                0, BigDecimal.ZERO,                    // zupy
                0, BigDecimal.ZERO,                    // abonamenty
                0, BigDecimal.ZERO,                    // karnety zwykłe
                0, BigDecimal.ZERO,                    // karnety mięsne
                BigDecimal.ZERO, 0                     // podsumowanie
        );
    }
}