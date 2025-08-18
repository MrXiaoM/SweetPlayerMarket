package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.OfflinePlayer;
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
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiConfirm;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class GuiConfirmBuy extends AbstractGuiConfirm {
    public GuiConfirmBuy(SweetPlayerMarket plugin) {
        super(plugin, "gui/confirm-buy.yml");
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
        protected void onClickConfirm(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event
        ) {
            IEconomy currency;
            String currencyName;
            OfflinePlayer owner;
            double totalMoney;
            int totalCount;
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
                    t(player, "&e商品库存不足，减少一点卖出数量吧~");
                    actionLock = false;
                    return;
                }
                ItemStack sample = marketItem.item();

                totalMoney = count * marketItem.price();
                totalCount = count * sample.getAmount();

                // 检查玩家背包是否有足够的物品，并取走
                int invCount = getInvCount(sample);
                if (invCount < totalCount) {
                    t(player, "&e你没有足够的物品来卖出");
                    actionLock = false;
                    return;
                }
                Utils.takeItem(player, sample, totalCount);

                // 添加物品到额外参数中
                ConfigurationSection params = marketItem.params();
                List<ItemStack> itemList = new ArrayList<>();
                for (Object obj : params.getList("buy.received-items", new ArrayList<>())) {
                    if (obj instanceof ItemStack) {
                        itemList.add((ItemStack) obj);
                    }
                }
                for (int i = 0; i < count; i++) {
                    itemList.add(marketItem.item());
                }
                params.set("buy.received-items", itemList);

                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .amount(finalAmount)
                        .params(params)
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

            // 给予玩家的指定数量货币。由于卖家上架时已收取货币，不需要拿走卖家的货币
            currency.giveMoney(player, totalMoney);

            // TODO: 提示玩家卖出成功
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
