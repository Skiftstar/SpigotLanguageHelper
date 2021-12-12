package Kyu.LangSupport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.*;

public final class LanguageHelper {

    private static String defaultLang;
    private static Reader defaultLangResource;

    private static String prefix;

    private static YamlConfiguration pLangConf;
    private static File pLangFile;

    private static Map<String, Map<String, String>> messages = new HashMap<>();
    private static Map<String, Map<String, List<String>>> lores = new HashMap<>();

    private static Map<Player, String> playerLangs = new HashMap<>();

    private static JavaPlugin plugin;

    public static void setup(JavaPlugin plugin, String defaultLang, Reader langResource, String prefix) {
        LanguageHelper.plugin = plugin;
        LanguageHelper.defaultLang = defaultLang;
        LanguageHelper.defaultLangResource = langResource;
        LanguageHelper.prefix = prefix;

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
        new MessageJoinListener(plugin);
    }

    private static void loadMessages() {
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
                    }
                }
            }
            lores.put(name, langLores);
            messages.put(name, langMessages);
        }

    }

    private static void updateDefaultLangFile() {
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


    public static List<String> getLore(Player p, String loreKey) {
        String pLang;
        if (!playerLangs.containsKey(p)) {
            pLang = "en";
            setupPlayer(p);
        } else {
            pLang = playerLangs.get(p);
        }
        if (!lores.containsKey(pLang)) {
            return lores.get("en").getOrDefault(loreKey, new ArrayList<>(Arrays.asList(color("&cLore &4 " + loreKey + " &c not found!"))));
        } else {
            return lores.get(pLang).getOrDefault(loreKey, new ArrayList<>(Arrays.asList(color("&cLore &4 " + loreKey + " &c not found!"))));
        }
    }

    public static List<String> getLore(String loreKey) {
        return lores.get("en").getOrDefault(loreKey, new ArrayList<>(Arrays.asList(color("&cLore &4 " + loreKey + " &c not found!"))));
    }

    public static String getMess(Player p, String messageKey, boolean... usePrefix) {
        String pLang;
        if (!playerLangs.containsKey(p)) {
            pLang = "en";
            setupPlayer(p);
        } else {
            pLang = playerLangs.get(p);
        }
        String message;
        if (!messages.containsKey(pLang)) {
            message = messages.get("en").getOrDefault(messageKey, color("&cMessage &4" + messageKey + "&c not found!"));
        } else {
            message = messages.get(pLang).getOrDefault(messageKey, color("&cMessage &4" + messageKey + "&c not found!"));
        }
        if (usePrefix.length > 0 && usePrefix[0]) {
            message = prefix + message;
        }
        return message;
    }

    public static String getMess(String messageKey, boolean... usePrefix) {
        String message = messages.get("en").getOrDefault(messageKey, color("&cMessage &4" + messageKey + "&c not found!"));
        if (usePrefix.length > 0 && usePrefix[0]) {
            message = prefix + message;
        }
        return message;
    }

    public static void setupPlayer(Player p) {
        if (pLangConf.get(p.getUniqueId().toString()) == null) {
            p.sendMessage(getMess("NoLangSet", true).replace("%default", defaultLang));
            pLangConf.set(p.getUniqueId().toString(), defaultLang);
            saveConfig(pLangConf, pLangFile);
            playerLangs.put(p, defaultLang);
        } else {
            String lang = pLangConf.getString(p.getUniqueId().toString());
            playerLangs.put(p, lang);
        }
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static void saveConfig(YamlConfiguration config, File toSave) {
        try {
            config.save(toSave);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void changeLang(Player p, String newLang) {
        playerLangs.remove(p);
        playerLangs.put(p, newLang);
        pLangConf.set(p.getUniqueId().toString(), newLang);
        saveConfig(pLangConf, pLangFile);
    }

    public static void remPlayer(Player p) {
        playerLangs.remove(p);
    }

    public static String getDefaultLang() {
        return defaultLang;
    }
}
