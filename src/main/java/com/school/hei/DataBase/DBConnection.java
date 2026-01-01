package com.school.hei.DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private String url;
    private String username;
    private String password;

    public DBConnection() {
        this.url = System.getenv("DB_URL");
        this.username = System.getenv("DB_USERNAME");
        this.password = System.getenv("DB_PASSWORD");

        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("DB_URL is empty");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("DB_USERNAME is empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("DB_PASSWORD is empty");
        }
    }

    public Connection getDBConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeDBConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
