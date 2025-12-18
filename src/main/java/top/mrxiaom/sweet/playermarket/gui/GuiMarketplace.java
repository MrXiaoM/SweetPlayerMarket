package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
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
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.util.List;

@AutoRegister
public class GuiMarketplace extends AbstractGuiSearch {
    private boolean canBuySelfItems;
    private LoadedIcon iconItemSelf;
    public GuiMarketplace(SweetPlayerMarket plugin) {
        super(plugin, "marketplace.yml");
    }

    public boolean canBuySelfItems() {
        return canBuySelfItems;
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        String resourceFile = "gui/" + filePath;
        canBuySelfItems = cfg.getBoolean("can-buy-self-items");
        super.reloadConfig(cfg);
        iconItemSelf = Utils.requireIconNotNull(this, resourceFile, iconItemSelf, "main-icons.物_自己");
    }

    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        super.reloadMenuConfig(config);
        iconItemSelf = null;
    }

    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {
        super.loadMainIcon(section, id, icon);
        if (id.equals("物_自己")) {
            iconItemSelf = icon;
        }
    }

    @Override
    protected LoadedIcon decideIconByMarketItem(SearchGui instance, Player player, MarketItem item, ListPair<String, Object> r) {
        if (!canBuySelfItems && item.playerId().equals(plugin.getKey(player))) {
            return iconItemSelf;
        }
        return iconItem;
    }

    public static GuiMarketplace inst() {
        return instanceOf(GuiMarketplace.class);
    }

    public static Impl create(Player player, Searching searching) {
        GuiMarketplace self = inst();
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
        protected void checkNeedToLockAction(char id) {
            if (id == '物') {
                actionLock = true;
            }
        }

        @Override
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
                ListPair<String, Object> r = new ListPair<>();
                r.add("__internal__index", i);
                r.add("__internal__market_item", marketItem);
                if (!canBuySelfItems && marketItem.playerId().equals(plugin.getKey(player))) {
                    iconItemSelf.click(player, click, r);
                    actionLock = false;
                    return;
                }
                iconItem.click(player, click, r);
                actionLock = false;
                return;
            }
            actionLock = false;
        }
    }
}
