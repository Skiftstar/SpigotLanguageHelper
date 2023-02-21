package Kyu.LangSupport.DB;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

public interface DB {

    static String NAME_SPACER = "::::";

    public void init();

    public void updateUser(String uuid, String newLang);

    public void setupPlayer(Player p);

    public boolean isStoreMessagesInDB();

    public boolean hasKey(String language, String topKey, String msgKey);

    public void setLore(String language, String topKey, String msgKey, List<String> lore);

    public void setMessage(String language, String topKey, String msgKey, String message);

    public Map<String, Map<String, List<String>>> getLores();

    public Map<String, Map<String, String>> getMessages();

}
