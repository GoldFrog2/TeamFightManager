package gf.tFM;

import gf.tFM.command.TfmCommand;
import gf.tFM.listener.TeamProtectionListener;
import gf.tFM.manager.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TFM extends JavaPlugin {

    private static TFM instance;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;

        // Zapisz config, jeśli go nie ma
        saveDefaultConfig();
        saveResource("teams.yml", false);

        // Inicjalizacja managera drużyn
        this.teamManager = new TeamManager(this);
        teamManager.loadTeams();

        // Rejestracja komend
        TfmCommand tfmCommand = new TfmCommand(this, teamManager);
        getCommand("tfm").setExecutor(tfmCommand);
        getCommand("tfm").setTabCompleter(tfmCommand);

        // Listener blokujący friendly fire
        getServer().getPluginManager().registerEvents(new TeamProtectionListener(teamManager), this);

        getLogger().info("✅ TeamFightManager (TFM) został pomyślnie włączony!");
    }

    @Override
    public void onDisable() {
        teamManager.saveTeams();
        getLogger().info("💾 TeamFightManager zapisano i wyłączono.");
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    // ========================
    // 🔹 API DLA INNYCH PLUGINÓW
    // ========================
    public static TFM getInstance() {
        return instance;
    }
}
