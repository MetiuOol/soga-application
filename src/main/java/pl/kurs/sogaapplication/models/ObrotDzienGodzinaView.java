package pl.kurs.sogaapplication.models;

import java.math.BigDecimal;

public interface ObrotDzienGodzinaView  {
    Integer getSellerId();
    String  getSellerName();
    Integer getDzien();     // 0=nd ... 6=sb (Firebird WEEKDAY)
    Integer getGodzina();   // 0..23
    BigDecimal getSuma();
}
