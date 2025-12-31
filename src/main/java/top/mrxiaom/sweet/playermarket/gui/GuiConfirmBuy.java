package top.mrxiaom.sweet.playermarket.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IShopBuyConfirmAdapter;
import top.mrxiaom.sweet.playermarket.api.event.MarketConfirmBuyEvent;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.NoticeFlag;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.NoticeManager;
import top.mrxiaom.sweet.playermarket.func.ShopAdapterRegistry;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiConfirm;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class GuiConfirmBuy extends AbstractGuiConfirm {
    public GuiConfirmBuy(SweetPlayerMarket plugin) {
        super(plugin, "confirm-buy.yml");
    }

    public static GuiConfirmBuy inst() {
        return instanceOf(GuiConfirmBuy.class);
    }

    public static Impl create(Player player, GuiMarketplace.Impl parent, MarketItem marketItem) {
        GuiConfirmBuy self = inst();
        return self.new Impl(player, parent, marketItem);
    }

    public class Impl extends ConfirmGui {
        private final GuiMarketplace.Impl parent;
        private int amountCanSell;
        protected Impl(Player player, GuiMarketplace.Impl parent, MarketItem marketItem) {
            super(player, marketItem);
            this.parent = parent;
        }

        @Override
        public int getMaxCount() {
            return amountCanSell;
        }

        @Override
        protected void updateReplacements() {
            super.updateReplacements();
            ItemStack sample = marketItem.item();
            double invAmount = (double) getInvCount(sample) / sample.getAmount();
            amountCanSell = Math.min(marketItem.amount(), (int) Math.floor(invAmount));
            commonReplacements.add("%amount_can_sell%", amountCanSell);
        }

        @Override
        protected void checkNeedToLockAction(char id) {
            if (id == '确' || id == '返') {
                actionLock = true;
            }
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
            double totalMoney;
            int totalCount;
            actionLock = true;
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
                    Messages.Gui.buy__amount_not_enough.tm(player);
                    actionLock = false;
                    return;
                }
                // 检查商品适配器设置
                ShopAdapterRegistry.Entry entry = ShopAdapterRegistry.inst().getByMarketItem(marketItem);
                if (entry.hasFactoryParams()) {
                    // 如果有适配器，使用适配器的卖出逻辑
                    IShopBuyConfirmAdapter shopAdapter = entry.getBuyConfirmAdapter(marketItem, player);
                    if (shopAdapter == null) {
                        Messages.Gui.buy__adapter_not_found.tm(player);
                        return;
                    }
                    totalMoney = shopAdapter.getTotalMoney(count);
                    Integer total = shopAdapter.checkAndTake(count);
                    if (total == null) return;
                    totalCount = total;
                } else {
                    // 如果没有适配器，直接扣除玩家物品
                    ItemStack sample = marketItem.item();

                    totalMoney = count * marketItem.price();
                    totalCount = count * sample.getAmount();

                    // 检查玩家背包是否有足够的物品，并取走
                    int invCount = getInvCount(sample);
                    if (invCount < totalCount) {
                        Messages.Gui.buy__item_not_enough.tm(player);
                        actionLock = false;
                        return;
                    }
                    Utils.takeItem(player, sample, totalCount);
                }

                ConfigurationSection params = marketItem.params();
                // 添加物品到额外参数中，让商家自行领取
                List<Object> itemList = new ArrayList<>(params.getList("buy.received-items", new ArrayList<>()));
                for (int i = 0; i < count; i++) {
                    itemList.add(marketItem.item());
                }
                params.set("buy.received-items", itemList);

                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .noticeFlag(NoticeFlag.CAN_CLAIM_ITEMS)
                        .amount(finalAmount)
                        .params(params)
                        .build()
                )) {
                    Messages.Gui.buy__submit_failed.tm(player);
                    actionLock = false;
                    return;
                }
            } catch (Throwable e) {
                warn("玩家 " + player.getName() + " 在下单 " + this.marketItem.playerName() + " 的收购商品 " + this.marketItem.shopId() + " 时出现异常", e);
                player.closeInventory();
                Messages.Gui.buy__exception.tm(player);
                return;
            }
            // 给予玩家的指定数量货币。由于卖家上架时已收取货币，不需要拿走卖家的货币
            currency.giveMoney(player, totalMoney);
            // 获取物品名，提示玩家卖出成功
            ItemStack itemDisplay = marketItem.item();
            MiniMessage miniMessage = AdventureItemStack.wrapHoverEvent(itemDisplay).build();
            Messages.Gui.buy__success.tm(miniMessage, player,
                    Pair.of("%item%", plugin.displayNames().getDisplayName(itemDisplay, player)),
                    Pair.of("%total_count%", totalCount),
                    Pair.of("%money%", String.format("%.2f", totalMoney).replace(".00", "")),
                    Pair.of("%currency%", currencyName));
            parent.doSearch();
            parent.open();
            NoticeManager.inst().confirmNotice(marketItem);

            double finalTotalMoney = totalMoney;
            plugin.getScheduler().runTask(() -> {
                MarketConfirmBuyEvent e = new MarketConfirmBuyEvent(marketItem, player, count, totalCount, finalTotalMoney, currency);
                Bukkit.getPluginManager().callEvent(e);
            });
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

        private int getInvCount(ItemStack sample) {
            int invAmount = 0;
            PlayerInventory inventory = player.getInventory();
            ItemStack[] contents = inventory.getContents();
            for (ItemStack content : contents) {
                if (content != null && content.isSimilar(sample)) {
                    invAmount += content.getAmount();
                }
            }
            return invAmount;
        }
    }
}
