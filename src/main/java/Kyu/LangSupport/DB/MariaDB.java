package Kyu.LangSupport.DB;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.mariadb.jdbc.MariaDbDataSource;

import Kyu.LangSupport.LanguageHelper;

public class MariaDB implements DB {

    private String user, password, url, host, database;
    private int port;
    private MariaDbDataSource dataSource;
    private boolean storeMessagesInDB;

    private LanguageHelper helperInstance;

    public MariaDB(String host, int port, String user, String password, String database, boolean storeMessagesInDB) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;
        this.database = database;
        this.storeMessagesInDB = storeMessagesInDB;
    }

    public void init() {
        this.helperInstance = LanguageHelper.getInstance(); 
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        try {
            url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
            dataSource = new MariaDbDataSource(url);
            initDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initDb() throws SQLException, IOException {
        String[] queries = new String[]{"CREATE TABLE IF NOT EXISTS userLangs("
            + "lang char(50) NOT NULL,"
            + "uuid char(36) NOT NULL,"
            + "PRIMARY KEY (uuid)"
        + ");",
        
        "CREATE TABLE IF NOT EXISTS translations("
          + "lang char(50) NOT NULL,"
          + "messKey char(255) NOT NULL,"
          + "msg LONGTEXT,"
          + "lore LONGTEXT,"
          + "PRIMARY KEY (messKey, lang)"
        + ");"};
        // execute each query to the database.
        Connection conn = dataSource.getConnection(user, password);
        PreparedStatement stmt = null;
        for (String query : queries) {
            // If you use the legacy way you have to check for empty queries here.
            if (query.isBlank())
                continue;
            stmt = conn.prepareStatement(query);
            System.out.println(query);
            stmt.execute();
            stmt.close();
        }
        conn.close();
        helperInstance.getPlugin().getLogger().info("§2Database setup complete.");
    }

    public boolean isStoreMessagesInDB() {
        return storeMessagesInDB;
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection(user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateUser(String uuid, String newLang) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE userLangs SET lang = ? WHERE uuid = ?;")) {
            stmt.setString(1, newLang);
            stmt.setString(2, uuid);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setupPlayer(Player p) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT lang FROM userLangs WHERE uuid = ?;")) {
            stmt.setString(1, p.getUniqueId().toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String lang = rs.getString("lang");
                helperInstance.getPlayerLangs().put(p.getUniqueId(), lang);
            } else {
                String gameLanguage = p.locale().getLanguage().split("_")[0];
                String defaultLang = helperInstance.getDefaultLang();
                if (helperInstance.getMessages().get(gameLanguage) != null) {
                    defaultLang = gameLanguage;
                }

                PreparedStatement statemt = conn
                        .prepareStatement("INSERT INTO userLangs(uuid, lang) VALUES(?, ?);");
                statemt.setString(1, p.getUniqueId().toString());
                statemt.setString(2, defaultLang);
                statemt.execute();
                statemt.close();

                helperInstance.getpLangConf().set(p.getUniqueId().toString(), defaultLang);
                helperInstance.saveConfig(helperInstance.getpLangConf(), helperInstance.getpLangFile());
                helperInstance.getPlayerLangs().put(p.getUniqueId(), defaultLang);
                p.sendMessage(
                    helperInstance.getMess(p, "NoLangSet", true).replace("%default", defaultLang));
            }
            conn.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasKey(String language, String topKey, String msgKey) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM translations WHERE lang = ? AND messKey = ?;")) {
            stmt.setString(1, language);
            stmt.setString(2, helperInstance.getPlugin().getName() + NAME_SPACER + topKey + NAME_SPACER + msgKey);
            ResultSet rs = stmt.executeQuery();
            stmt.close();
            conn.close();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("ERROR TRYING TO CHECK IF MESSAGE EXISTS IN DATABASE");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setLore(String language, String topKey, String msgKey, List<String> lore) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO translations (messKey, lang, lore) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE lore=?;")) {
            stmt.setString(1, helperInstance.getPlugin().getName() + NAME_SPACER + topKey + NAME_SPACER + msgKey);
            stmt.setString(2, language);
            stmt.setString(3, String.join("\n", lore));
            stmt.setString(4, String.join("\n", lore));
            stmt.executeQuery();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("ERROR TRYING TO UPDATE LORE IN DATABASE");
            e.printStackTrace();
        }
    }

    @Override
    public void setMessage(String language, String topKey, String msgKey, String message) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO translations (messKey, lang, msg) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE msg=?;")) {
            stmt.setString(1, helperInstance.getPlugin().getName() + NAME_SPACER + topKey + NAME_SPACER + msgKey);
            stmt.setString(2, language);
            stmt.setString(3, String.join("\n", message));
            stmt.setString(4, String.join("\n", message));
            stmt.executeQuery();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println("ERROR TRYING TO UPDATE MESSAGE IN DATABASE");
            e.printStackTrace();
        }  
    }

    @Override
    public Map<String, Map<String, List<String>>> getLores() {
        Map<String, Map<String, List<String>>> lores = new HashMap<>();

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM translations WHERE lore IS NOT NULL ORDER BY lang ASC;")) {
            ResultSet rs = stmt.executeQuery();

            String currLang = "";
            Map<String, List<String>> currMap = new HashMap<>();
            while (rs.next()) {
                String language = rs.getString("lang");
                String key = rs.getString("messKey").split(NAME_SPACER)[2];
                List<String> lore = new ArrayList<>(Arrays.asList(rs.getString("lore").split("\n")));

                if (!currLang.equals(language)) {
                    if (currMap.size() > 0) {
                        lores.put(currLang, currMap);
                    }
                    currMap = new HashMap<>();
                    currLang = language;
                }
                currMap.put(key, lore);
            }
            lores.put(currLang, currMap);
            stmt.close();
            conn.close();
            return lores;
        } catch (SQLException e) {
            System.out.println("ERROR TRYING TO FETCH ALL LORES FROM DATABASE");
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Map<String, String>> getMessages() {
        Map<String, Map<String, String>> messages = new HashMap<>();

        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM translations WHERE msg IS NOT NULL ORDER BY lang ASC;")) {
            ResultSet rs = stmt.executeQuery();

            String currLang = "";
            Map<String, String> currMap = new HashMap<>();
            while (rs.next()) {
                String language = rs.getString("lang");
                String key = rs.getString("messKey").split(NAME_SPACER)[2];
                String message = rs.getString("msg");

                if (!currLang.equals(language)) {
                    if (currMap.size() > 0) {
                        messages.put(currLang, currMap);
                    }
                    currMap = new HashMap<>();
                    currLang = language;
                }
                currMap.put(key, message);
            }
            messages.put(currLang, currMap);
            stmt.close();
            conn.close();
            return messages;
        } catch (SQLException e) {
            System.out.println("ERROR TRYING TO FETCH ALL MESSAGES FROM DATABASE");
            e.printStackTrace();
            return new HashMap<>();
        }
    }

}