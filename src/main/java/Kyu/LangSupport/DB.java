package Kyu.LangSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bson.Document;
import org.bukkit.entity.Player;
import org.mariadb.jdbc.MariaDbDataSource;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public interface DB {

    public void updateUser(String uuid, String newLang);

    public void setupPlayer(Player p);

    class MongoDB implements DB {

        private String uri;
        private String database;

        public MongoDB(String uri, String database) {
            this.uri = uri;
            this.database = database;
        }

        public void updateUser(String uuid, String newLang) {
            MongoClient client = getConnection();
            MongoDatabase db = client.getDatabase(database);
            MongoCollection<Document> collection = db.getCollection("langusers");
            collection.updateOne(Filters.eq("uuid", uuid), Filters.eq("lang", newLang), null);
            client.close();
        }

        public void setupPlayer(Player p) {
            MongoClient client = getConnection();
            MongoDatabase db = client.getDatabase(database);
            MongoCollection<Document> collection = db.getCollection("langusers");

            Document doc = collection.find(Filters.eq("uuid", p.getUniqueId().toString())).first();
            if (!doc.isEmpty()) {
                String lang = doc.getString("lang");
                LanguageHelper.playerLangs.put(p.getUniqueId(), lang);
                client.close();
                return;
            }

            String gameLanguage = p.locale().getLanguage().split("_")[0];
            String defaultLang = LanguageHelper.defaultLang;
            if (LanguageHelper.messages.get(gameLanguage) != null) {
                defaultLang = gameLanguage;
            }

            collection.insertOne(new Document("uuid", p.getUniqueId().toString()).append("lang", defaultLang));

            LanguageHelper.pLangConf.set(p.getUniqueId().toString(), defaultLang);
            LanguageHelper.saveConfig(LanguageHelper.pLangConf, LanguageHelper.pLangFile);
            LanguageHelper.playerLangs.put(p.getUniqueId(), defaultLang);
            p.sendMessage(LanguageHelper.instance.getMess(p, "NoLangSet", true).replace("%default", defaultLang));
            client.close();
        }

        private MongoClient getConnection() {
            return MongoClients.create(uri);
        }

    }

    class MariaDB implements DB {

        private String user, password, url;
        private MariaDbDataSource dataSource;

        public MariaDB(String host, int port, String user, String password, String database) {
            super();
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
                    LanguageHelper.playerLangs.put(p.getUniqueId(), lang);
                } else {
                    String gameLanguage = p.locale().getLanguage().split("_")[0];
                    String defaultLang = LanguageHelper.defaultLang;
                    if (LanguageHelper.messages.get(gameLanguage) != null) {
                        defaultLang = gameLanguage;
                    }

                    PreparedStatement statemt = conn
                            .prepareStatement("INSERT INTO langusers(uuid, lang) VALUES(?, ?);");
                    statemt.setString(1, p.getUniqueId().toString());
                    statemt.setString(2, defaultLang);
                    statemt.execute();
                    statemt.close();

                    LanguageHelper.pLangConf.set(p.getUniqueId().toString(), defaultLang);
                    LanguageHelper.saveConfig(LanguageHelper.pLangConf, LanguageHelper.pLangFile);
                    LanguageHelper.playerLangs.put(p.getUniqueId(), defaultLang);
                    p.sendMessage(
                            LanguageHelper.instance.getMess(p, "NoLangSet", true).replace("%default", defaultLang));
                }
                conn.close();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
