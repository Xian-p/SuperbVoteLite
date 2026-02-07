package me.example.superbvotelite;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class OfflineVoteStore {

    public static final class StoredVote {
        private final String serviceName;
        private final String address;
        private final long timeMillis;

        public StoredVote(String serviceName, String address, long timeMillis) {
            this.serviceName = serviceName;
            this.address = address;
            this.timeMillis = timeMillis;
        }

        public String getServiceName() { return serviceName; }
        public String getAddress() { return address; }
        public long getTimeMillis() { return timeMillis; }
    }

    private final SuperbVoteLite plugin;
    private final File file;

    // UUID -> list of votes
    private final Map<UUID, List<StoredVote>> queued = new HashMap<>();

    public OfflineVoteStore(SuperbVoteLite plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "votes.yml");
    }

    public synchronized void addVote(UUID uuid, StoredVote vote) {
        queued.computeIfAbsent(uuid, k -> new ArrayList<>()).add(vote);
    }

    public synchronized List<StoredVote> popVotes(UUID uuid) {
        List<StoredVote> list = queued.remove(uuid);
        return list == null ? Collections.emptyList() : list;
    }

    public synchronized void load() {
        queued.clear();
        if (!file.exists()) return;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            List<Map<?, ?>> rawList = yml.getMapList(key);
            List<StoredVote> votes = new ArrayList<>();

            for (Map<?, ?> map : rawList) {
                String service = Objects.toString(map.get("service"), "");
                String addr = Objects.toString(map.get("address"), "");
                long time = 0L;
                Object t = map.get("time");
                if (t instanceof Number n) time = n.longValue();

                if (!service.isBlank()) {
                    votes.add(new StoredVote(service, addr, time));
                }
            }

            if (!votes.isEmpty()) queued.put(uuid, votes);
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, List<StoredVote>> e : queued.entrySet()) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (StoredVote v : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("service", v.getServiceName());
                m.put("address", v.getAddress());
                m.put("time", v.getTimeMillis());
                out.add(m);
            }
            yml.set(e.getKey().toString(), out);
        }

        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save votes.yml: " + ex.getMessage());
        }
    }
}