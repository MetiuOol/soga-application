package pl.kurs.sogaapplication.models;

import java.math.BigDecimal;

public interface ObrotDzien {
    Integer getSellerId();
    String  getSellerName();
    Integer getDzien();     // 0=nd ... 6=sb (Firebird WEEKDAY)
    Integer getGodzina();
    BigDecimal getSuma();
}
