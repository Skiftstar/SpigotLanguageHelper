package Kyu.LangSupport.DB;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import Kyu.LangSupport.LanguageHelper;

public class MongoDB implements DB {

    private String uri;
    private String database;
    private boolean storeMessagesInDB;

    private LanguageHelper helperInstance;

    public MongoDB(String uri, String database, boolean storeMessagesInDB) {
        this.uri = uri;
        this.database = database;
        this.storeMessagesInDB = storeMessagesInDB;
        this.helperInstance = LanguageHelper.getInstance();
    }

    public boolean isStoreMessagesInDB() {
        return storeMessagesInDB;
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
            helperInstance.getPlayerLangs().put(p.getUniqueId(), lang);
            client.close();
            return;
        }

        String gameLanguage = p.locale().getLanguage().split("_")[0];
        String defaultLang = helperInstance.getDefaultLang();
        if (helperInstance.getMessages().get(gameLanguage) != null) {
            defaultLang = gameLanguage;
        }

        collection.insertOne(new Document("uuid", p.getUniqueId().toString()).append("lang", defaultLang));

        helperInstance.getpLangConf().set(p.getUniqueId().toString(), defaultLang);
        helperInstance.saveConfig(helperInstance.getpLangConf(), helperInstance.getpLangFile());
        helperInstance.getPlayerLangs().put(p.getUniqueId(), defaultLang);
        p.sendMessage(helperInstance.getMess(p, "NoLangSet", true).replace("%default", defaultLang));
        client.close();
    }

    private MongoClient getConnection() {
        return MongoClients.create(uri);
    }

    @Override
    public boolean hasKey(String language, String key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setLore(String language, String key, List<String> lore) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setMessage(String language, String key, String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Map<String, Map<String, List<String>>> getLores() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Map<String, String>> getMessages() {
        // TODO Auto-generated method stub
        return null;
    }

}