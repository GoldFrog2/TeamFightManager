package gf.tFM.command;

import gf.tFM.TFM;
import gf.tFM.manager.Team;
import gf.tFM.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TfmCommand implements CommandExecutor, TabCompleter {

    private final TFM plugin;
    private final TeamManager teamManager;
    private static final List<String> SUBS = Arrays.asList("create", "join", "leave", "list", "set", "chat", "top");

    public TfmCommand(TFM plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Użycie: /" + label + " <create|join|leave|list|set|chat|top>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Skrót czatu drużynowego: ! <wiadomość>
        if (sub.startsWith("!")) {
            return handleChat(sender, args);
        }

        switch (sub) {
            case "create": return handleCreate(sender, args);
            case "join": return handleJoin(sender, args);
            case "leave": return handleLeave(sender);
            case "list": return handleList(sender);
            case "set": return handleSet(sender, args);
            case "chat": return handleChat(sender, args);
            case "top": return handleTop(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Nieznana komenda. Użyj /" + label + " <create|join|leave|list|set|chat|top>");
                return true;
        }
    }

    /* ---------------------- /Tfm create <nazwa> [kolor] ---------------------- */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tfm.create")) {
            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do tworzenia drużyn.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /Tfm create <nazwa> [kolor]");
            return true;
        }

        String name = args[1];
        ChatColor color = ChatColor.WHITE;

        if (args.length >= 3) {
            try {
                color = ChatColor.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Nieznany kolor. Użyj np. RED, BLUE, GREEN, YELLOW...");
                return true;
            }
        }

        if (teamManager.getTeamByName(name).isPresent()) {
            sender.sendMessage(ChatColor.RED + "Taka drużyna już istnieje!");
            return true;
        }

        teamManager.createTeam(name, color);
        sender.sendMessage(ChatColor.GREEN + "✅ Stworzono drużynę " + color + name + ChatColor.GREEN + "!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "➤ Utworzono nową drużynę: " + color + name);
        return true;
    }

    /* ---------------------- /Tfm join <nazwa> ---------------------- */
    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracze mogą dołączać do drużyn.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /Tfm join <nazwa>");
            return true;
        }

        String name = args[1];
        var teamOpt = teamManager.getTeamByName(name);
        if (teamOpt.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono drużyny o nazwie " + name);
            return true;
        }

        teamManager.joinTeam(player.getUniqueId(), teamOpt.get());
        player.sendMessage(ChatColor.GREEN + "Dołączyłeś do drużyny " + teamOpt.get().getColor() + teamOpt.get().getName() + "!");
        Bukkit.broadcastMessage(ChatColor.GRAY + player.getName() + " dołączył do drużyny " + teamOpt.get().getColor() + teamOpt.get().getName());
        return true;
    }

    /* ---------------------- /Tfm leave ---------------------- */
    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracze mogą opuszczać drużyny.");
            return true;
        }

        var teamOpt = teamManager.getTeamOfPlayer(player.getUniqueId());
        if (teamOpt.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nie jesteś w żadnej drużynie.");
            return true;
        }

        teamManager.leaveCurrentTeam(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Opuściłeś drużynę " + teamOpt.get().getColor() + teamOpt.get().getName());
        Bukkit.broadcastMessage(ChatColor.GRAY + player.getName() + " opuścił drużynę " + teamOpt.get().getColor() + teamOpt.get().getName());
        return true;
    }

    /* ---------------------- /Tfm list ---------------------- */
    private boolean handleList(CommandSender sender) {
        Collection<Team> teams = teamManager.getAllTeams();
        if (teams.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Brak utworzonych drużyn.");
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "=== Lista Drużyn ===");
        for (Team t : teams) {
            sender.sendMessage(t.getColor() + t.getName() + ChatColor.WHITE + " | Gracze: " +
                    t.getMembers().size() + " | Punkty: " + ChatColor.GOLD + t.getPoints());
        }
        return true;
    }

    /* ---------------------- /Tfm set <gracz> <team> ---------------------- */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tfm.set")) {
            sender.sendMessage(ChatColor.RED + "Brak uprawnień.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Użycie: /Tfm set <gracz> <team>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Gracz nie jest online.");
            return true;
        }

        var teamOpt = teamManager.getTeamByName(args[2]);
        if (teamOpt.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono drużyny " + args[2]);
            return true;
        }

        teamManager.joinTeam(target.getUniqueId(), teamOpt.get());
        sender.sendMessage(ChatColor.GREEN + "Gracz " + target.getName() + " został przypisany do drużyny " + teamOpt.get().getColor() + teamOpt.get().getName());
        target.sendMessage(ChatColor.GRAY + "Zostałeś przypisany do drużyny " + teamOpt.get().getColor() + teamOpt.get().getName());
        return true;
    }

    /* ---------------------- /Tfm chat <wiadomość> ---------------------- */
    private boolean handleChat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Tylko gracze mogą używać czatu drużynowego.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /Tfm chat <wiadomość>");
            return true;
        }

        var teamOpt = teamManager.getTeamOfPlayer(player.getUniqueId());
        if (teamOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Nie jesteś w żadnej drużynie!");
            return true;
        }

        Team team = teamOpt.get();
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String formatted = ChatColor.GRAY + "[" + team.getColor() + team.getName() + ChatColor.GRAY + "] " +
                player.getName() + ": " + ChatColor.WHITE + message;

        for (UUID member : team.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null && p.isOnline()) {
                p.sendMessage(formatted);
            }
        }
        return true;
    }

    /* ---------------------- /Tfm top ---------------------- */
    private boolean handleTop(CommandSender sender) {
        List<Team> sorted = teamManager.getTeamsSortedByPoints();
        if (sorted.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Brak drużyn do wyświetlenia.");
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "=== Ranking Drużyn ===");
        int pos = 1;
        for (Team t : sorted) {
            sender.sendMessage(ChatColor.GRAY + "" + pos + ". " + t.getColor() + t.getName() +
                    ChatColor.WHITE + " - " + ChatColor.GOLD + t.getPoints() + " pkt");
            pos++;
        }
        return true;
    }

    /* ---------------------- TAB COMPLETER ---------------------- */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return teamManager.getAllTeams().stream()
                    .map(Team::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return teamManager.getAllTeams().stream()
                    .map(Team::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(ChatColor.values())
                    .map(Enum::name)
                    .filter(c -> c.startsWith(args[2].toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
