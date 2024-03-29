package Kyu.LangSupport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import Kyu.LangSupport.DB.DB;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

public final class LanguageHelper {

    private String defaultLang;
    private Map<String, Reader> langRessources;

    private String prefix;

    private boolean useDB;
    private DB database;

    private YamlConfiguration pLangConf;
    private File pLangFile;

    private Map<String, Map<String, String>> messages = new HashMap<>();
    private Map<String, Map<String, List<String>>> lores = new HashMap<>();

    private Map<UUID, String> playerLangs = new HashMap<>();

    private JavaPlugin plugin;

    private boolean sendNoLangSetMess = true;

    private static LanguageHelper instance;

    public LanguageHelper(JavaPlugin plugin, String defaultLang, Reader langResource, String resourceName, String prefix, DB... database) {
        LanguageHelper.instance = this;
        this.plugin = plugin;
        this.defaultLang = defaultLang;
        this.langRessources = new HashMap<>() {{ put(resourceName, langResource); }};
        this.prefix = prefix;
        if (database.length > 0) {
            this.useDB = true;
            this.database = database[0];
            this.database.init();
        }

        setup();
    }

    public LanguageHelper(JavaPlugin plugin, String defaultLang, Map<String, Reader> langResources, String prefix, DB... database) {
        LanguageHelper.instance = this;
        this.plugin = plugin;
        this.defaultLang = defaultLang;
        this.langRessources = langResources;
        this.prefix = prefix;
        if (database.length > 0) {
            this.useDB = true;
            this.database = database[0];
            this.database.init();
        }

        setup();
    }

    public void setup() {
        pLangFile = new File(plugin.getDataFolder(), "playerLangs.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
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

        if (isUseDB() && this.database.isStoreMessagesInDB()) {
            loadMessagesDB();
        } else {
            loadMessagesLocal();
        }
        MessageJoinListener listener = new MessageJoinListener(plugin, this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "my:channel", listener);
    }

    public void setSendNoLangSetMess(boolean sendNoLangSetMess) {
        this.sendNoLangSetMess = sendNoLangSetMess;
    }

    private void loadMessagesDB() {
        updateLangsDB();
        messages = this.database.getMessages();
        lores = this.database.getLores();
    }

    private void loadMessagesLocal() {
        updateLangsLocal();

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

    private void updateLangsDB() {
        for (String langRessourceName : langRessources.keySet()) {
            YamlConfiguration refConf = YamlConfiguration.loadConfiguration(langRessources.get(langRessourceName));
            final String langName = langRessourceName;
            for (String topKey : refConf.getKeys(false)) {
                for (String mess : refConf.getConfigurationSection(topKey).getKeys(false)) {
                    if (!database.hasKey(langName, topKey, mess)) {
                        if (topKey.toLowerCase().contains("lores")) {
                            database.setLore(langName, topKey, mess, refConf.getStringList(topKey + "." + mess));
                        } else {
                            database.setMessage(langName, topKey, mess, refConf.getString(topKey + "." + mess));
                        }
                    }
                }
            }
        }
    }

    private void updateLangsLocal() {
        for (String langResourceName : langRessources.keySet()) {
            YamlConfiguration refConf = YamlConfiguration.loadConfiguration(langRessources.get(langResourceName));
            final String langName = langResourceName;

            File file = new File(plugin.getDataFolder(), "locales/" + langName);
            if (!file.exists()) {
                try {
                    Files.copy(plugin.getResource(langName), file.toPath());
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }

            YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(file);
            for (String topKey : refConf.getKeys(false)) {
                for (String mess : refConf.getConfigurationSection(topKey).getKeys(false)) {
                    if (langConfig.get(topKey + "." + mess) == null) {
                        if (topKey.toLowerCase().contains("lores")) {
                            langConfig.set(topKey + "." + mess, refConf.getStringList(topKey + "." + mess));
                        } else {
                            langConfig.set(topKey + "." + mess, refConf.getString(topKey + "." + mess));
                        }
                    }
                }
            }
            saveConfig(langConfig, file);
        }
    }

    public void sendMess(Player p, String messageKey, boolean usePrefix, Map<String, String> placeholders) {
        String message = getMess(p, messageKey, usePrefix);
        for (String placeholder : placeholders.keySet()) {
            message = message.replace(placeholder, placeholders.get(placeholder));
        }
        send(p, message);
    }

    /**
     * Same as sendMess but replaces generic {} placeholder so no map is needed
     * @param p
     * @param messageKey
     * @param usePrefix
     * @param placeholders
     */
    public void sendMess(Player p, String messageKey, boolean usePrefix, String... placeholders) {
        String message = getMess(p, messageKey, usePrefix);
        if (placeholders.length > 0) {
            for (String placeholder : placeholders) {
                message = message.replaceFirst("{}", placeholder);
            }
        }
        send(p, message);
    }

    public void sendMess(Player p, String messageKey, boolean usePrefix) {
        send(p, getMess(p, messageKey, usePrefix));
    }

    private void send(Player p, String s) {
        p.sendMessage(s);
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
        return getMess(pLang, messageKey, usePrefix);
    }

    public String getMess(String language, String messageKey, boolean... usePrefix) {
        String message;
        if (!messages.containsKey(language)) {
            message = messages.get(defaultLang).getOrDefault(messageKey,
                    color("&cMessage &4" + messageKey + "&c not found!"));
        } else {
            message = messages.get(language).getOrDefault(messageKey,
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
                if (sendNoLangSetMess) p.sendMessage(getMess(p, "NoLangSet", true).replace("%default", defaultLang));
            } else {
                String lang = pLangConf.getString(p.getUniqueId().toString());
                playerLangs.put(p.getUniqueId(), lang);
            }
        } else {
            this.database.setupPlayer(p);
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

    public void saveConfig(YamlConfiguration config, File toSave) {
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
            this.database.updateUser(p.toString(), newLang);
        }
    }

    public void remPlayer(Player p) {
        playerLangs.remove(p.getUniqueId());
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public Map<UUID, String> getPlayerLangs() {
        return playerLangs;
    }

    public Map<String, Map<String, String>> getMessages() {
        return messages;
    }

    public Map<String, Map<String, List<String>>> getLores() {
        return lores;
    }

    public YamlConfiguration getpLangConf() {
        return pLangConf;
    }

    public File getpLangFile() {
        return pLangFile;
    }

    public boolean isUseDB() {
        return useDB;
    }

    public DB getDatabase() {
        return database;
    }

    public Set<String> getLanguages() {
        return messages.keySet();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public static LanguageHelper getInstance() {
        return instance;
    }

    public boolean isSendNoLangSetMess() {
        return sendNoLangSetMess;
    }
}
