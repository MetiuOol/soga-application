package pl.kurs.sogaapplication.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serwis konfiguracyjny dla restauracji
 * Zarządza konfiguracją grup towarów, sprzedawców itp.
 */
@Service
public class RestaurantConfigService {
    
    @Value("${restaurant.kitchen.groups}")
    private String kitchenGroupsConfig;

    /**
     * Lista konkretnych towarów kuchennych (ID_TW z tabeli TOWARY).
     * Pozwala rozdzielić kuchnię/bufet precyzyjnie po produktach,
     * nawet jeśli w jednej grupie są towary z kuchni i z bufetu.
     */
    @Value("${restaurant.kitchen.products:}")
    private String kitchenProductsConfig;

    // Produkty bufetu (napoje, alkohole, dodatki)
    @Value("${restaurant.buffet.products:}")
    private String buffetProductsConfig;

    // Grupy bufetowe (np. napoje, alkohole, desery)
    @Value("${restaurant.buffet.groups:}")
    private String buffetGroupsConfig;

    // Produkty opakowań (na wynos itp.)
    @Value("${restaurant.packaging.products:}")
    private String packagingProductsConfig;

    // Produkty dowozu (dopłaty do abonamentów itp.)
    @Value("${restaurant.delivery.products:}")
    private String deliveryProductsConfig;

    @Value("${restaurant.warehouses.kitchen:}")
    private String kitchenWarehousesConfig;

    @Value("${restaurant.warehouses.buffet:}")
    private String buffetWarehousesConfig;

    @Value("${restaurant.warehouses.costs:}")
    private String costsWarehousesConfig;
    
    @Value("${restaurant.sellers.default}")
    private String defaultSellersConfig;
    
    @Value("${restaurant.sellers.all}")
    private String allSellersConfig;
    
    /**
     * Zwraca listę ID grup towarów kuchennych
     */
    public List<Integer> getKitchenGroups() {
        return parseIntIds(kitchenGroupsConfig);
    }

    /**
     * Zwraca listę ID_TW towarów kuchennych.
     * Jeśli konfiguracja jest pusta, zwraca pustą listę.
     */
    public List<Long> getKitchenProducts() {
        return parseLongIds(kitchenProductsConfig);
    }

    public List<Long> getBuffetProducts() {
        return parseLongIds(buffetProductsConfig);
    }

    /**
     * Zwraca listę grup bufetowych (ID_GR), jeśli są skonfigurowane.
     */
    public List<Integer> getBuffetGroups() {
        return parseIntIds(buffetGroupsConfig);
    }

    public List<Long> getPackagingProducts() {
        return parseLongIds(packagingProductsConfig);
    }

    public List<Long> getDeliveryProducts() {
        return parseLongIds(deliveryProductsConfig);
    }

    public List<Integer> getKitchenWarehouses() {
        return parseIntIds(kitchenWarehousesConfig);
    }

    public List<Integer> getBuffetWarehouses() {
        return parseIntIds(buffetWarehousesConfig);
    }

    public List<Integer> getCostWarehouses() {
        return parseIntIds(costsWarehousesConfig);
    }
    
    /**
     * Zwraca listę ID domyślnych sprzedawców
     */
    public List<Integer> getDefaultSellers() {
        return parseIntIds(defaultSellersConfig);
    }
    
    /**
     * Zwraca listę ID wszystkich sprzedawców
     */
    public List<Integer> getAllSellers() {
        return parseIntIds(allSellersConfig);
    }
    
    /**
     * Sprawdza czy dana grupa towaru należy do kuchni
     */
    public boolean isKitchenGroup(Integer groupId) {
        return getKitchenGroups().contains(groupId);
    }

    /**
     * Sprawdza czy dana grupa towaru należy do bufetu.
     */
    public boolean isBuffetGroup(Integer groupId) {
        return getBuffetGroups().contains(groupId);
    }

    /**
     * Sprawdza czy dany towar (ID_TW) należy do kuchni.
     */
    public boolean isKitchenProduct(Long productId) {
        return getKitchenProducts().contains(productId);
    }
    
    /**
     * Sprawdza czy dany sprzedawca jest w domyślnej liście
     */
    public boolean isDefaultSeller(Integer sellerId) {
        return getDefaultSellers().contains(sellerId);
    }

    private List<Integer> parseIntIds(String config) {
        if (config == null || config.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private List<Long> parseLongIds(String config) {
        if (config == null || config.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(config.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}





 