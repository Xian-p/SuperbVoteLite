package me.example.superbvotelite;

import com.vexsoftware.votifier.model.Vote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public final class JoinListener implements Listener {

    private final SuperbVoteLite plugin;
    private final OfflineVoteStore store;

    public JoinListener(SuperbVoteLite plugin, OfflineVoteStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("settings.process-offline-on-join", true)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<OfflineVoteStore.StoredVote> votes = store.popVotes(uuid);
        if (votes.isEmpty()) return;

        // Deliver one by one (so per-service rewards work)
        VoteListener rewarder = new VoteListener(plugin, store);

        for (OfflineVoteStore.StoredVote v : votes) {
            Vote vote = new Vote(v.getServiceName(), player.getName(), v.getAddress());
            Bukkit.getScheduler().runTask(plugin, () -> rewarder.deliverRewards(player, vote));
        }

        store.saveAsync();

        if (plugin.isDebug()) {
            plugin.getLogger().info("Processed " + votes.size() + " stored vote(s) for " + player.getName());
        }
    }
}