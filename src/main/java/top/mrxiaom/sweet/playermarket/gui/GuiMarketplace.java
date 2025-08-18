package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.EnumSort;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.util.List;

@AutoRegister
public class GuiMarketplace extends AbstractGuiSearch {
    public GuiMarketplace(SweetPlayerMarket plugin) {
        super(plugin, "gui/marketplace.yml");
    }

    public static GuiMarketplace inst() {
        return instanceOf(GuiMarketplace.class);
    }

    public static Impl create(Player player, Searching searching) {
        GuiMarketplace self = inst();
        return self.new Impl(player, searching);
    }

    public class Impl extends SearchGui {
        private int columnIndex = -1;
        protected Impl(Player player, Searching searching) {
            super(player, searching);
            List<String> columnList = plugin.displayNames().columnList();
            for (int i = 0; i < columnList.size(); i++) {
                if (columnList.get(i).equals("create_time")) {
                    columnIndex = i;
                    break;
                }
            }
            postInit();
        }

        public void switchOrderColumn() {
            int i = ++columnIndex;
            List<String> columnList = plugin.displayNames().columnList();
            if (i >= columnList.size()) {
                i = columnIndex = 0;
            }
            searching.orderColumn(columnList.get(i));
        }

        public void switchOrderSortType() {
            if (searching.orderType() == EnumSort.ASC) {
                searching.orderType(EnumSort.DESC);
            } else {
                searching.orderType(EnumSort.ASC);
            }
        }

        @Override
        @SuppressWarnings("UnnecessaryReturnStatement")
        protected void onClickMarketItem(InventoryAction action, ClickType click, InventoryType.SlotType slotType, int slot, MarketItem item, int i, InventoryView view, InventoryClickEvent event) {
            if (item.amount() == 0) {
                actionLock = false;
                Messages.Gui.common__item_not_found.tm(player);
                return;
            }
            if (click.isLeftClick()) {
                MarketItem marketItem = refreshItem(item);
                if (marketItem == null || marketItem.amount() == 0) {
                    items.set(i, item.toBuilder().amount(0).build());
                    actionLock = false;
                    Messages.Gui.common__item_not_found.tm(player);
                    return;
                }
                if (item.type().equals(EnumMarketType.SELL)) {
                    GuiConfirmSell.create(player, this, marketItem).open();
                    return;
                }
                if (item.type().equals(EnumMarketType.BUY)) {
                    GuiConfirmBuy.create(player, this, marketItem).open();
                    return;
                }
            }
        }
    }
}
