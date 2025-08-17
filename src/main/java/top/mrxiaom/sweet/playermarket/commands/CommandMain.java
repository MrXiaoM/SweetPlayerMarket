package top.mrxiaom.sweet.playermarket.commands;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
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
        if (args.length >= 1 && "create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
            if (!(sender instanceof Player)) {
                return t(sender, "只有玩家可以执行该命令");
            }
            Player player = (Player) sender;
            ItemStack item = player.getItemInHand();
            if (item.getType().equals(Material.AIR)) {
                return t(sender, "&e请手持你要上架的物品");
            }
            if (args.length == 1) {
                return t(sender, "&e请输入商店类型");
            }
            EnumMarketType type = Util.valueOrNull(EnumMarketType.class, args[1]);
            if (type == null) {
                return t(sender, "&e请输入正确的商品类型");
            }
            if (args.length == 2) {
                return t(sender, "&e请输入价格");
            }
            double price = Util.parseDouble(args[2]).orElse(0.0);
            if (price <= 0) {
                return t(sender, "&e请输入正确的价格");
            }
            IEconomy currency;
            if (args.length == 3) {
                currency = plugin.getVault();
            } else {
                currency = plugin.parseEconomy(args[3]);
                if (currency == null) {
                    return t(sender, "&e请输入正确的货币类型");
                }
            }
            if (currency instanceof VaultEconomy) {
                if (!sender.hasPermission("sweet.playermarket.create.currency.vault")) {
                    return t(sender, "&e你没有使用该货币上架商品的权限");
                }
            }
            if (currency instanceof PlayerPointsEconomy) {
                if (!sender.hasPermission("sweet.playermarket.create.currency.playerpoints")) {
                    return t(sender, "&e你没有使用该货币上架商品的权限");
                }
            }
            if (currency instanceof MPointsEconomy) {
                String sign = ((MPointsEconomy) currency).sign();
                if (!sender.hasPermission("sweet.playermarket.create.currency.mpoints." + sign)) {
                    return t(sender, "&e你没有使用该货币上架商品的权限");
                }
            }
            Integer itemAmount = args.length == 4
                    ? Integer.valueOf(item.getAmount())
                    : Util.parseInt(args[4]).orElse(null);
            if (itemAmount == null) {
                return t(sender, "&e请输入正确的单个商品的物品数量");
            }
            if (itemAmount > item.getMaxStackSize()) {
                return t(sender, "&e请输入正确的单个商品的物品数量，你输入的数量超出了堆叠限制");
            }
            if (itemAmount > item.getAmount()) {
                return t(sender, "&e请输入正确的单个商品的物品数量，你输入的数量超出了手持物品数量");
            }
            Integer marketAmount = args.length == 5
                    ? Integer.valueOf(1)
                    : Util.parseInt(args[5]).orElse(null);
            if (marketAmount == null || marketAmount < 1 || marketAmount > 64) {
                return t(sender, "&e请输入正确的商品总份数");
            }

            // TODO: 计算上架所需手续费用，并检查玩家够不够钱

            ItemStack shopItem = item.clone();
            shopItem.setAmount(itemAmount);

            int totalAmount = itemAmount * marketAmount;
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
                    return t(sender, "&e你没有足够的物品来上架商品");
                }
                Utils.takeItem(player, sample, totalAmount);
            }
            if (type.equals(EnumMarketType.BUY)) {
                // 收购商店，收取玩家指定类型的货币
                double totalPrice = price * marketAmount;
                if (!currency.has(player, totalPrice)) {
                    return t(sender, "&e你没有足够的货币来上架商品");
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
                return t(sender, "&a你的商品已成功上架到全球市场!");
            } else {
                return t(sender, "&e商品上架失败，请联系服务器管理员");
            }
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
        if (args.length == 4) {
            if ("create".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.playermarket.create")) {
                return startsWith(arg3Create, args[3]);
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
