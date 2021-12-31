package Kyu.LangSupport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class LanguageHelper {

    private String defaultLang;
    private Reader defaultLangResource;

    private String prefix;

    private boolean useDB;
    private DB database;

    private YamlConfiguration pLangConf;
    private File pLangFile;

    private Map<String, Map<String, String>> messages = new HashMap<>();
    private Map<String, Map<String, List<String>>> lores = new HashMap<>();

    private Map<UUID, String> playerLangs = new HashMap<>();

    private JavaPlugin plugin;

    public LanguageHelper(JavaPlugin plugin, String defaultLang, Reader langResource, String prefix, boolean useDB) {
        this.plugin = plugin;
        this.useDB = useDB;
        this.defaultLang = defaultLang;
        this.defaultLangResource = langResource;
        this.prefix = prefix;

        setup();
    }

    public void setDatabase(DB database) {
        this.database = database;
    }

    public void setup() {
        pLangFile = new File(plugin.getDataFolder(), "playerLangs.yml");
        if (!pLangFile.exists()) {
            try {
                pLangFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        pLangConf = YamlConfiguration.loadConfiguration(pLangFile);

        File folder = new File(plugin.getDataFolder() + "/locales");
        if (!folder.exists()) {
            folder.mkdir();
        }

        loadMessages();
        MessageJoinListener listener = new MessageJoinListener(plugin, this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "my:channel", listener);
    }

    private void loadMessages() {
        updateDefaultLangFile();

        File folder = new File(plugin.getDataFolder() + "/locales");
        for (File file : folder.listFiles()) {
            Map<String, String> langMessages = new HashMap<>();
            Map<String, List<String>> langLores = new HashMap<>();
            String name = file.getName().split(".yml")[0];
            YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
            for (String key : conf.getKeys(false)) {
                for (String messageKey : conf.getConfigurationSection(key).getKeys(false)) {
                    if (key.toLowerCase().contains("lores")) {
                        List<String> lore = new ArrayList<>();
                        for (String line : conf.getStringList(key + "." + messageKey)) {
                            lore.add(color(line));
                        }
                        langLores.put(messageKey, lore);
                    } else {
                        String message = color(conf.getString(key + "." + messageKey));
                        langMessages.put(messageKey, message);
                        plugin.getLogger().info("Putting Message " + messageKey + " from " + name + " into map!");
                    }
                }
            }
            lores.put(name, langLores);
            messages.put(name, langMessages);
        }

    }

    private void updateDefaultLangFile() {
        File file = new File(plugin.getDataFolder(), "locales/" + defaultLang + ".yml");
        if (!file.exists()) {
            try {
                Files.copy(plugin.getResource(defaultLang + ".yml"), file.toPath());
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        YamlConfiguration refConf = YamlConfiguration.loadConfiguration(defaultLangResource);
        YamlConfiguration defaultConf = YamlConfiguration.loadConfiguration(file);
        for (String topKey : refConf.getKeys(false)) {
            for (String mess : refConf.getConfigurationSection(topKey).getKeys(false)) {
                if (defaultConf.get(topKey + "." + mess) == null) {
                    if (topKey.toLowerCase().contains("lores")) {
                        defaultConf.set(topKey + "." + mess, refConf.getStringList(topKey + "." + mess));
                    } else {
                        defaultConf.set(topKey + "." + mess, refConf.getString(topKey + "." + mess));
                    }
                }
            }
        }
        saveConfig(defaultConf, file);
    }

    public List<String> getLore(Player p, String loreKey) {
        String pLang;
        if (!playerLangs.containsKey(p.getUniqueId())) {
            pLang = defaultLang;
            setupPlayer(p);
        } else {
            pLang = playerLangs.get(p.getUniqueId());
        }
        if (!lores.containsKey(pLang)) {
            return lores.get(defaultLang).getOrDefault(loreKey,
                    new ArrayList<>(Arrays.asList(color("&cLore &4 " + loreKey + " &c not found!"))));
        } else {
            return lores.get(pLang).getOrDefault(loreKey,
                    new ArrayList<>(Arrays.asList(color("&cLore &4 " + loreKey + " &c not found!"))));
        }
    }

    public List<String> getLore(String loreKey) {
        return lores.get(defaultLang).getOrDefault(loreKey,
                new ArrayList<>(Arrays.asList(color("&cLore &4 " + loreKey + " &c not found!"))));
    }

    public String getMess(Player p, String messageKey, boolean... usePrefix) {
        String pLang;
        if (!playerLangs.containsKey(p.getUniqueId())) {
            pLang = defaultLang;
            setupPlayer(p);
        } else {
            pLang = playerLangs.get(p.getUniqueId());
        }
        String message;
        if (!messages.containsKey(pLang)) {
            message = messages.get(defaultLang).getOrDefault(messageKey,
                    color("&cMessage &4" + messageKey + "&c not found!"));
        } else {
            message = messages.get(pLang).getOrDefault(messageKey,
                    color("&cMessage &4" + messageKey + "&c not found!"));
        }
        if (usePrefix.length > 0 && usePrefix[0]) {
            message = prefix + message;
        }
        return message;
    }

    public String getMess(String messageKey, boolean... usePrefix) {
        String message = messages.get(defaultLang).getOrDefault(messageKey,
                color("&cMessage &4" + messageKey + "&c not found!"));
        if (usePrefix.length > 0 && usePrefix[0]) {
            message = prefix + message;
        }
        return message;
    }

    public void setupPlayer(Player p) {
        if (!isUseDB()) {
            if (pLangConf.get(p.getUniqueId().toString()) == null) {
                String gameLanguage = p.locale().getLanguage().split("_")[0];
                String defaultLang = this.defaultLang;
                if (messages.get(gameLanguage) != null) {
                    defaultLang = gameLanguage;
                }

                pLangConf.set(p.getUniqueId().toString(), defaultLang);
                saveConfig(pLangConf, pLangFile);
                playerLangs.put(p.getUniqueId(), defaultLang);
                p.sendMessage(getMess(p, "NoLangSet", true).replace("%default", defaultLang));
            } else {
                String lang = pLangConf.getString(p.getUniqueId().toString());
                playerLangs.put(p.getUniqueId(), lang);
            }
        } else {
            Connection conn = database.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT lang FROM langusers WHERE uuid = ?;")) {
                stmt.setString(1, p.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String lang = rs.getString("lang");
                    playerLangs.put(p.getUniqueId(), lang);
                } else {
                    String gameLanguage = p.locale().getLanguage().split("_")[0];
                    String defaultLang = this.defaultLang;
                    if (messages.get(gameLanguage) != null) {
                        defaultLang = gameLanguage;
                    }

                    PreparedStatement statemt = conn.prepareStatement("INSERT INTO langusers(uuid, lang) VALUES(?, ?);");
                    statemt.setString(1, p.getUniqueId().toString());
                    statemt.setString(2, defaultLang);
                    statemt.execute();
                    statemt.close();

                    pLangConf.set(p.getUniqueId().toString(), defaultLang);
                    saveConfig(pLangConf, pLangFile);
                    playerLangs.put(p.getUniqueId(), defaultLang);
                    p.sendMessage(getMess(p, "NoLangSet", true).replace("%default", defaultLang));
                }
                conn.close();
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getLanguage(Player p) {
        String language = null;
        try {
            Object ep = getMethod("getHandle", p.getClass()).invoke(p, (Object[]) null);
            Field f = ep.getClass().getDeclaredField("locale");
            f.setAccessible(true);
            language = (String) f.get(ep);
            language = language.split("_")[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return language;
    }

    private Method getMethod(String name, Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name))
                return m;
        }
        return null;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private void saveConfig(YamlConfiguration config, File toSave) {
        try {
            config.save(toSave);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void changeLang(UUID p, String newLang) {
        playerLangs.remove(p);
        playerLangs.put(p, newLang);
        pLangConf.set(p.toString(), newLang);
        saveConfig(pLangConf, pLangFile);

        if (isUseDB()) {
            Connection conn = database.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE langusers SET lang = ? WHERE uuid = ?;")) {
                stmt.setString(1, newLang);
                stmt.setString(2, p.toString());
                stmt.executeUpdate();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void remPlayer(Player p) {
        playerLangs.remove(p.getUniqueId());
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public boolean isUseDB() {
        return useDB;
    }

    public DB getDatabase() {
        return database;
    }
}
