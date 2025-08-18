package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
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
        public int getMaxCount() {
            return marketItem.amount();
        }

        @Override
        protected void onClickConfirm(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event
        ) {
            IEconomy currency;
            String currencyName;
            double totalMoney;
            actionLock = true;
            try (Connection conn = plugin.getConnection()) {
                MarketplaceDatabase db = plugin.getMarketplace();
                MarketItem marketItem = db.getItem(conn, this.marketItem.shopId());
                if (marketItem == null || marketItem.amount() == 0) {
                    Messages.Gui.common__item_not_found.tm(player);
                    parent.doSearch(false);
                    parent.open();
                    return;
                }
                currency = marketItem.currency();
                currencyName = plugin.displayNames().getCurrencyName(marketItem.currencyName());
                if (currency == null) {
                    Messages.Gui.common__currency_not_found.tm(player, Pair.of("%currency%", currencyName));
                    actionLock = false;
                    return;
                }
                int finalAmount = marketItem.amount() - count;
                if (finalAmount < 0) {
                    Messages.Gui.sell__amount_not_enough.tm(player);
                    actionLock = false;
                    return;
                }
                totalMoney = count * marketItem.price();
                if (!currency.has(player, totalMoney)) {
                    Messages.Gui.sell__currency_not_enough.tm(player, Pair.of("%currency%", currencyName));
                    actionLock = false;
                    return;
                }

                // 添加货币到额外参数中，让商家自行领取
                ConfigurationSection params = marketItem.params();
                double old = params.getDouble("sell.received-currency", 0.0);
                params.set("sell.received-currency", old + totalMoney);

                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .noticeFlag(1)
                        .amount(finalAmount)
                        .params(params)
                        .build()
                )) {
                    Messages.Gui.sell__submit_failed.tm(player);
                    actionLock = false;
                    return;
                }
            } catch (Throwable e) {
                warn("玩家 " + player.getName() + " 在下单 " + marketItem.playerName() + " 的出售商品 " + marketItem.shopId() + " 时出现异常", e);
                player.closeInventory();
                Messages.Gui.sell__exception.tm(player);
                return;
            }
            // 拿走玩家的指定数量货币。由于上方已添加货币到额外参数中，不需要给予卖家货币
            currency.takeMoney(player, totalMoney);
            // 给予玩家物品
            for (int i = 0; i < count; i++) {
                ItemStack item = marketItem.item();
                ItemStackUtil.giveItemToPlayer(player, item);
            }
            // TODO: 获取物品名，提示玩家购买成功
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
