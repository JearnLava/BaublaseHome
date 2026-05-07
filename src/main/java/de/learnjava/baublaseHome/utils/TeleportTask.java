package de.learnjava.baublaseHome.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportTask extends BukkitRunnable {

    public static final int COOLDOWN_SECONDS = 5;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String PREFIX =
            "  <bold><gradient:#FA56AF:#4498DB>Baublase</gradient></bold> <dark_gray>| </dark_gray>";

    private final Player   player;
    private final Location destination;
    private final Location startLocation;
    private final Runnable onCancel;

    private int secondsLeft = COOLDOWN_SECONDS;

    public TeleportTask(Player player, Location destination, Runnable onCancel) {
        this.player      = player;
        this.destination = destination;
        this.startLocation = player.getLocation().clone();
        this.onCancel    = onCancel;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        if (hasMoved()) {
            cancel();
            player.sendActionBar(MM.deserialize(PREFIX + "<gray>Teleport wurde abgebrochen.</gray>"));
            onCancel.run();
            return;
        }

        if (secondsLeft <= 0) {
            player.teleport(destination);
            cancel();
            return;
        }

        player.sendActionBar(MM.deserialize(
                PREFIX + "<gray>Teleport in <light_purple>" + secondsLeft + " Sekunden</light_purple><gray>...</gray>"
        ));

        secondsLeft--;
    }

    private boolean hasMoved() {
        Location current = player.getLocation();
        return Math.abs(current.getX() - startLocation.getX()) > 0.1
                || Math.abs(current.getY() - startLocation.getY()) > 0.1
                || Math.abs(current.getZ() - startLocation.getZ()) > 0.1;
    }
}