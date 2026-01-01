package com.school.hei.model;

import com.school.hei.type.DishTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private Integer id;
    private String name;
    private DishTypeEnum dishType;
    private List<Ingredient> ingredients;

    public Dish() {
    }

    public Dish(int id, String name, DishTypeEnum dishType, List<Ingredient> ingredients) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.ingredients = ingredients;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must not be empty or null");
        }
        this.name = name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public void setDishType(DishTypeEnum dishType) {
        this.dishType = dishType;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> newIngredients) {
        if (this.ingredients != null) {
            for (Ingredient oldIngredient : this.ingredients) {
                if (oldIngredient != null && oldIngredient.getDish() == this) {
                    oldIngredient.setDish(null);
                }
            }
        }
        this.ingredients = newIngredients == null ? new ArrayList<Ingredient>() : newIngredients;

        for (Ingredient newIngredient : ingredients) {
            if (newIngredient != null) {
                newIngredient.setDish(this);
            }
        }
    }

    private Double getDishCost() {
        return ingredients == null ? null : ingredients.stream()
                .mapToDouble(Ingredient::getPrice)
                .sum();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id)
                && Objects.equals(name, dish.name)
                && dishType == dish.dishType
                && Objects.equals(ingredients, dish.ingredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dishType, ingredients);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dishType=" + dishType +
                ", ingredients=" + ingredients +
                ", price=" + getDishCost() +
                '}';
    }
}
