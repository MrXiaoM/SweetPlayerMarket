package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.time.LocalDateTime;
import java.util.List;

@AutoRegister
public class GuiMyItems extends AbstractGuiSearch {
    public GuiMyItems(SweetPlayerMarket plugin) {
        super(plugin, "my-items.yml");
    }

    LoadedIcon iconClaim;
    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        super.reloadMenuConfig(config);
        iconClaim = null;
    }

    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {
        super.loadMainIcon(section, id, icon);
        if (id.equals("领")) {
            iconClaim = icon;
        }
    }

    @Override
    protected LoadedIcon decideIconByMarketItem(SearchGui instance, Player player, MarketItem item, ListPair<String, Object> r) {
        if (item.noticeFlag() == 1) {
            int amountCanTake = 0;
            if (item.type().equals(EnumMarketType.SELL)) {
                amountCanTake = item.params().getInt("sell.received-count");
            }
            if (item.type().equals(EnumMarketType.BUY)) {
                List<?> list = item.params().getList("buy.received-items");
                amountCanTake = list == null ? 0 : list.size();
            }
            if (LocalDateTime.now().isAfter(item.outdateTime()) && item.amount() > 0) {
                amountCanTake += item.amount();
            }
            if (amountCanTake > 0) {
                r.add("%amount_can_take%", amountCanTake);
                return iconClaim;
            }
        }
        return iconItem;
    }

    public static GuiMyItems inst() {
        return instanceOf(GuiMyItems.class);
    }

    public static Impl create(Player player, Searching searching) {
        GuiMyItems self = inst();
        return self.new Impl(player, searching);
    }

    public class Impl extends SearchGui {
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

        @Override
        protected void onClickMarketItem(InventoryAction action, ClickType click, InventoryType.SlotType slotType, int slot, MarketItem item, int i, InventoryView view, InventoryClickEvent event) {
            ListPair<String, Object> r = new ListPair<>();
            r.add("__internal__market_item", item);
            r.add("__internal__index", i);
            if (item.noticeFlag() == 1) {
                iconClaim.click(player, click, r);
            } else {
                iconItem.click(player, click, r);
            }
            actionLock = false;
        }
    }
}
