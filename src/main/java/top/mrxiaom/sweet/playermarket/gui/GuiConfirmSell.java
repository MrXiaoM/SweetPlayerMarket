package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiConfirm;

import java.sql.Connection;

@AutoRegister
public class GuiConfirmSell extends AbstractGuiConfirm {
    public GuiConfirmSell(SweetPlayerMarket plugin) {
        super(plugin, "gui/confirm-sell.yml");
    }

    public static GuiConfirmSell inst() {
        return instanceOf(GuiConfirmSell.class);
    }

    public static Impl create(Player player, GuiMarketplace.Impl parent, MarketItem marketItem) {
        GuiConfirmSell self = inst();
        return self.new Impl(player, parent, marketItem);
    }

    public class Impl extends ConfirmGui {
        private final GuiMarketplace.Impl parent;
        protected Impl(Player player, GuiMarketplace.Impl parent, MarketItem marketItem) {
            super(player, marketItem);
            this.parent = parent;
        }

        @Override
        protected void onClickConfirm(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event
        ) {
            IEconomy currency;
            String currencyName;
            OfflinePlayer owner;
            double totalMoney;
            actionLock = true;
            try (Connection conn = plugin.getConnection()) {
                MarketplaceDatabase db = plugin.getMarketplace();
                MarketItem marketItem = db.getItem(conn, this.marketItem.shopId());
                if (marketItem == null || marketItem.amount() == 0) {
                    t(player, "&e来晚了，该商品已下架");
                    parent.doSearch(false);
                    parent.open();
                    return;
                }
                currency = marketItem.currency();
                currencyName = plugin.displayNames().getCurrencyName(marketItem.currencyName());
                if (currency == null) {
                    t(player, "&e在该子服不支持使用" + currencyName + "货币");
                    actionLock = false;
                    return;
                }
                owner = plugin.getPlayer(marketItem.playerId());
                if (owner == null) {
                    t(player, "&e店主的玩家数据在这个子服不存在，无法购买他的商品");
                    actionLock = false;
                    return;
                }
                int finalAmount = marketItem.amount() - count;
                if (finalAmount < 0) {
                    t(player, "&e商品库存不足，减少一点购买数量吧~");
                    actionLock = false;
                    return;
                }
                totalMoney = count * marketItem.price();
                if (!currency.has(player, totalMoney)) {
                    t(player, "&e你没有足够的" + currencyName);
                    actionLock = false;
                    return;
                }
                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .amount(finalAmount)
                        .build())) {
                    t(player, "&e数据库更改提交失败，可能该商品已下架");
                    actionLock = false;
                    return;
                }
            } catch (Throwable e) {
                warn("玩家 " + player.getName() + " 在下单商品 " + marketItem.shopId() + " 时出现异常", e);
                player.closeInventory();
                t(player, "&e出现错误，已打印日志到控制台，请联系服务器管理员");
                return;
            }
            // 拿走玩家的指定数量货币
            currency.takeMoney(player, totalMoney);
            // 给予卖家货币
            currency.giveMoney(owner, totalMoney);
            // 给予玩家物品
            for (int i = 0; i < count; i++) {
                ItemStack item = marketItem.item();
                ItemStackUtil.giveItemToPlayer(player, item);
            }
            // TODO: 提示玩家购买成功
            parent.doSearch(false);
            parent.open();
        }

        @Override
        protected void onClickBack(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event
        ) {
            actionLock = true;
            parent.doSearch(false);
            parent.open();
        }
    }
}
