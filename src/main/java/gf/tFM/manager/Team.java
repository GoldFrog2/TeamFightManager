package gf.tFM.manager;

import org.bukkit.ChatColor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {
    private final String name;
    private final ChatColor color;
    private final Set<UUID> members;
    private int points;

    public Team(String name, ChatColor color) {
        this.name = name;
        this.color = color;
        this.members = new HashSet<>();
        this.points = 0;
    }

    public String getName() { return name; }
    public ChatColor getColor() { return color; }
    public Set<UUID> getMembers() { return members; }
    public int getPoints() { return points; }

    public void addPoints(int amount) { points += amount; }
    public void setPoints(int points) { this.points = points; }

    public void addMember(UUID playerId) { members.add(playerId); }
    public void removeMember(UUID playerId) { members.remove(playerId); }

    public boolean isMember(UUID playerId) { return members.contains(playerId); }
}
