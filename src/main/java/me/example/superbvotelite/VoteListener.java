package me.example.superbvotelite;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class VoteListener implements Listener {

    private final SuperbVoteLite plugin;
    private final OfflineVoteStore store;

    public VoteListener(SuperbVoteLite plugin, OfflineVoteStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();

        // NuVotifier may call this async; always hop to main thread before using Bukkit API.
        Bukkit.getScheduler().runTask(plugin, () -> handleVoteSync(vote));
    }

    private void handleVoteSync(Vote vote) {
        String playerName = vote.getUsername();
        String service = vote.getServiceName();

        if (playerName == null || playerName.isBlank()) {
            if (plugin.isDebug()) plugin.getLogger().warning("Received vote with blank username from " + service);
            return;
        }

        Player online = Bukkit.getPlayerExact(playerName);
        boolean storeOffline = plugin.getConfig().getBoolean("settings.store-offline-votes", true);

        if (online == null) {
            if (storeOffline) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                store.addVote(offlinePlayer.getUniqueId(), new OfflineVoteStore.StoredVote(
                        vote.getServiceName(),
                        vote.getAddress(),
                        System.currentTimeMillis()
                ));
                store.saveAsync();

                String msg = plugin.getConfig().getString("messages.offline-queued", "");
                // If you want to message offline players via other systems, hook it here.
                if (plugin.isDebug()) plugin.getLogger().info("Queued offline vote for " + playerName + " (" + service + ")");
            } else {
                if (plugin.isDebug()) plugin.getLogger().info("Ignoring vote for offline player " + playerName + " (" + service + ")");
            }
            return;
        }

        deliverRewards(online, vote);
    }

    public void deliverRewards(Player player, Vote vote) {
        String receivedMsg = plugin.getConfig().getString("messages.received", "");
        if (receivedMsg != null && !receivedMsg.isBlank()) {
            player.sendMessage(color(applyPlaceholders(receivedMsg, player.getName(), vote)));
        }

        String broadcastMsg = plugin.getConfig().getString("messages.broadcast", "");
        if (broadcastMsg != null && !broadcastMsg.isBlank()) {
            Bukkit.broadcastMessage(color(applyPlaceholders(broadcastMsg, player.getName(), vote)));
        }

        List<String> cmds = new ArrayList<>();

        cmds.addAll(plugin.getConfig().getStringList("rewards.global-commands"));

        ConfigurationSection perService = plugin.getConfig().getConfigurationSection("rewards.per-service");
        if (perService != null) {
            ConfigurationSection svc = perService.getConfigurationSection(vote.getServiceName());
            if (svc != null) {
                cmds.addAll(svc.getStringList("commands"));
            }
        }

        for (String raw : cmds) {
            if (raw == null || raw.isBlank()) continue;
            String cmd = applyPlaceholders(raw, player.getName(), vote);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        if (plugin.isDebug()) {
            plugin.getLogger().info("Delivered vote rewards to " + player.getName() + " for " + vote.getServiceName());
        }
    }

    private String applyPlaceholders(String input, String playerName, Vote vote) {
        String ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        return input
                .replace("%player%", playerName)
                .replace("%service%", safe(vote.getServiceName()))
                .replace("%address%", safe(vote.getAddress()))
                .replace("%timestamp%", ts);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}