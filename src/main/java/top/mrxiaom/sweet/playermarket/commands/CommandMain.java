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
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.economy.MPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.PlayerPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.VaultEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;
import top.mrxiaom.sweet.playermarket.gui.GuiMarketplace;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.time.LocalDateTime;
import java.util.*;

@AutoRegister
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && "open".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.open")) {
            Player player;
            if (args.length >= 2 && sender.hasPermission("sweet.playermarket.open.other")) {
                player = Util.getOnlinePlayer(args[2]).orElse(null);
                if (player == null) {
                    return Messages.player__not_online.tm(sender);
                }
            } else if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                return Messages.player__only.tm(sender);
            }
            GuiMarketplace.create(player, Searching.of(false)).open();
            return true;
        }
        if (args.length >= 1 && "create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
            if (!(sender instanceof Player)) {
                return Messages.player__only.tm(sender);
            }
            Player player = (Player) sender;
            ItemStack item = player.getItemInHand();
            if (item.getType().equals(Material.AIR)) {
                return Messages.Command.create__no_item.tm(sender);
            }
            if (args.length == 1) {
                return Messages.Command.create__no_type_input.tm(sender);
            }
            EnumMarketType type = Util.valueOrNull(EnumMarketType.class, args[1]);
            if (type == null) {
                return Messages.Command.create__no_type_found.tm(sender);
            }
            if (args.length == 2) {
                return Messages.Command.create__no_price_input.tm(sender);
            }
            double price = Util.parseDouble(args[2]).orElse(0.0);
            if (price <= 0) {
                return Messages.Command.create__no_price_valid.tm(sender);
            }
            IEconomy currency;
            if (args.length == 3) {
                currency = plugin.parseEconomy(defaultCurrency);
                if (currency == null) {
                    return Messages.Command.create__no_currency_default.tm(sender);
                }
            } else {
                currency = plugin.parseEconomy(args[3]);
                if (currency == null) {
                    return Messages.Command.create__no_currency_found.tm(sender);
                }
            }
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
            Integer itemCount = args.length <= 4
                    ? Integer.valueOf(item.getAmount())
                    : Util.parseInt(args[4]).orElse(null);
            if (itemCount == null) {
                return Messages.Command.create__no_item_count_valid.tm(sender);
            }
            if (itemCount > item.getMaxStackSize()) {
                return Messages.Command.create__no_item_count_valid_stack.tm(sender);
            }
            if (itemCount > item.getAmount()) {
                return Messages.Command.create__no_item_count_valid_held.tm(sender);
            }
            Integer marketAmount = args.length <= 5
                    ? Integer.valueOf(1)
                    : Util.parseInt(args[5]).orElse(null);
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
                PlayerInventory inventory = player.getInventory();
                ItemStack[] contents = inventory.getContents();
                for (ItemStack content : contents) {
                    if (content != null && content.isSimilar(sample)) {
                        invAmount += content.getAmount();
                    }
                }
                if (invAmount < totalAmount) {
                    return Messages.Command.create__sell__no_enough_items.tm(sender);
                }
                Utils.takeItem(player, sample, totalAmount);
            }
            if (type.equals(EnumMarketType.BUY)) {
                // 收购商店，收取玩家指定类型的货币
                double totalPrice = price * marketAmount;
                if (!currency.has(player, totalPrice)) {
                    return Messages.Command.create__buy__no_enough_currency.tm(sender);
                }
                currency.takeMoney(player, totalPrice);
            }

            MarketItem marketItem = MarketItem.builder(player)
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
                return Messages.Command.create__success.tm(sender);
            } else {
                return Messages.Command.create__failed.tm(sender);
            }
        }
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            if (args.length == 2 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                return Messages.Command.reload__database.tm(sender);
            }
            plugin.reloadConfig();
            return Messages.Command.reload__success.tm(sender);
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
