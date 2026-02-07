package me.example.superbvotelite;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class SuperbVoteLite extends JavaPlugin {

    private OfflineVoteStore offlineVoteStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.offlineVoteStore = new OfflineVoteStore(this);
        this.offlineVoteStore.load();

        getServer().getPluginManager().registerEvents(new VoteListener(this, offlineVoteStore), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this, offlineVoteStore), this);

        getLogger().info("SuperbVoteLite enabled.");
    }

    @Override
    public void onDisable() {
        if (offlineVoteStore != null) {
            offlineVoteStore.save();
        }
    }

    public boolean isDebug() {
        return getConfig().getBoolean("settings.debug", false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("svl")) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("SuperbVoteLite config reloaded.");
            return true;
        }

        sender.sendMessage("Usage: /svl reload");
        return true;
    }
}