package me.example.superbvotelite;

import com.vexsoftware.votifier.model.Vote;
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

        // Re-use the same reward logic
        VoteListener rewarder = new VoteListener(plugin, store);

        for (OfflineVoteStore.StoredVote v : votes) {
            // Your NuVotifier version needs the 4-arg constructor (service, user, address, timestamp)
            Vote vote = new Vote(
                    v.getServiceName(),
                    player.getName(),
                    v.getAddress() == null ? "" : v.getAddress(),
                    String.valueOf(v.getTimeMillis())
            );

            // PlayerJoinEvent runs on the main thread, so it's safe to execute rewards directly
            rewarder.deliverRewards(player, vote);
        }

        store.saveAsync();

        if (plugin.isDebug()) {
            plugin.getLogger().info("Processed " + votes.size() + " stored vote(s) for " + player.getName());
        }
    }
                        }
