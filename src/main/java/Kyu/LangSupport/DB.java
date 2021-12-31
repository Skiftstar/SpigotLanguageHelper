package Kyu.LangSupport;

import java.sql.Connection;
import java.sql.SQLException;

import org.mariadb.jdbc.MariaDbDataSource;

public class DB {
    private String user, password, url;
    private MariaDbDataSource dataSource;

    public DB(String host, int port, String user, String password, String database) {
        this.user = user;
        this.password = password;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        try {
            url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
            dataSource = new MariaDbDataSource(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection(user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
