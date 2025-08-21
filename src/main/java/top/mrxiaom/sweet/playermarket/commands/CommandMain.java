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
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.commands.arguments.MeArguments;
import top.mrxiaom.sweet.playermarket.commands.arguments.OpenArguments;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.economy.MPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.PlayerPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.VaultEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;
import top.mrxiaom.sweet.playermarket.gui.GuiMarketplace;
import top.mrxiaom.sweet.playermarket.gui.GuiMyItems;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.time.LocalDateTime;
import java.util.*;

import static top.mrxiaom.pluginbase.utils.arguments.CommandArguments.NULL;

@AutoRegister
@SuppressWarnings("SameReturnValue")
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    private String defaultCurrency;
    public CommandMain(SweetPlayerMarket plugin) {
        super(plugin);
        registerCommand("sweetplayermarket", this);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        defaultCurrency = config.getString("default.currency", "Vault");
    }

    private Player getPlayerOrSelf(CommandArguments args, CommandSender sender, String perm) {
        return args.nextPlayerOrSelf(sender,
                () -> Messages.player__only.tm(sender), perm,
                () -> Messages.Command.no_permission.tm(sender),
                () -> Messages.player__not_online.tm(sender));
    }

    private boolean runOpen(CommandSender sender, OpenArguments args) {
        Player player = getPlayerOrSelf(args, sender, "sweet.playermarket.open.other");
        if (player == null) {
            return true;
        }
        IEconomy currency = plugin.parseEconomy(args.currency());
        GuiMarketplace.create(player, Searching.of(false)
                .type(Util.valueOr(EnumMarketType.class, args.type(), null))
                .currency(currency == null ? null : currency.id())).open();
        return true;
    }

    private boolean runMe(CommandSender sender, MeArguments args) {
        Player player = getPlayerOrSelf(args, sender, "sweet.playermarket.me.other");
        if (player == null) {
            return true;
        }
        GuiMyItems.create(player, Searching.of(false)
                .playerId(plugin.getKey(player))
                .notice(args.notice() ? 1 : null)
                .onlyOutOfStock(args.onlyOutOfStock())
        ).open();
        return true;
    }

    @SuppressWarnings({"deprecation"})
    private boolean runCreate(Player sender, CommandArguments args) {
        ItemStack item = sender.getItemInHand();
        if (item.getType().equals(Material.AIR)) {
            return Messages.Command.create__no_item.tm(sender);
        }
        EnumMarketType type = args.nextValueOf(EnumMarketType.class);
        if (type == null) {
            return Messages.Command.create__no_type_found.tm(sender);
        }
        double price = args.nextDouble(0.0);
        if (price <= 0) {
            return Messages.Command.create__no_price_valid.tm(sender);
        }
        IEconomy currency = args.nextOptional(currencyName -> {
            if (currencyName == null) {
                IEconomy parsed = plugin.parseEconomy(defaultCurrency);
                if (parsed != null) {
                    return parsed;
                }
                Messages.Command.create__no_currency_default.tm(sender);
            } else {
                IEconomy parsed = plugin.parseEconomy(currencyName);
                if (parsed != null) {
                    return parsed;
                }
                Messages.Command.create__no_currency_found.tm(sender);
            }
            return null;
        });
        if (currency == null) return true;
        if (currency instanceof VaultEconomy) {
            if (!sender.hasPermission("sweet.playermarket.create.currency.vault")) {
                return Messages.Command.create__no_currency_permission.tm(sender);
            }
        }
        if (currency instanceof PlayerPointsEconomy) {
            if (!sender.hasPermission("sweet.playermarket.create.currency.playerpoints")) {
                return Messages.Command.create__no_currency_permission.tm(sender);
            }
        }
        if (currency instanceof MPointsEconomy) {
            String sign = ((MPointsEconomy) currency).sign();
            if (!sender.hasPermission("sweet.playermarket.create.currency.mpoints." + sign)) {
                return Messages.Command.create__no_currency_permission.tm(sender);
            }
        }
        Integer itemCount = args.nextInt(item::getAmount, NULL());
        if (itemCount == null) {
            return Messages.Command.create__no_item_count_valid.tm(sender);
        }
        if (itemCount > item.getMaxStackSize()) {
            return Messages.Command.create__no_item_count_valid_stack.tm(sender);
        }
        if (itemCount > item.getAmount()) {
            return Messages.Command.create__no_item_count_valid_held.tm(sender);
        }
        Integer marketAmount = args.nextInt(() -> 1, NULL());
        if (marketAmount == null || marketAmount < 1 || marketAmount > 64) {
            return Messages.Command.create__no_amount_valid.tm(sender);
        }

        // TODO: 计算上架所需手续费用，并检查玩家够不够钱

        ItemStack shopItem = item.clone();
        shopItem.setAmount(itemCount);

        int totalAmount = itemCount * marketAmount;
        if (type.equals(EnumMarketType.SELL)) {
            // 出售商店，检查玩家背包里有没有这么多的物品，并拿走这些物品
            ItemStack sample = shopItem.clone();
            sample.setAmount(1);
            int invAmount = 0;
            PlayerInventory inventory = sender.getInventory();
            ItemStack[] contents = inventory.getContents();
            for (ItemStack content : contents) {
                if (content != null && content.isSimilar(sample)) {
                    invAmount += content.getAmount();
                }
            }
            if (invAmount < totalAmount) {
                return Messages.Command.create__sell__no_enough_items.tm(sender);
            }
            Utils.takeItem(sender, sample, totalAmount);
        }
        if (type.equals(EnumMarketType.BUY)) {
            // 收购商店，收取玩家指定类型的货币
            double totalPrice = price * marketAmount;
            if (!currency.has(sender, totalPrice)) {
                return Messages.Command.create__buy__no_enough_currency.tm(sender);
            }
            currency.takeMoney(sender, totalPrice);
        }

        MarketItem marketItem = MarketItem.builder(sender)
                .item(shopItem)
                .type(type)
                .price(price)
                .currency(currency)
                .amount(marketAmount)
                // TODO: 商品到期时间移到配置文件
                .outdateTime(LocalDateTime.now().plusDays(5))
                .build();

        if (plugin.getMarketplace().putItem(marketItem)) {
            // TODO: 通过 BungeeCord 通知其它子服已打开的界面，应该刷新全球市场菜单
            return Messages.Command.create__success.tm(sender,
                    Pair.of("%item%", plugin.displayNames().getDisplayName(shopItem, sender)));
        } else {
            return Messages.Command.create__failed.tm(sender);
        }
    }

    private boolean runReload(CommandSender sender, CommandArguments args) {
        if (args.match("database")) {
            plugin.options.database().reloadConfig();
            plugin.options.database().reconnect();
            return Messages.Command.reload__database.tm(sender);
        }
        plugin.reloadConfig();
        return Messages.Command.reload__success.tm(sender);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command cmd,
            @NotNull String label,
            @NotNull String[] args
    ) {
        CommandArguments command = CommandArguments.simple(args);
        if (command.match("open")) {
            if (!sender.hasPermission("sweet.playermarket.open")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return runOpen(sender, command.to(OpenArguments::of));
        }
        if (command.match("me")) {
            if (!sender.hasPermission("sweet.playermarket.me")) {
                return Messages.Command.no_permission.tm(sender);
            }
            return runMe(sender, command.to(MeArguments::of));
        }
        if (command.match("create")) {
            if (!sender.hasPermission("sweet.playermarket.create")) {
                return Messages.Command.no_permission.tm(sender);
            }
            if (!(sender instanceof Player)) {
                return Messages.player__only.tm(sender);
            }
            return runCreate((Player) sender, command);
        }
        if (command.match("reload")) {
            if (!sender.isOp()) {
                return Messages.Command.no_permission.tm(sender);
            }
            return runReload(sender, command);
        }
        return true;
    }

    private final List<String> arg1Create = new ArrayList<>();
    {
        for (EnumMarketType value : EnumMarketType.values()) {
            arg1Create.add(value.name().toLowerCase());
        }
    }
    private final List<String> arg3Create = Lists.newArrayList("Vault", "PlayerPoints", "MPoints:");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            add(sender, list, "sweet.playermarket.open", "open");
            add(sender, list, "sweet.playermarket.create", "create");
            add(sender, list, "sweet.playermarket.me", "me");
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
                return startsWith(arg1Create, args[1]);
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
                return Collections.singletonList(Messages.TabComplete.create__price.str());
            }
        }
        if (args.length == 4) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                return startsWith(arg3Create, args[3]);
            }
        }
        if (args.length == 5) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                return Collections.singletonList(Messages.TabComplete.create__item_count.str());
            }
        }
        if (args.length == 6) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                return Collections.singletonList(Messages.TabComplete.create__amount.str());
            }
        }
        return Collections.emptyList();
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
}
