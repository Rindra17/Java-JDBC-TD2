package com.school.hei.repository;

import com.school.hei.DataBase.DBConnection;
import com.school.hei.model.*;
import com.school.hei.type.CategoryEnum;
import com.school.hei.type.DishTypeEnum;
import com.school.hei.type.MovementTypeEnum;
import com.school.hei.type.UnitType;
import com.school.hei.util.UnitConverter;
import com.sun.jdi.Value;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {
    public final DBConnection dbConnection = new DBConnection();

    public Dish findDishById(Integer id) throws SQLException {
        String sql = """
                select d.id as dish_id, d.name as dish_name, dish_type, d.selling_price as dish_price
                from dish d where d.id = ?;
                """;

        Dish dish = new Dish();
        try (Connection con = dbConnection.getDBConnection();
             PreparedStatement ps = con.prepareStatement(sql);) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dish.setId(rs.getInt("dish_id"));
                    dish.setName(rs.getString("dish_name"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setPrice(rs.getObject("dish_price") == null
                            ? null : rs.getDouble("dish_price"));
                    List<DishIngredient> dishIngredients = findIngredientByDishId(rs.getInt("dish_id"));
                    dish.setIngredients(dishIngredients);
                    return dish;
                }
                throw new RuntimeException("Dish not found " + id);
            }
        }
    }

    public List<DishIngredient> findIngredientByDishId(Integer id) throws SQLException {
        String sql = """
                select ingredient.id, ingredient.name, ingredient.price, ingredient.category
                from ingredient join dish_ingredient on ingredient.id = dish_ingredient.id_ingredient 
                where dish_ingredient.id_dish = ?
                """;

        List<DishIngredient> dishIngredients = new ArrayList<>();
        try (Connection con = dbConnection.getDBConnection();
             PreparedStatement ps = con.prepareStatement(sql);) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DishIngredient di = new DishIngredient();
                    di.setId(rs.getInt("id"));
                    di.setQuantity(rs.getDouble("ing_quantity"));
                    di.setUnit(UnitType.valueOf(rs.getString("ing_unit")));

                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(rs.getInt("id"));
                    ingredient.setName(rs.getString("name"));
                    ingredient.setPrice(rs.getDouble("price"));
                    ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                    di.setIngredient(ingredient);
                    dishIngredients.add(di);
                }
                dbConnection.closeDBConnection(con);
                return dishIngredients;
            }
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }
        List<Ingredient> savedIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        Connection conn = dbConnection.getDBConnection();
        try {
            conn.setAutoCommit(false);
            String insertSql = """
                        INSERT INTO ingredient (id, name, category, price)
                        VALUES (?, ?, ?::ingredient_category, ?)
                        returning id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient ingredient : newIngredients) {
                    if (ingredient.getId() != null) {
                        ps.setInt(1, ingredient.getId());
                    } else {
                        ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
                    }
                    ps.setString(2, ingredient.getName());
                    ps.setString(3, ingredient.getCategory().name());
                    ps.setDouble(4, ingredient.getPrice());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        int generatedId = rs.getInt(1);
                        ingredient.setId(generatedId);
                        savedIngredients.add(ingredient);
                    }
                }
                conn.commit();
                return savedIngredients;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeDBConnection(conn);
        }
    }

    public Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                    INSERT INTO dish (id, selling_price, name, dish_type)
                    VALUES (?, ?, ?, ?::dish_type)
                    ON CONFLICT (id) DO UPDATE
                    SET name = excluded.name,
                        dish_type = excluded.dish_type,
                        selling_price = excluded.selling_price
                    RETURNING id
                """;

        try (Connection conn = dbConnection.getDBConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "dish", "id"));
                }
                if (toSave.getPrice() != null) {
                    ps.setDouble(2, toSave.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getDishType().name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            List<DishIngredient> newDishIngredients = toSave.getIngredients();

            detachIngredients(conn, dishId, newDishIngredients);
            attachIngredients(conn, dishId, toSave.getIngredients());

            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Ingredient saveIngredient(Ingredient ingredient) {
        String baseSql = """
                insert into stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
                values (?, ?, ?, ?::movement_type, ?::unit, ?)
                on conflict (id) do nothing;
                """;
        try (Connection connection = dbConnection.getDBConnection();
             PreparedStatement ps = connection.prepareStatement(baseSql)) {
            for (StockMovement mvt : ingredient.getStockMovementList()) {
                if (mvt.getId() != null) {
                    ps.setInt(1, mvt.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(connection, "stockmovement", "id"));
                }
                ps.setInt(2, ingredient.getId());
                ps.setDouble(3, mvt.getValue().getQuantity());
                ps.setString(4, mvt.getType().name());
                ps.setString(5, mvt.getValue().getUnit().name());
                ps.setTimestamp(6, Timestamp.from(mvt.getCreationDatetime()));

                ps.addBatch();
            }
            ps.executeBatch();
            return ingredient;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Ingredient findIngredientById(Integer id) throws SQLException {
        String sql = """
                 select id, name, price, category
                            from ingredient
                            where id = ?
                """;

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(rs.getInt("id"));
                    ingredient.setName(rs.getString("name"));
                    ingredient.setPrice(rs.getDouble("price"));
                    ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                    ingredient.setStockMovementList(getStockMouvementsByIngredientId(id));

                    return ingredient;
                }
                throw new RuntimeException("Ingredient not found " + id);
            }
        }
    }

    public List<DishIngredient> findDishIngredientById(Integer id) throws SQLException {
        List<DishIngredient> dishIngredients = new ArrayList<>();
        String sql = """
                select d.id , id_dish,id_ingredient, required_quantity, unit,
                i.id as ing_id , i.name as ing_name, i.price as ing_price,
                i.category as ing_category
                from dish_ingredient d join ingredient i
                on d.id_ingredient = i.id
                where id_dish = ?
                """;
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                DishIngredient dishIng = new DishIngredient();
                Ingredient ing = new Ingredient();
                dishIng.setId(resultSet.getInt(1));
                dishIng.setQuantity(resultSet.getDouble("required_quantity"));
                dishIng.setUnit(UnitType.valueOf(resultSet.getString("unit")));

                ing.setId(resultSet.getInt("ing_id"));
                ing.setName(resultSet.getString("ing_name"));
                ing.setPrice(resultSet.getDouble("ing_price"));
                ing.setCategory(CategoryEnum.valueOf(resultSet.getString("ing_category")));
                dishIng.setIngredient(ing);
                dishIngredients.add(dishIng);

            }
            return dishIngredients;
        }
    }

    private List<StockMovement> getStockMouvementsByIngredientId(Integer id) throws SQLException {
        String sql = """
                    SELECT id, id_ingredient, quantity, type, unit, creation_datetime
                    FROM stock_movement
                    WHERE id_ingredient = ?
                """;

        List<StockMovement> stockMovements = new ArrayList<>();

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    StockMovement stockMovement = new StockMovement();
                    stockMovement.setId(rs.getInt("id"));

                    StockValue value = new StockValue();
                    value.setQuantity(rs.getDouble("quantity"));
                    value.setUnit(UnitType.valueOf(rs.getString("unit")));
                    stockMovement.setValue(value);

                    stockMovement.setType(MovementTypeEnum.valueOf(rs.getString("type")));

                    stockMovement.setCreationDatetime(rs.getTimestamp("creation_date").toInstant());

                    stockMovements.add(stockMovement);
                }
            }
        }
        return stockMovements;
    }

    private void detachIngredients(Connection conn, Integer dishId, List<DishIngredient> ingredients) throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "delete from dish_ingredient where id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            }
            return;
        }
        String baseSql = """
                delete from dishingredient where id_dish = ?
                and id_ingredient not in (%s)
                """;
        String inClause = ingredients.stream().map(i -> "?").collect(Collectors.joining(","));
        String finalSql = String.format(baseSql, inClause);

        try (PreparedStatement ps = conn.prepareStatement(finalSql)) {
            ps.setInt(1, dishId);
            int idx = 2;
            for (DishIngredient ingredient : ingredients) {
                ps.setInt(idx, ingredient.getId());
                idx++;
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void attachIngredients(Connection conn, Integer dishId, List<DishIngredient> dishIngredients) throws SQLException {

        if (dishIngredients == null || dishIngredients.isEmpty()) {
            return;
        }

        String attachSql = """
                   insert into dish_ingredient (id_dish, id_ingredient, required_quantity, unit)
                   values (?,?,?,?::unit)
                   on conflict do update
                   set required_quantity= EXCLUDED.required_quantity,
                       unit= EXCLUDED.unit;
                """;

        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            for (DishIngredient dishIngredient : dishIngredients) {
                ps.setInt(1, dishId);
                ps.setInt(2, dishIngredient.getIngredient().getId());
                ps.setDouble(3, dishIngredient.getQuantity());
                ps.setString(4, dishIngredient.getUnit().name());

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private int getNextSerialValue(Connection con, String tableName, String columnNane) throws SQLException {
        String sequenceName = getSerialSequenceName(con, tableName, columnNane);
        if (sequenceName == null) {
            throw new IllegalArgumentException(
                    "Any sequence found for " + tableName + "." + columnNane
            );
        }
        updateSeqenceNextValue(con, tableName, columnNane, sequenceName);

        String nextValSql = "select nextval(?)";
        try (PreparedStatement ps = con.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateSeqenceNextValue(Connection con, String tableName, String columnNane, String sequenceName) throws
            SQLException {
        String setValSql = String.format(
                "select setval('%s', (select coalesce(max(%s), 0) from %s))",
                sequenceName, columnNane, tableName
        );

        try (PreparedStatement ps = con.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }

    private String getSerialSequenceName(Connection con, String tableName, String columnNane) throws SQLException {
        String sql = "select pg_get_serial_sequence(?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnNane);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    public Order saveOrder(Order orderToSave) throws SQLException {
        Connection conn = null;
        try {
            conn = dbConnection.getDBConnection();
            conn.setAutoCommit(false);

            checkStockAvailability(orderToSave, conn);

            String reference = generateOrderReference(conn);
            orderToSave.setReference(reference);

            insertOrder(orderToSave, conn);

            insertDishOrderLines(orderToSave, conn);

            conn.commit();
            return orderToSave;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new RuntimeException("Erreur lors de la création de la commande", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void checkStockAvailability(Order order, Connection conn) throws SQLException {
        StringBuilder errorMsg = new StringBuilder();

        for (DishOrder doLine : order.getDishOrders()) {
            Dish dish = findDishById(doLine.getDish().getId());

            for (DishIngredient comp : dish.getIngredients()) {
                Ingredient ing = findIngredientById(comp.getIngredient().getId());
                double required = doLine.getQuantity() * comp.getQuantity();

                try {
                    double requiredKG = UnitConverter.convertTo(ing.getName(), comp.getUnit(), UnitType.KG, required);
                    double currentKG = ing.getStockValueAt(Instant.now()).getQuantity();
                    if (currentKG < requiredKG) {
                        errorMsg.append(String.format(
                                "Ingrédient '%s' insuffisant (besoin: %.2f KG, disponible: %.2f)%n",
                                ing.getName(), requiredKG, currentKG
                        ));
                    }
                } catch (IllegalArgumentException e) {
                    errorMsg.append("Convertion impossible: ").append(e.getMessage());
                }

            }
        }

        if (errorMsg.length() > 0) {
            throw new RuntimeException("Stock insuffisant :\n" + errorMsg);
        }
    }

    private String generateOrderReference(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(reference FROM 4) AS INT)), 0) + 1 FROM \"order\"";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int next = rs.getInt(1);
                return String.format("ORD%05d", next);
            }
            return "ORD00001";
        }
    }

    private void insertOrder(Order order, Connection conn) throws SQLException {
        String sql = "INSERT INTO \"order\" (reference) VALUES (?) RETURNING id, creation_datetime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getReference());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    order.setId(rs.getInt("id"));
                    order.setCreationDateTime(rs.getTimestamp("creation_datetime").toInstant());
                } else {
                    throw new SQLException("Échec insertion commande");
                }
            }
        }
    }

    private void insertDishOrderLines(Order order, Connection conn) throws SQLException {
        String sql = "INSERT INTO dish_order (id_order, id_dish, quantity) VALUES (?, ?, ?) RETURNING id";
        for (DishOrder line : order.getDishOrders()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, order.getId());
                ps.setInt(2, line.getDish().getId());
                ps.setInt(3, line.getQuantity());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        line.setId(rs.getInt(1));
                    }
                }
                line.setOrder(order);
            }
        }
    }

    public List<DishOrder> findDishOrdersByOrderReference(String reference){
        String sql = """
                select dso.id as dish_order_id, dso.id_dish as dish_order_id_dish,
                       dso.quantity as dish_order_quantity , dso.id_order as dish_order_id_order
                from "order" o
                join dish_order dso on o.id = dso.id_order
                where reference = ?;
                """;
        try(Connection conn = dbConnection.getDBConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reference);
            ResultSet rs = ps.executeQuery();
            List<DishOrder> dishOrders = new ArrayList<>();
            while(rs.next()){
                DishOrder dishOrder = new DishOrder();
                dishOrder.setId(rs.getInt("dish_order_id"));
                Dish dish = findDishById(rs.getInt("dish_order_id_dish"));
                dishOrder.setDish(dish);
                dishOrder.setQuantity(rs.getInt("dish_order_quantity"));
                dishOrders.add(dishOrder);
            }
            return dishOrders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order findOrderByReference(String reference) throws SQLException {
        String sql = """
                select o.id, o.reference, o.creation_datetime
                from "order" o
                where reference = ?;
                """;
        try(Connection conn = dbConnection.getDBConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reference);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                List<DishOrder> dishOrders = new ArrayList<>();
                dishOrders = findDishOrdersByOrderReference(reference);
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setReference(rs.getString("reference"));
                order.setCreationDateTime(rs.getTimestamp("creation_datetime").toInstant());
                order.setDishOrders(dishOrders);
                return order;
            }
            throw new RuntimeException("Order not found with reference: " + reference);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public StockValue getStockValueAt(Instant t, Integer ingId) throws SQLException {
        String sql = """
                select id_ingredient,
                       sum(
                               case
                                   when type = 'IN' then quantity
                                   else quantity * (-1)
                                   end
                       ) as quantity, unit
                from stock_movement sm
                         join ingredient i on sm.id_ingredient = i.id
                where i.id = ?
                  and creation_datetime <= ?
                group by (id_ingredient, unit)
                """;

        StockValue stockValue = new StockValue();
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingId);
            ps.setTimestamp(2, Timestamp.from(t));
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    stockValue.setQuantity(rs.getDouble("quantity"));
                    stockValue.setUnit(UnitType.valueOf(rs.getString("unit")));
                }
                else {
                    stockValue.setQuantity(0.0);
                }
            }
        }
        return stockValue;
    }

    public Double getDishCost(Integer dishId) throws SQLException {
        String sql= """
                select d.name, sum(i.price * di.required_quantity) as dish_cost
                from dish_ingredient di
                         join dish d on di.id_dish = d.id
                         join ingredient i on i.id = di.id_ingredient
                where d.id = ?
                group by (d.name);
                """;
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            try(ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    return rs.getDouble("dish_cost");
                }
                else {
                    return 0.0;
                }
            }
        }
    }

    public Double getGrossMargin(Integer dishId) throws SQLException {
        String sql= """
                select d.name,
                       d.selling_price - (select sum(i.price * di.required_quantity)
                                          from dish_ingredient di
                                                   join ingredient i on i.id = di.id_ingredient
                                          where di.id_dish = ?) as margin
                from dish d
                where d.id = ?;
                """;
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ps.setInt(2, dishId);
            try(ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    return rs.getDouble("margin");
                }
                else {
                    return 0.0;
                }
            }
        }
    }

}
