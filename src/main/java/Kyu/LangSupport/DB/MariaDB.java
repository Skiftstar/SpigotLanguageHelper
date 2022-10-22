package Kyu.LangSupport.DB;

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

    private String user, password, url;
    private MariaDbDataSource dataSource;
    private boolean storeMessagesInDB;

    private LanguageHelper helperInstance;

    public MariaDB(String host, int port, String user, String password, String database, boolean storeMessagesInDB) {
        super();
        this.user = user;
        this.password = password;
        this.storeMessagesInDB = storeMessagesInDB;
        this.helperInstance = LanguageHelper.getInstance(); 
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
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE langusers SET lang = ? WHERE uuid = ?;")) {
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
        try (PreparedStatement stmt = conn.prepareStatement("SELECT lang FROM langusers WHERE uuid = ?;")) {
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
                        .prepareStatement("INSERT INTO langusers(uuid, lang) VALUES(?, ?);");
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
    public boolean hasKey(String language, String messageKey) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM translations WHERE language = ? AND key = ?;")) {
            stmt.setString(1, language);
            stmt.setString(2, helperInstance.getPlugin().getName() + NAME_SPACER + messageKey);
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
    public void setLore(String language, String key, List<String> lore) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO translations (key, language, lore) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE lore=?;")) {
            stmt.setString(1, helperInstance.getPlugin().getName() + NAME_SPACER + key);
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
    public void setMessage(String language, String key, String message) {
        Connection conn = getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO translations (key, language, message) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE message=?;")) {
            stmt.setString(1, helperInstance.getPlugin().getName() + NAME_SPACER + key);
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
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM translations WHERE lore IS NOT NULL ORDER BY language ASC;")) {
            ResultSet rs = stmt.executeQuery();

            String currLang = "";
            Map<String, List<String>> currMap = new HashMap<>();
            while (rs.next()) {
                String language = rs.getString("language");
                String key = rs.getString("key").split(NAME_SPACER)[1];
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
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM translations WHERE message IS NOT NULL ORDER BY language ASC;")) {
            ResultSet rs = stmt.executeQuery();

            String currLang = "";
            Map<String, String> currMap = new HashMap<>();
            while (rs.next()) {
                String language = rs.getString("language");
                String key = rs.getString("key").split(NAME_SPACER)[1];
                String message = rs.getString("message");

                if (!currLang.equals(language)) {
                    if (currMap.size() > 0) {
                        messages.put(currLang, currMap);
                    }
                    currMap = new HashMap<>();
                    currLang = language;
                }
                currMap.put(key, message);
            }
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