package Kyu.LangSupport;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageJoinListener implements Listener {

    public MessageJoinListener(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent e) {
        LanguageHelper.setupPlayer(e.getPlayer());
    }

    @EventHandler
    private void onLeave(PlayerQuitEvent e) {
        LanguageHelper.remPlayer(e.getPlayer());
    }

}