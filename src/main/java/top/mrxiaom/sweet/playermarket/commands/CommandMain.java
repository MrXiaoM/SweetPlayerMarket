package top.mrxiaom.sweet.playermarket.commands;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.commands.arguments.*;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.limitation.BaseLimitation;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;
import top.mrxiaom.sweet.playermarket.func.LimitationManager;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    private String defaultCurrency;
    public CommandMain(SweetPlayerMarket plugin) {
        super(plugin);
        registerCommand("sweetplayermarket", this);
    }

    public String defaultCurrency() {
        return defaultCurrency;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        defaultCurrency = config.getString("default.currency", "Vault");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command cmd,
            @NotNull String label,
            @NotNull String[] args
    ) {
        SimpleArguments command = SimpleArguments.of(args);
        if (command.match("open")) {
            if (!sender.hasPermission("sweet.playermarket.open")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.to(OpenArguments::of).execute(plugin, sender);
        }
        if (command.match("me")) {
            if (!sender.hasPermission("sweet.playermarket.me")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.to(MeArguments::of).execute(plugin, sender);
        }
        if (command.match("create")) {
            if (!sender.hasPermission("sweet.playermarket.create")) {
                return Messages.Command.no_permission.tm(sender);
            }
            if (!(sender instanceof Player)) {
                return Messages.player__only.tm(sender);
            }
            return command.into(CreateArguments::of).execute(plugin, (Player) sender);
        }
        if (command.match("recalc")) {
            if (!sender.hasPermission("sweet.playermarket.recalc")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.into(RecalcArguments::of).execute(plugin, sender);
        }
        if (command.match("reload")) {
            if (!sender.isOp()) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.into(ReloadArguments::of).execute(plugin, sender);
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            add(sender, list, "sweet.playermarket.open", "open");
            add(sender, list, "sweet.playermarket.create", "create");
            add(sender, list, "sweet.playermarket.me", "me");
            add(sender, list, "sweet.playermarket.recalc", "recalc");
            if (sender.isOp()) {
                list.add("reload");
            }
            return startsWith(list, args[0]);
        }
        if (args.length == 2) {
            if (sender.isOp()) {
                if ("reload".equalsIgnoreCase(args[0])) {
                    if ("database".startsWith(args[0])) {
                        return Collections.singletonList("database");
                    }
                    return Collections.emptyList();
                }
            }
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                BaseLimitation limitation = getLimit(sender);
                List<String> list = new ArrayList<>();
                if (limitation != null) {
                    for (EnumMarketType type : EnumMarketType.values()) {
                        if (limitation.canUseMarketType(type)) {
                            list.add(type.name().toLowerCase());
                        }
                    }
                }
                return startsWith(list, args[1]);
            }
            if ("open".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.open.other")) {
                return null;
            }
            if ("me".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.me.other")) {
                return null;
            }
        }
        if (args.length == 3) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                if (isItemAvailable(sender)) {
                    return Collections.singletonList(Messages.TabComplete.create__price.str());
                } else {
                    return Collections.emptyList();
                }
            }
        }
        if (args.length == 4) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                BaseLimitation limitation = getLimit(sender);
                List<String> list = new ArrayList<>();
                if (limitation != null) {
                    if (limitation.canUseCurrency(plugin.getVault()) && sender.hasPermission("sweet.playermarket.create.currency.vault")) {
                        list.add("Vault");
                    }
                    if (limitation.canUseCurrency(plugin.getPlayerPoints()) && sender.hasPermission("sweet.playermarket.create.currency.playerpoints")) {
                        list.add("PlayerPoints");
                    }
                    if (plugin.getMPoints() != null) {
                        for (String sign : plugin.getMPoints().getSigns()) {
                            if (sender.hasPermission("sweet.playermarket.create.currency.mpoints." + sign)
                                    && limitation.canUseCurrency(plugin.getMPoints().of(sign))) {
                                list.add("MPoints:" + sign);
                            }
                        }
                    }
                }
                return startsWith(list, args[3]);
            }
        }
        if (args.length == 5) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                if (isItemAvailable(sender)) {
                    return Collections.singletonList(Messages.TabComplete.create__item_count.str());
                } else {
                    return Collections.emptyList();
                }
            }
        }
        if (args.length == 6) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                if (isItemAvailable(sender)) {
                    return Collections.singletonList(Messages.TabComplete.create__amount.str());
                } else {
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }
    @SuppressWarnings({"deprecation"})
    private BaseLimitation getLimit(CommandSender sender) {
        if (sender instanceof Player) {
            ItemStack item = ((Player) sender).getItemInHand();
            if (item.getType().equals(Material.AIR)) {
                return null;
            }
            return LimitationManager.inst().getLimitByItem(item);
        }
        return null;
    }
    @SuppressWarnings({"deprecation"})
    private boolean isItemAvailable(CommandSender sender) {
        if (sender instanceof Player) {
            ItemStack item = ((Player) sender).getItemInHand();
            return !item.getType().equals(Material.AIR);
        }
        return false;
    }
    private void add(CommandSender sender, List<String> list, String permission, String... args) {
        if (sender.hasPermission(permission)) {
            list.addAll(Arrays.asList(args));
        }
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

    public static CommandMain inst() {
        return instanceOf(CommandMain.class);
    }
}
