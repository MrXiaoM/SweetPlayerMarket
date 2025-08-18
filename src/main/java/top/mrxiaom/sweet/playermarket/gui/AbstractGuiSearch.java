package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.IModifier;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractGuiSearch extends AbstractGuiModule {
    private final String filePath;
    protected LoadedIcon iconItem, iconEmpty;
    public AbstractGuiSearch(SweetPlayerMarket plugin, String file) {
        super(plugin, plugin.resolve("./" + file));
        this.filePath = file;
    }

    @Override
    protected String warningPrefix() {
        return "[" + filePath + "]";
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        if (!file.exists()) {
            plugin.saveResource(filePath, file);
        }
        super.reloadConfig(cfg);
    }

    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        iconItem = null;
        iconEmpty = null;
    }

    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {
        if (id.equals("物")) {
            iconItem = icon;
        }
        if (id.equals("空")) {
            iconEmpty = icon;
        }
    }

    @Override
    protected ItemStack applyMainIcon(IGui instance, Player player, char id, int index, int appearTimes) {
        SearchGui gui = (SearchGui) instance;
        if (id == '物') {
            int i = appearTimes - 1;
            MarketItem item = gui.getItem(i);
            if (item == null) {
                return iconEmpty.generateIcon(player);
            } else {
                ItemStack baseItem = item.item();
                int displayAmount = baseItem.getAmount();
                String itemName = plugin.getItemName(baseItem);
                List<String> itemLore = AdventureItemStack.getItemLoreAsMiniMessage(baseItem);

                ListPair<String, Object> r = new ListPair<>();
                r.addAll(gui.commonReplacements);
                r.add("%display%", itemName);
                r.add("%player%", item.playerName());
                r.add("%type%", plugin.displayNames().getMarketTypeName(item.type()));
                r.add("%amount%", item.amount());
                r.add("%price%", item.price());
                r.add("%currency%", plugin.displayNames().getCurrencyName(item.currencyName()));
                r.add("%create_time%", plugin.toString(item.createTime()));
                r.add("%outdate_time%", plugin.toString(item.outdateTime()));

                IModifier<String> displayModifier = oldName -> Pair.replace(oldName, r);
                IModifier<List<String>> loreModifier = oldLore -> {
                    List<String> lore = new ArrayList<>();
                    for (String s : oldLore) {
                        if (s.equals("item lore")) {
                            lore.addAll(itemLore);
                            continue;
                        }
                        lore.add(Pair.replace(s, r));
                    }
                    return lore;
                };
                ItemStack icon = iconItem.generateIcon(baseItem, player, displayModifier, loreModifier);
                icon.setAmount(displayAmount);
                return icon;
            }
        }
        IModifier<String> displayModifier = oldName -> Pair.replace(oldName, gui.commonReplacements);
        IModifier<List<String>> loreModifier = oldLore -> Pair.replace(oldLore, gui.commonReplacements);
        LoadedIcon icon = otherIcons.get(id);
        if (icon != null) {
            return icon.generateIcon(player, displayModifier, loreModifier);
        }
        return null;
    }

    public abstract class SearchGui extends Gui implements InventoryHolder, Refreshable, Pageable {
        protected final List<MarketItem> items = new ArrayList<>();
        protected final int slotsSize;
        protected Inventory inventory;
        protected Searching searching;
        protected int pages = 1;
        protected boolean actionLock = false;
        protected final ListPair<String, Object> commonReplacements = new ListPair<>();
        protected SearchGui(Player player, Searching searching) {
            super(player, guiTitle, guiInventory);
            int itemsSize = 0;
            for (char c : super.inventory) {
                if (c == '物') itemsSize++;
            }
            this.slotsSize = itemsSize;
            this.searching = searching;
        }

        protected void postInit() {
            doSearch(false);
        }

        @Nullable
        public MarketItem getItem(int index) {
            return index < 0 || index >= items.size() ? null : items.get(index);
        }

        public int getItemsSize() {
            return items.size();
        }

        public void doSearch(boolean refreshInv) {
            items.clear();
            items.addAll(plugin.getMarketplace().getItems(pages, slotsSize, searching));
            if (refreshInv) {
                open();
            }
        }

        @Override
        public void refreshGui() {
            doSearch(false);
            updateInventory(inventory);
            Util.submitInvUpdate(player);
        }

        @Override
        public void turnPageUp(int pages) {
            if (this.pages - pages < 1) return;
            this.pages -= pages;
            doSearch(true);
        }

        @Override
        public void turnPageDown(int pages) {
            List<MarketItem> items = plugin.getMarketplace().getItems(this.pages + pages, slotsSize, searching);
            if (items.isEmpty()) return;
            this.pages += pages;
            this.items.clear();
            this.items.addAll(items);
            open();
        }

        public Searching searching() {
            return searching;
        }

        public void searching(Searching searching) {
            this.searching = searching;
        }

        @Override
        protected Inventory create(InventoryHolder holder, int size, String title) {
            return this.inventory = super.create(this, size, title.replace("%page%", String.valueOf(pages)));
        }

        @Override
        public void updateInventory(BiConsumer<Integer, ItemStack> setItem) {
            updateReplacements();
            super.updateInventory(setItem);
            actionLock = false;
        }

        protected void updateReplacements() {
            ListPair<String, Object> r = commonReplacements;
            r.clear();
            r.add("%search_type%", plugin.displayNames().getMarketTypeName(searching.type()));
            r.add("%search_currency%", plugin.displayNames().getCurrencyName(searching.currency()));
            r.add("%search_sort_column%", plugin.displayNames().getColumnName(searching.orderColumn()));
            r.add("%search_sort_type%", plugin.displayNames().getSortName(searching.orderType()));
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        @Override
        public void onClick(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                ItemStack currentItem, ItemStack cursor,
                InventoryView view, InventoryClickEvent event
        ) {
            event.setCancelled(true);
            if (actionLock) return;
            Character clickedId = getClickedId(slot);
            if (clickedId == null) return;
            if (clickedId == '物') {
                actionLock = true;
                int i = getAppearTimes(clickedId, slot) - 1;
                MarketItem item = getItem(i);
                if (item == null) {
                    actionLock = false;
                    return;
                }
                onClickMarketItem(action, click, slotType, slot, item, i, view, event);
                return;
            }
            if (onClickMainIcons(action, click, slotType, slot, clickedId, view, event)) {
                return;
            }
            handleOtherClick(click, clickedId);
        }

        protected abstract void onClickMarketItem(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                MarketItem item, int i,
                InventoryView view, InventoryClickEvent event);

        protected boolean onClickMainIcons(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                Character clickedId,
                InventoryView view, InventoryClickEvent event
        ) {
            return false;
        }

        protected MarketItem refreshItem(MarketItem item) {
            return plugin.getMarketplace().getItem(item.shopId());
        }
    }
}
