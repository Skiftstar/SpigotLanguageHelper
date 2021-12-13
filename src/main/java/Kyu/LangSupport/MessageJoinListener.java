package Kyu.LangSupport;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageJoinListener implements Listener {

    private LanguageHelper helper;

    public MessageJoinListener(JavaPlugin plugin, LanguageHelper helper) {
        this.helper = helper;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent e) {
        helper.setupPlayer(e.getPlayer());
    }

    @EventHandler
    private void onLeave(PlayerQuitEvent e) {
        helper.remPlayer(e.getPlayer());
    }

}