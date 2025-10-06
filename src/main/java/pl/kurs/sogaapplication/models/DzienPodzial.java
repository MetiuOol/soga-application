package pl.kurs.sogaapplication.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DzienPodzial(LocalDate dzien, BigDecimal kuchnia, BigDecimal bufet, BigDecimal suma) {}
