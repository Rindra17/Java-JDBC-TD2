package com.school.hei.repository;

import com.school.hei.DataBase.DBConnection;
import com.school.hei.model.Dish;
import com.school.hei.model.Ingredient;
import com.school.hei.type.CategoryEnum;
import com.school.hei.type.DishTypeEnum;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) {
        String dishSql = """
           select d.id as dish_id, d.name as dish_name, d.dish_type
           from dish d
           where id = ?
        """;

        String ingredientSql = """
            select i.id as ing_id, i.name as ing_name, i.price as ing_price, i.category as ing_category
            from ingredient i
            where i.id = ?
            order by i.id
        """;

        Connection con;
        PreparedStatement dishStmt;
        ResultSet dishRs;
        PreparedStatement ingredientStmt;
        ResultSet ingredientRs;

        try {
           con = dbConnection.getDBConnection();
           dishStmt = con.prepareStatement(dishSql);
           dishStmt.setInt(1, id);
           dishRs = dishStmt.executeQuery();
           if (!dishRs.next()) {
               throw new RuntimeException("Dish with ID " + id +" not found");
           }

           Dish dish = new Dish();
           dish.setId(dishRs.getInt("dish_id"));
           dish.setName(dishRs.getString("dish_name"));
           dish.setDishType(DishTypeEnum.valueOf(dishRs.getString("dish_type")));

           ingredientStmt = con.prepareStatement(ingredientSql);
           ingredientStmt.setInt(1, id);
           ingredientRs = ingredientStmt.executeQuery();

           List<Ingredient> ingredients = new ArrayList<>();
           while (ingredientRs.next()) {
               Ingredient ingredient = new Ingredient();
               ingredient.setId(ingredientRs.getInt("ing_id"));
               ingredient.setName(ingredientRs.getString("ing_name"));
               ingredient.setPrice(ingredientRs.getDouble("ing_price"));
               ingredient.setCategory(CategoryEnum.valueOf(ingredientRs.getString("ing_category")));
               ingredients.add(ingredient);
           }
           dish.setIngredients(ingredients);
           dbConnection.closeDBConnection(con);
           return dish;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ingredient> findIngredients(int page, int size) {
        String sql = """
            select i.id as ing_id, i.name as ing_name, i.price as ing_price, i.category as ing_category, i.id_dish
            from ingredient i
            order by i.id
            limit ? offset ?
        """;

        int offset = (page - 1) * size;
        Connection con;
        PreparedStatement ingredientStmt;
        ResultSet ingredientRs;

        try {
            con = dbConnection.getDBConnection();
            ingredientStmt = con.prepareStatement(sql);
            ingredientStmt.setInt(1, size);
            ingredientStmt.setInt(2, offset);
            ingredientRs = ingredientStmt.executeQuery();
            List<Ingredient> ingredients = new ArrayList<>();
            while (ingredientRs.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(ingredientRs.getInt("ing_id"));
                ingredient.setName(ingredientRs.getString("ing_name"));
                ingredient.setPrice(ingredientRs.getDouble("ing_price"));
                ingredient.setCategory(CategoryEnum.valueOf(ingredientRs.getString("ing_category")));
                if (ingredientRs.getInt("id_dish") > 0) {
                    ingredient.setDish(findDishById(ingredientRs.getInt("id_dish")));
                }
                ingredients.add(ingredient);
            }
            dbConnection.closeDBConnection(con);
            return ingredients;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    List<Ingredient> createIngredients(List<Ingredient> newIngredients) {

        throw new RuntimeException("Not Implemented");
    }

    Dish saveDish(Dish dishToSave) {
        throw new RuntimeException("Not Implemented");
    }

    List<Dish> findDishsByIngredientName(String ingredientName) {
        throw new RuntimeException("Not Implemented");
    }

    List<Ingredient> findIngredientsByCriteria(String ingredientName, CategoryEnum category, String dishName, int page, int size) {
        throw new RuntimeException("Not Implemented");
    }
}
