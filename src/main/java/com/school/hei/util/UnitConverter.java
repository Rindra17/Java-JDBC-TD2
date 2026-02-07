package com.school.hei.util;

import com.school.hei.type.UnitType;

import java.util.HashMap;
import java.util.Map;

public class UnitConverter {
    private static final Map<String, Map<UnitType, Map<UnitType, Double>>> CONVERSIONS = new HashMap<>();

    static {
        // Tomate
        add("Tomate", UnitType.KG, UnitType.PCS, 10);
        add("Tomate", UnitType.PCS, UnitType.KG, 0.1);

        // Laitue
        add("Laitue", UnitType.KG, UnitType.PCS, 2);
        add("Laitue", UnitType.PCS, UnitType.KG, 0.5);

        // Chocolat
        add("Chocolat", UnitType.KG, UnitType.PCS, 10);
        add("Chocolat", UnitType.PCS, UnitType.KG, 0.1);
        add("Chocolat", UnitType.KG, UnitType.L, 2.5);
        add("Chocolat", UnitType.L, UnitType.KG, 0.4);
        add("Chocolat", UnitType.PCS, UnitType.L, 0.25);
        add("Chocolat", UnitType.L, UnitType.PCS, 4);

        // Poulet
        add("Poulet", UnitType.KG, UnitType.PCS, 8);
        add("Poulet", UnitType.PCS, UnitType.KG, 0.125);

        // Beurre
        add("Beurre", UnitType.KG, UnitType.PCS, 4);
        add("Beurre", UnitType.PCS, UnitType.KG, 0.25);
        add("Beurre", UnitType.KG, UnitType.L, 5);
        add("Beurre", UnitType.L, UnitType.KG, 0.2);
        add("Beurre", UnitType.PCS, UnitType.L, 1.25);
        add("Beurre", UnitType.L, UnitType.PCS, 0.8);
    }

    private static void add(String ingredient, UnitType from, UnitType to, double factor) {
        CONVERSIONS
                .computeIfAbsent(ingredient, k -> new HashMap<>())
                .computeIfAbsent(from, k -> new HashMap<>())
                .put(to, factor);
    }

    public static Double convertTo(String ingredientName, UnitType from, UnitType to, Double value) {
        if (from == to) return value;

        ingredientName = ingredientName.trim();

        Map<UnitType, Map<UnitType, Double>> ingredientMap = CONVERSIONS.get(ingredientName);
        if (ingredientMap == null)
            throw new RuntimeException("Unknown ingredient: " + ingredientName);

        Map<UnitType, Double> fromMap = ingredientMap.get(from);
        if (fromMap == null || !fromMap.containsKey(to))
            throw new RuntimeException("Cannot convert from " + from + " to " + to);

        return value * fromMap.get(to);
    }
}
