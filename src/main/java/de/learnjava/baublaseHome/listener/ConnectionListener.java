package de.learnjava.baublaseHome.listener;

import de.learnjava.baublaseHome.BaublaseHome;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        BaublaseHome plugin = BaublaseHome.getInstance();
        if (!plugin.getSavingMethod().equalsIgnoreCase("mysql")) return;

        var uuid = event.getPlayer().getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getHomeRepository().findAllHomes(uuid);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BaublaseHome plugin = BaublaseHome.getInstance();
        plugin.getPlayerHomes().remove(event.getPlayer().getUniqueId());
    }
}