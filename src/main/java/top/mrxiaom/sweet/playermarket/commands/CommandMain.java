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
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;
import top.mrxiaom.sweet.playermarket.func.AutoDeployManager;
import top.mrxiaom.sweet.playermarket.func.LimitationManager;

import java.util.*;

import static top.mrxiaom.pluginbase.utils.CollectionUtils.startsWith;

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
        if (command.match("server")) {
            if (!sender.hasPermission("sweet.playermarket.server")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.into(ServerArguments::of).execute(plugin, sender);
        }
        if (command.match("create")) {
            if (!sender.hasPermission("sweet.playermarket.create")) {
                return Messages.Command.no_permission.tm(sender);
            }
            if (!(sender instanceof Player)) {
                return Messages.player__only.tm(sender);
            }
            return command.to(CreateArguments::of).execute(plugin, (Player) sender);
        }
        if (command.match("limitation")) {
            if (!sender.hasPermission("sweet.playermarket.limitation")) {
                return Messages.Command.no_permission.tm(sender);
            }
            if (!(sender instanceof Player)) {
                return Messages.player__only.tm(sender);
            }
            return command.into(LimitationArguments::of).execute(plugin, (Player) sender);
        }
        if (command.match("tags")) {
            if (!sender.hasPermission("sweet.playermarket.tags")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.into(TagsArguments::of).execute(plugin, sender);
        }
        if (command.match("recalc")) {
            if (!sender.hasPermission("sweet.playermarket.recalc")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.into(RecalcArguments::of).execute(plugin, sender);
        }
        if (command.match("auto-deploy")) {
            if (!sender.hasPermission("sweet.playermarket.auto-deploy")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.into(AutoDeployArguments::of).execute(plugin, sender);
        }
        if (command.match("reload")) {
            if (!sender.isOp()) {
                return Messages.Command.no_permission.tm(sender);
            }
            return command.into(ReloadArguments::of).execute(plugin, sender);
        }
        return (sender.isOp() ? Messages.Command.help__admin : Messages.Command.help__player).tm(sender);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            add(sender, list, "sweet.playermarket.open", "open");
            add(sender, list, "sweet.playermarket.create", "create");
            add(sender, list, "sweet.playermarket.limitation", "limitation");
            add(sender, list, "sweet.playermarket.tags", "tags");
            add(sender, list, "sweet.playermarket.me", "me");
            add(sender, list, "sweet.playermarket.server", "server");
            add(sender, list, "sweet.playermarket.recalc", "recalc");
            add(sender, list, "sweet.playermarket.auto-deploy", "auto-deploy");
            if (sender.isOp()) {
                list.add("reload");
            }
            return startsWith(args[0], list);
        }
        if (args.length == 2) {
            if (sender.isOp()) {
                if ("reload".equalsIgnoreCase(args[0])) {
                    List<String> list = new ArrayList<>();
                    list.add("assets");
                    list.add("database");
                    return startsWith(args[1], list);
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
                } else {
                    for (EnumMarketType type : EnumMarketType.values()) {
                        list.add(type.name().toLowerCase());
                    }
                }
                return startsWith(args[1], list);
            }
            if ("open".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.open.other")) {
                return null;
            }
            if ("tags".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.tags.other")) {
                return null;
            }
            if ("me".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.me.other")) {
                return null;
            }
            if ("server".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.server.other")) {
                return null;
            }
            if ("auto-deploy".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.auto-deploy")) {
                return startsWith(args[1], AutoDeployManager.inst().keys());
            }
            if ("recalc".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.recalc")) {
                List<String> list = new ArrayList<>();
                list.add("tags");
                list.add("index");
                return startsWith(args[1], list);
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
            if ("auto-deploy".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.auto-deploy")) {
                return startsWith(args[2], Lists.newArrayList("print", "condition", "test"));
            }
        }
        if (args.length == 4) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                BaseLimitation limitation = getLimit(sender);
                List<String> list = new ArrayList<>();
                if (limitation != null) {
                    if (limitation.canUseCurrency(plugin.getVault()) && plugin.getVault().hasPermission(sender)) {
                        list.add("Vault");
                    }
                    if (limitation.canUseCurrency(plugin.getPlayerPoints()) && plugin.getPlayerPoints().hasPermission(sender)) {
                        list.add("PlayerPoints");
                    }
                    if (plugin.getMPoints() != null) {
                        for (String sign : plugin.getMPoints().getSigns()) {
                            IEconomy currency = plugin.getMPoints().of(sign);
                            if (limitation.canUseCurrency(currency) && currency.hasPermission(sender)) {
                                list.add("MPoints:" + sign);
                            }
                        }
                    }
                    if (plugin.getCoinsEngine() != null) {
                        for (String currencyId : plugin.getCoinsEngine().getSigns()) {
                            IEconomy currency = plugin.getCoinsEngine().of(currencyId);
                            if (limitation.canUseCurrency(currency) && currency.hasPermission(sender)) {
                                list.add("CoinsEngine:" + currencyId);
                            }
                        }
                    }
                    for (String currencyId : plugin.getCustomEconomy().getSigns()) {
                        IEconomy currency = plugin.getCustomEconomy().of(currencyId);
                        if (limitation.canUseCurrency(currency) && currency.hasPermission(sender)) {
                            list.add("Custom:" + currencyId);
                        }
                    }
                }
                return startsWith(args[3], list);
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
            return LimitationManager.inst().getLimitation(item, null);
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

    public static CommandMain inst() {
        return instanceOf(CommandMain.class);
    }
}
