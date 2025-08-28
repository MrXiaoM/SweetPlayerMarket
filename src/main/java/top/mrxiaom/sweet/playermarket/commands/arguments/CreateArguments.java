package top.mrxiaom.sweet.playermarket.commands.arguments;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.commands.CommandMain;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.OutdateTime;
import top.mrxiaom.sweet.playermarket.data.limitation.BaseLimitation;
import top.mrxiaom.sweet.playermarket.data.limitation.CreateCost;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.economy.MPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.PlayerPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.VaultEconomy;
import top.mrxiaom.sweet.playermarket.func.LimitationManager;
import top.mrxiaom.sweet.playermarket.func.NoticeManager;
import top.mrxiaom.sweet.playermarket.func.OutdateTimeManager;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.sql.Connection;
import java.sql.SQLException;

public class CreateArguments extends AbstractArguments<Player> {
    protected CreateArguments(CommandArguments args) {
        super(args);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean execute(SweetPlayerMarket plugin, Player sender) {
        ItemStack item = sender.getItemInHand();
        if (item.getType().equals(Material.AIR)) {
            return Messages.Command.create__no_item.tm(sender);
        }
        // 商品类型
        EnumMarketType type = nextValueOf(EnumMarketType.class);
        if (type == null) {
            return Messages.Command.create__no_type_found.tm(sender);
        }
        // 商品单价
        double price = nextDouble(0.0);
        if (price < 0.01) {
            return Messages.Command.create__no_price_valid.tm(sender);
        }
        // 商品货币类型
        IEconomy currency = nextOptional(currencyName -> {
            if (currencyName == null) {
                IEconomy parsed = plugin.parseEconomy(CommandMain.inst().defaultCurrency());
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
        // 货币使用权限限制
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
        // 单份商品的物品数量
        Integer itemCount = nextInt(item::getAmount, NULL());
        if (itemCount == null) {
            return Messages.Command.create__no_item_count_valid.tm(sender);
        }
        if (itemCount > item.getMaxStackSize()) {
            return Messages.Command.create__no_item_count_valid_stack.tm(sender);
        }
        if (itemCount > item.getAmount()) {
            return Messages.Command.create__no_item_count_valid_held.tm(sender);
        }
        // 商品总份数
        Integer marketAmount = nextInt(() -> 1, NULL());
        if (marketAmount == null || marketAmount < 1 || marketAmount > 64) {
            return Messages.Command.create__no_amount_valid.tm(sender);
        }

        // 检查商品上架条件
        BaseLimitation limitation = LimitationManager.inst().getLimitByItem(item);
        if (!sender.hasPermission("sweet.playermarket.create.bypass.type") && !limitation.canUseMarketType(type)) {
            return Messages.Command.create__limitation__type_not_allow.tm(sender);
        }
        if (!sender.hasPermission("sweet.playermarket.create.bypass.currency") && !limitation.canUseCurrency(currency)) {
            return Messages.Command.create__limitation__currency_not_allow.tm(sender,
                    Pair.of("%currency%", plugin.displayNames().getCurrencyName(currency)));
        }
        // 检查玩家是否有足够的手续费
        double totalPrice = price * marketAmount;
        CreateCost createCost = limitation.getCreateCost(type);
        IEconomy costCurrency;
        double createCostMoney;
        if (!sender.hasPermission("sweet.playermarket.create.bypass.cost") && createCost != null) {
            costCurrency = createCost.currency(currency);
            createCostMoney = createCost.money(totalPrice);
            if (createCostMoney > 0 && !costCurrency.has(sender, createCostMoney)) {
                return Messages.Command.create__limitation__create_cost_failed.tm(sender,
                        Pair.of("%currency%", plugin.displayNames().getCurrencyName(costCurrency)),
                        Pair.of("%money%", String.format("%.2f", createCostMoney).replace(".00", "")));
            }
        } else {
            costCurrency = null;
            createCostMoney = 0.0;
        }

        OutdateTime outdateTime = OutdateTimeManager.inst().get(sender);

        try (Connection conn = plugin.getConnection()) {
            MarketplaceDatabase db = plugin.getMarketplace();
            String shopId = db.createNewId(conn);
            if (shopId == null) {
                return Messages.Command.create__failed_db.tm(sender);
            }

            ItemStack shopItem = item.clone();
            shopItem.setAmount(itemCount);

            int totalAmount = itemCount * marketAmount;
            switch (type) {
                case SELL: {
                    // 出售商店，检查玩家背包里有没有这么多的物品，并拿走这些物品
                    int invAmount = Utils.getItemAmount(sender, shopItem);
                    if (invAmount < totalAmount) {
                        return Messages.Command.create__sell__no_enough_items.tm(sender);
                    }
                    Utils.takeItem(sender, shopItem, totalAmount);
                    break;
                }
                case BUY: {
                    // 收购商店，收取玩家指定类型的货币
                    double totalMoney = createCost != null && createCost.isTheSameCurrency(currency)
                            ? (totalPrice + createCostMoney)
                            : (totalPrice);
                    if (!currency.has(sender, totalMoney)) {
                        return Messages.Command.create__buy__no_enough_currency.tm(sender);
                    }
                    currency.takeMoney(sender, totalPrice);
                    break;
                }
                default: {
                    return Messages.Command.create__no_type_found.tm(sender);
                }
            }

            // 扣除手续费
            if (costCurrency != null && createCostMoney > 0) {
                costCurrency.takeMoney(sender, createCostMoney);
            }

            // 将商品信息提交到数据库
            db.putItem(conn, MarketItem.builder(shopId, sender)
                    .item(shopItem)
                    .type(type)
                    .price(price)
                    .currency(currency)
                    .amount(marketAmount)
                    .outdateTime(outdateTime.get(type))
                    .build(plugin.itemTagResolver()));
        } catch (SQLException e) {
            plugin.warn("玩家 " + sender.getName() + " 上架商品失败", e);
            return Messages.Command.create__failed.tm(sender);
        }
        // 通过 BungeeCord 通知其它子服已打开的界面，应该刷新全球市场菜单
        NoticeManager.inst().updateCreated();
        // 提示商品上架成功
        MiniMessage miniMessage = AdventureItemStack.wrapHoverEvent(item).build();
        return Messages.Command.create__success.tm(miniMessage, sender,
                Pair.of("%item%", plugin.displayNames().getDisplayName(item, sender)));
    }

    public static CreateArguments of(CommandArguments args) {
        return new CreateArguments(args);
    }
}
