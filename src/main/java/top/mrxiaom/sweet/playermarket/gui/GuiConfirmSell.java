package top.mrxiaom.sweet.playermarket.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IShopAdapterFactory;
import top.mrxiaom.sweet.playermarket.api.IShopSellConfirmAdapter;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.NoticeManager;
import top.mrxiaom.sweet.playermarket.func.ShopAdapterRegistry;
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
            MarketItem marketItem;
            IEconomy currency;
            String currencyName;
            double totalMoney = 0;
            IShopSellConfirmAdapter shopAdapter = null;
            actionLock = true;
            IEconomy shouldReturnMoneyWhenException = null;
            try (Connection conn = plugin.getConnection()) {
                MarketplaceDatabase db = plugin.getMarketplace();
                marketItem = db.getItem(conn, this.marketItem.shopId());
                if (marketItem == null || marketItem.amount() == 0) {
                    Messages.Gui.common__item_not_found.tm(player);
                    parent.doSearch();
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

                ConfigurationSection params = marketItem.params();
                // 检查商品适配器设置
                String factoryId = params.getString("adapter.factory-id", null);
                if (factoryId != null) {
                    IShopAdapterFactory factory = ShopAdapterRegistry.inst().getById(factoryId);
                    shopAdapter = factory == null ? null : factory.getSellConfirmAdapter(marketItem, player);
                    if (shopAdapter == null) {
                        Messages.Gui.sell__adapter_not_found.tm(player);
                        return;
                    }
                }
                // 添加货币到额外参数中，让商家自行领取
                double old = params.getDouble("sell.received-currency", 0.0);
                params.set("sell.received-currency", old + totalMoney);
                int oldCount = params.getInt("sell.received-count", 0);
                params.set("sell.received-count", oldCount + count);

                // 拿走玩家的指定数量货币。由于上方已添加货币到额外参数中，不需要给予卖家货币
                if (!currency.takeMoney(player, totalMoney)) {
                    Messages.Gui.sell__currency_not_enough.tm(player, Pair.of("%currency%", currencyName));
                    actionLock = false;
                    return;
                }
                shouldReturnMoneyWhenException = currency;
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
                warn("玩家 " + player.getName() + " 在下单 " + this.marketItem.playerName() + " 的出售商品 " + this.marketItem.shopId() + " 时出现异常", e);
                player.closeInventory();
                Messages.Gui.sell__exception.tm(player);
                if (shouldReturnMoneyWhenException != null && totalMoney > 0) {
                    shouldReturnMoneyWhenException.giveMoney(player, totalMoney);
                }
                return;
            }

            int totalCount;
            if (shopAdapter != null) {
                // 如果有商品适配器，则按适配器的实现来给予玩家物品
                totalCount = shopAdapter.giveToPlayer(count);
            } else {
                int total = 0;
                // 如果没有商品适配器，直接给予玩家物品
                for (int i = 0; i < count; i++) {
                    ItemStack item = marketItem.item();
                    total += item.getAmount();
                    ItemStackUtil.giveItemToPlayer(player, item);
                }
                totalCount = total;
            }
            // 获取物品名，提示玩家购买成功
            ItemStack itemDisplay = marketItem.item();
            MiniMessage miniMessage = AdventureItemStack.wrapHoverEvent(itemDisplay).build();
            Messages.Gui.sell__success.tm(miniMessage, player,
                    Pair.of("%item%", plugin.displayNames().getDisplayName(itemDisplay, player)),
                    Pair.of("%total_count%", totalCount),
                    Pair.of("%money%", String.format("%.2f", totalMoney).replace(".00", "")),
                    Pair.of("%currency%", currencyName));
            parent.doSearch();
            parent.open();
            NoticeManager.inst().confirmNotice(marketItem);
        }

        @Override
        protected void onClickBack(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event
        ) {
            actionLock = true;
            parent.doSearch();
            parent.open();
        }
    }
}
