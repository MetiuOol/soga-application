package pl.kurs.sogaapplication.service.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.kurs.sogaapplication.dto.RestaurantReportDto;
import pl.kurs.sogaapplication.models.DzienPodzial;
import pl.kurs.sogaapplication.service.analysis.SalesAnalysisService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serwis do eksportu raportów
 * Odpowiada za eksport danych do różnych formatów (XML, Excel, CSV)
 */
@Service
public class ReportExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportExportService.class);
    
    private final SalesAnalysisService salesAnalysisService;
    
    public ReportExportService(SalesAnalysisService salesAnalysisService) {
        this.salesAnalysisService = salesAnalysisService;
    }
    
    /**
     * Eksportuje raport do XML
     */
    public void exportToXml(RestaurantReportDto report) {
        exportToXml(report, generateXmlFileName(report));
    }
    
    /**
     * Eksportuje raport do XML z niestandardową nazwą pliku
     */
    public void exportToXml(RestaurantReportDto report, String fileName) {
        logger.info("Eksport raportu do XML: {}", fileName);
        
        try {
            String xmlContent = buildXmlContent(report);
            Path filePath = Path.of(fileName);
            Files.writeString(filePath, xmlContent, StandardCharsets.UTF_8);
            
            logger.info("Raport XML został zapisany: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Błąd podczas zapisu XML: {}", e.getMessage(), e);
            throw new RuntimeException("Nie udało się zapisać raportu XML", e);
        }
    }
    
    /**
     * Eksportuje dane dzienne do XML (kompatybilność z istniejącym kodem)
     */
    public void exportDailySalesToXml(LocalDate from, Collection<Integer> sellerIds) {
        logger.info("Eksport sprzedaży dziennej do XML od {} dla sprzedawców: {}", from, sellerIds);
        
        try {
            List<DzienPodzial> dailySales = salesAnalysisService.analyzeDailySales(from, sellerIds);
            String xmlContent = buildDailySalesXml(dailySales, from, sellerIds);
            String fileName = generateDailySalesXmlFileName(from, sellerIds);
            Path filePath = Path.of(fileName);
            Files.writeString(filePath, xmlContent, StandardCharsets.UTF_8);
            
            logger.info("Raport XML sprzedaży dziennej został zapisany: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Błąd podczas zapisu XML sprzedaży dziennej: {}", e.getMessage(), e);
            throw new RuntimeException("Nie udało się zapisać raportu XML sprzedaży dziennej", e);
        }
    }
    
    /**
     * Eksportuje raport do CSV
     */
    public void exportToCsv(RestaurantReportDto report) {
        exportToCsv(report, generateCsvFileName(report));
    }
    
    /**
     * Eksportuje raport do CSV z niestandardową nazwą pliku
     */
    public void exportToCsv(RestaurantReportDto report, String fileName) {
        logger.info("Eksport raportu do CSV: {}", fileName);
        
        try {
            String csvContent = buildCsvContent(report);
            Path filePath = Path.of(fileName);
            Files.writeString(filePath, csvContent, StandardCharsets.UTF_8);
            
            logger.info("Raport CSV został zapisany: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Błąd podczas zapisu CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Nie udało się zapisać raportu CSV", e);
        }
    }
    
    // Metody pomocnicze
    
    private String buildXmlContent(RestaurantReportDto report) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<restaurantReport>\n");
        sb.append("  <period from=\"").append(report.from()).append("\" to=\"").append(report.to()).append("\"/>\n");
        sb.append("  <sellers>").append(report.sellerIds()).append("</sellers>\n");
        sb.append("  <summary>\n");
        sb.append("    <kitchen>").append(report.kitchenSales().toPlainString()).append("</kitchen>\n");
        sb.append("    <buffet>").append(report.buffetSales().toPlainString()).append("</buffet>\n");
        sb.append("    <packaging>").append(report.packagingSales().toPlainString()).append("</packaging>\n");
        sb.append("    <delivery>").append(report.deliverySales().toPlainString()).append("</delivery>\n");
        sb.append("    <total>").append(report.totalSales().toPlainString()).append("</total>\n");
        sb.append("  </summary>\n");
        sb.append("  <dailySales>\n");
        
        for (var daily : report.dailySales()) {
            sb.append("    <day date=\"").append(daily.date()).append("\">\n");
            sb.append("      <kitchen>").append(daily.kitchen().toPlainString()).append("</kitchen>\n");
            sb.append("      <buffet>").append(daily.buffet().toPlainString()).append("</buffet>\n");
            sb.append("      <packaging>").append(daily.packaging().toPlainString()).append("</packaging>\n");
            sb.append("      <delivery>").append(daily.delivery().toPlainString()).append("</delivery>\n");
            sb.append("      <total>").append(daily.total().toPlainString()).append("</total>\n");
            sb.append("    </day>\n");
        }
        
        sb.append("  </dailySales>\n");
        sb.append("</restaurantReport>\n");
        return sb.toString();
    }
    
    private String buildDailySalesXml(List<DzienPodzial> dailySales, LocalDate from, Collection<Integer> sellerIds) {
        var last = from.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        String ids = sellerIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<sprzedazMiesiac from=\"").append(from).append("\" to=\"").append(last)
                .append("\" sellerIds=\"").append(ids).append("\">\n");
        for (var r : dailySales) {
        sb.append("  <dzien data=\"").append(r.dzien()).append("\">\n");
        sb.append("    <kuchnia>").append(r.kuchnia().toPlainString()).append("</kuchnia>\n");
        sb.append("    <bufet>").append(r.bufet().toPlainString()).append("</bufet>\n");
        sb.append("    <opakowania>").append(r.opakowania().toPlainString()).append("</opakowania>\n");
        sb.append("    <dowoz>").append(r.dowoz().toPlainString()).append("</dowoz>\n");
        sb.append("    <suma>").append(r.suma().toPlainString()).append("</suma>\n");
        sb.append("  </dzien>\n");
        }
        sb.append("</sprzedazMiesiac>\n");
        return sb.toString();
    }
    
    private String buildCsvContent(RestaurantReportDto report) {
        var sb = new StringBuilder();
        sb.append("Data,Kuchnia,Bufet,Opakowania,Dowoz,Razem\n");
        
        for (var daily : report.dailySales()) {
            sb.append(daily.date()).append(",")
              .append(daily.kitchen().toPlainString()).append(",")
              .append(daily.buffet().toPlainString()).append(",")
              .append(daily.packaging().toPlainString()).append(",")
              .append(daily.delivery().toPlainString()).append(",")
              .append(daily.total().toPlainString()).append("\n");
        }
        
        return sb.toString();
    }
    
    private String generateXmlFileName(RestaurantReportDto report) {
        return String.format("restaurant-report-%s-to-%s.xml", 
                report.from().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                report.to().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
    
    private String generateDailySalesXmlFileName(LocalDate from, Collection<Integer> sellerIds) {
        String ids = sellerIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return String.format("sprzedaz-%d-%02d-%s.xml",
                from.getYear(),
                from.getMonthValue(),
                ids);
    }
    
    private String generateCsvFileName(RestaurantReportDto report) {
        return String.format("restaurant-report-%s-to-%s.csv", 
                report.from().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                report.to().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}
