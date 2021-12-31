package Kyu.LangSupport;

import java.util.UUID;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class MessageJoinListener implements Listener, PluginMessageListener {

    private LanguageHelper helper;

    public MessageJoinListener(JavaPlugin plugin, LanguageHelper helper) {
        this.helper = helper;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onJoin(PlayerJoinEvent e) {
        helper.setupPlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onLeave(PlayerQuitEvent e) {
        helper.remPlayer(e.getPlayer());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] bytes) {
        if (!channel.equalsIgnoreCase("kyu:language")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();
        System.out.println(subChannel);
        if (subChannel.equalsIgnoreCase("ChangeLanguage")) {
            String string = in.readUTF();
            UUID uuid = UUID.fromString(string.split(";;;")[0]);
            String language = string.split(";;;")[1];
            helper.changeLang(uuid, language);
        }
    }

}