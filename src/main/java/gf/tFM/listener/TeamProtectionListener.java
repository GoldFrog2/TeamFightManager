package gf.tFM.listener;

import gf.tFM.manager.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;

public class TeamProtectionListener implements Listener {

    private final TeamManager teamManager;

    public TeamProtectionListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;

        if (event.getDamager() instanceof Player p) attacker = p;
        else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) attacker = p;

        if (attacker == null) return;

        var teamA = teamManager.getTeamOfPlayer(victim.getUniqueId());
        var teamB = teamManager.getTeamOfPlayer(attacker.getUniqueId());
        if (teamA.isPresent() && teamB.isPresent() && teamA.get().equals(teamB.get())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player thrower)) return;

        var throwerTeam = teamManager.getTeamOfPlayer(thrower.getUniqueId());
        if (throwerTeam.isEmpty()) return;

        event.getAffectedEntities().removeIf(e ->
                e instanceof Player target &&
                        teamManager.getTeamOfPlayer(target.getUniqueId())
                                .map(t -> t.equals(throwerTeam.get()))
                                .orElse(false));
    }
}
