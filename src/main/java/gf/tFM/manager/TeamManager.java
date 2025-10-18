package gf.tFM.manager;

import gf.tFM.TFM;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TeamManager {

    private final TFM plugin;
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final File file;
    private final FileConfiguration config;

    public TeamManager(TFM plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");

        // Utwórz plik, jeśli nie istnieje
        if (!file.exists()) {
            plugin.saveResource("teams.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        loadTeams();
    }

    /* =======================
     * PODSTAWOWE FUNKCJE
     * ======================= */

    public void createTeam(String name, ChatColor color) {
        teams.put(name.toLowerCase(Locale.ROOT), new Team(name, color));
        saveTeams();
    }

    public Optional<Team> getTeamByName(String name) {
        return Optional.ofNullable(teams.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<Team> getTeamOfPlayer(UUID playerId) {
        String teamName = playerTeams.get(playerId);
        if (teamName == null) return Optional.empty();
        return getTeamByName(teamName);
    }

    public void joinTeam(UUID playerId, Team team) {
        leaveCurrentTeam(playerId);
        playerTeams.put(playerId, team.getName().toLowerCase(Locale.ROOT));
        team.getMembers().add(playerId);
        saveTeams();
    }

    public void leaveCurrentTeam(UUID playerId) {
        getTeamOfPlayer(playerId).ifPresent(t -> t.getMembers().remove(playerId));
        playerTeams.remove(playerId);
        saveTeams();
    }

    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    public List<Team> getTeamsSortedByPoints() {
        return teams.values().stream()
                .sorted(Comparator.comparingInt(Team::getPoints).reversed())
                .collect(Collectors.toList());
    }

    public void addPoints(String teamName, int amount) {
        getTeamByName(teamName).ifPresent(t -> {
            t.setPoints(t.getPoints() + amount);
            saveTeams();
        });
    }

    /* =======================
     * ZAPIS / ODCZYT
     * ======================= */

    public void saveTeams() {
        config.set("teams", null); // wyczyść stare dane

        for (Team team : teams.values()) {
            String base = "teams." + team.getName();
            config.set(base + ".color", team.getColor().name());
            config.set(base + ".points", team.getPoints());

            List<String> members = team.getMembers().stream()
                    .map(UUID::toString)
                    .toList();
            config.set(base + ".members", members);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Nie udało się zapisać teams.yml!");
            e.printStackTrace();
        }
    }

    public void loadTeams() {
        if (!config.contains("teams")) return;

        for (String key : config.getConfigurationSection("teams").getKeys(false)) {
            ChatColor color = ChatColor.WHITE;
            try {
                color = ChatColor.valueOf(config.getString("teams." + key + ".color", "WHITE"));
            } catch (IllegalArgumentException ignored) {}

            int points = config.getInt("teams." + key + ".points", 0);
            List<String> memberStrings = config.getStringList("teams." + key + ".members");
            Set<UUID> members = memberStrings.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());

            Team team = new Team(key, color);
            team.setPoints(points);
            team.getMembers().addAll(members);
            teams.put(key.toLowerCase(Locale.ROOT), team);

            for (UUID member : members) {
                playerTeams.put(member, key.toLowerCase(Locale.ROOT));
            }
        }
    }
}
