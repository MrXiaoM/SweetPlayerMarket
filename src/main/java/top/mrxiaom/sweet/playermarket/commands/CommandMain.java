package top.mrxiaom.sweet.playermarket.commands;

import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;
import top.mrxiaom.sweet.playermarket.gui.GuiMarketplace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetPlayerMarket plugin) {
        super(plugin);
        registerCommand("sweetplayermarket", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && "open".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.open")) {
            Player player;
            if (args.length >= 2 && sender.hasPermission("sweet.playermarket.open.other")) {
                player = Util.getOnlinePlayer(args[2]).orElse(null);
                if (player == null) {
                    return t(sender, "&e玩家不在线 (或不存在)");
                }
            } else if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                return t(sender, "只有玩家可以执行该命令");
            }
            GuiMarketplace.create(player, Searching.of(false)).open();
            return true;
        }
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            if (args.length == 2 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                return t(sender, "&a已重载 database.yml 并重新连接数据库");
            }
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        return true;
    }

    private static final List<String> listArg0 = Lists.newArrayList(
            "hello");
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        return Collections.emptyList();
    }

    public List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
