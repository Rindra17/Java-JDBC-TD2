package com.school.hei.model;

import com.school.hei.type.CategoryEnum;

import java.util.Objects;

public class Ingredient {
    private Integer id;
    private String name;
    private Double price;
    private CategoryEnum category;
    private Dish dish;

    public Ingredient(int id, String name, Double price, CategoryEnum category, Dish dish) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.dish = dish;
    }

    public Ingredient() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Id must be a positive integer");
        }
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        if (price < 0 || price == null) {
            throw new IllegalArgumentException("Price must not be null or negative");
        }
        this.price = price;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public void setCategory(CategoryEnum category) {
        this.category = category;
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    private String getDishName() {
        return dish == null ? null : dish.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(price, that.price)
                && category == that.category
                && Objects.equals(dish, that.dish);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, category, dish);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category=" + category +
                ", dish=" + getDishName() +
                '}';
    }
}