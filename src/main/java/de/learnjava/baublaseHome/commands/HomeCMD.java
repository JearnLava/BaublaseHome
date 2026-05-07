package de.learnjava.baublaseHome.commands;

import de.learnjava.baublaseHome.BaublaseHome;
import de.learnjava.baublaseHome.database.dto.HomeObject;
import de.learnjava.baublaseHome.utils.HomeUtils;
import de.learnjava.baublaseHome.utils.TeleportTask;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HomeCMD implements CommandExecutor, TabCompleter {

    private final MiniMessage mm    = MiniMessage.miniMessage();
    private final HomeUtils   utils = new HomeUtils();

    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "list" -> listHomes(player);

            case "set" -> {
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                setHome(player, args[1]);
            }

            case "delete", "del" -> {
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                deleteHome(player, args[1]);
            }

            default -> {
                String homeName = args[0];
                if (args.length == 1) {
                    teleportToHome(player, homeName);
                } else if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
                    addPlayerToHome(player, homeName, args[2]);
                } else if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                    removePlayerFromHome(player, homeName, args[2]);
                } else {
                    sendUsage(player);
                }
            }
        }

        return true;
    }

    public void listHomes(Player player) {
        Map<String, HomeObject> homes = utils.getAllHomes(player);

        if (homes.isEmpty()) {
            utils.send(player, "<gray>Du hast noch keine Homes gesetzt.</gray>");
            return;
        }

        StringJoiner joiner = new StringJoiner("<dark_gray>, </dark_gray>");
        for (String name : homes.keySet()) {
            joiner.add("<click:run_command:'/home " + name + "'><hover:show_text:'<gray>Klicke um dich zu teleportieren.</gray>'><light_purple>" + name + "</light_purple></hover></click>");
        }

        utils.send(player, "<gray>Deine Homes: </gray>" + joiner);
    }

    private void setHome(Player player, String name) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        String perm = plugin.getConfig().getString("permissions.create");
        if (perm != null && !player.hasPermission(perm)) {
            utils.send(player, plugin.getConfig().getString("messages.no_perms",
                    "<gray>Du hast keine Berechtigung.</gray>"));
            return;
        }

        HomeUtils.NameResult nameResult = utils.validateName(name);
        if (nameResult == HomeUtils.NameResult.TOO_LONG) {
            utils.send(player, "<gray>Du kannst nur 10 Zeichen nutzen! Bitte versuche es erneut!</gray>");
            return;
        }
        if (nameResult == HomeUtils.NameResult.INVALID_CHARS) {
            utils.send(player, "<gray>Ungültiges Zeichen. Bitte versuche es erneut!</gray>");
            return;
        }

        int maxHomes     = utils.getMaxHomes(player);
        int currentHomes = utils.getCurrentHomes(player);

        if (maxHomes > 0 && currentHomes >= maxHomes) {
            utils.send(player, "<gray>Du hast bereits alle <light_purple>" + maxHomes
                    + "</light_purple> <gray>Homes gesetzt.</gray>");
            return;
        }

        utils.send(player, "<gray>Du hast </gray><green>erfolgreich</green> <gray>dein Home <light_purple>" + name + "</light_purple> <gray>gesetzt.</gray>");
        utils.saveHome(player, name, player.getLocation());
    }

    private void deleteHome(Player player, String name) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        String perm = plugin.getConfig().getString("permissions.delete");
        if (perm != null && !player.hasPermission(perm)) {
            utils.send(player, plugin.getConfig().getString("messages.no_perms",
                    "<gray>Du hast keine Berechtigung.</gray>"));
            return;
        }

        boolean deleted = utils.deleteHome(player, name);

        if (!deleted) {
            utils.send(player, "<gray>Du hast kein Home mit dem Namen <light_purple>"
                    + name + "</light_purple><gray>.</gray>");
            return;
        }

        utils.send(player, "<gray>Du hast <light_purple>" + name
                + "</light_purple> <gray>gelöscht.</gray>");
    }

    private void teleportToHome(Player player, String name) {
        BukkitTask existing = pendingTeleports.remove(player.getUniqueId());
        if (existing != null) existing.cancel();

        Optional<HomeObject> homeOpt = utils.getHome(player, name);

        if (homeOpt.isEmpty()) {
            utils.send(player, "<gray>Du hast kein Home mit dem Namen <light_purple>"
                    + name + "</light_purple><gray>.</gray>");
            return;
        }

        Location destination = utils.deserializeLocation(homeOpt.get().getLocationFromString());
        if (destination == null) {
            utils.send(player, "<gray>Es ist etwas schiefgelaufen. Bitte versuche es erneut!</gray>");
            return;
        }

        utils.send(player, "<gray>Du wirst zu <light_purple>" + name
                + "</light_purple> <gray>teleportiert. Bitte warte "
                + TeleportTask.COOLDOWN_SECONDS + " Sekunden.</gray>");

        TeleportTask task = new TeleportTask(player, destination, () -> {
            pendingTeleports.remove(player.getUniqueId());
        });

        BukkitTask bukkitTask = task.runTaskTimer(BaublaseHome.getInstance(), 0L, 20L);
        pendingTeleports.put(player.getUniqueId(), bukkitTask);
    }

    @SuppressWarnings("deprecation")
    private void addPlayerToHome(Player owner, String homeName, String targetName) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        if (utils.getHome(owner, homeName).isEmpty()) {
            utils.send(owner, "<gray>Du hast kein Home mit dem Namen <light_purple>"
                    + homeName + "</light_purple><gray>.</gray>");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            utils.send(owner, "<gray>Der Spieler <light_purple>" + targetName
                    + "</light_purple> <gray>wurde nicht gefunden.</gray>");
            return;
        }

        plugin.getHomeAccessRepository().addAccessAsync(owner.getUniqueId(), homeName, target.getUniqueId());

        utils.send(owner, "<gray>Du hast <light_purple>" + target.getName()
                + "</light_purple> <gray>zu <light_purple>" + homeName
                + "</light_purple> <gray>hinzugefügt.</gray>");
    }

    private void removePlayerFromHome(Player owner, String homeName, String targetName) {
        BaublaseHome plugin = BaublaseHome.getInstance();

        if (utils.getHome(owner, homeName).isEmpty()) {
            utils.send(owner, "<gray>Du hast kein Home mit dem Namen <light_purple>"
                    + homeName + "</light_purple><gray>.</gray>");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            utils.send(owner, "<gray>Der Spieler <light_purple>" + targetName
                    + "</light_purple> <gray>wurde nicht gefunden.</gray>");
            return;
        }

        plugin.getHomeAccessRepository().removeAccessAsync(owner.getUniqueId(), homeName, target.getUniqueId());

        utils.send(owner, "<gray>Du hast <light_purple>" + target.getName()
                + "</light_purple> <gray>von <light_purple>" + homeName
                + "</light_purple> <gray>entfernt.</gray>");
    }

    private void sendUsage(Player player) {
        utils.send(player,
                "<gray>Verwendung:</gray>\n" +
                        "<light_purple>/home <home></light_purple> <gray>- Teleportiere zu einem Home</gray>\n" +
                        "<light_purple>/home set <home></light_purple> <gray>- Setze ein Home</gray>\n" +
                        "<light_purple>/home delete <home></light_purple> <gray>- Lösche ein Home</gray>\n" +
                        "<light_purple>/home list</light_purple> <gray>- Zeige alle Homes</gray>\n" +
                        "<light_purple>/home <home> add <spieler></light_purple> <gray>- Erlaube Zugriff</gray>\n" +
                        "<light_purple>/home <home> remove <spieler></light_purple> <gray>- Entferne Zugriff</gray>"
        );
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        List<String> completes = new ArrayList<>();

        if (args.length == 1) {

            completes.add("set");
            completes.add("delete");
            completes.add("del");
            completes.add("list");

            Map<String, HomeObject> homes = utils.getAllHomes(player);
            completes.addAll(homes.keySet());

            return filter(completes, args[0]);
        }

        if (args.length == 2) {

            if (args[0].equalsIgnoreCase("delete")
                    || args[0].equalsIgnoreCase("del")) {

                Map<String, HomeObject> homes = utils.getAllHomes(player);

                return filter(
                        new ArrayList<>(homes.keySet()),
                        args[1]
                );
            }

            Map<String, HomeObject> homes = utils.getAllHomes(player);

            if (homes.containsKey(args[0])) {
                completes.add("add");
                completes.add("remove");

                return filter(completes, args[1]);
            }
        }

        if (args.length == 3) {

            Map<String, HomeObject> homes = utils.getAllHomes(player);

            if (homes.containsKey(args[0])
                    && (args[1].equalsIgnoreCase("add")
                    || args[1].equalsIgnoreCase("remove"))) {

                for (Player online : Bukkit.getOnlinePlayers()) {
                    completes.add(online.getName());
                }

                return filter(completes, args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .sorted()
                .toList();
    }
}