package top.mrxiaom.sweet.playermarket.gui.api;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.IModifier;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumSort;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;
import top.mrxiaom.sweet.playermarket.func.ShopAdapterRegistry;
import top.mrxiaom.sweet.playermarket.utils.ListX;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractGuiSearch extends AbstractGuiModule {
    private final String filePath;
    protected LoadedIcon iconItem, iconEmpty;
    public AbstractGuiSearch(SweetPlayerMarket plugin, String file) {
        super(plugin, plugin.resolve("./gui/" + file));
        this.filePath = file;
    }

    @Override
    protected String warningPrefix() {
        return "[" + filePath + "]";
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        File guiFolder = plugin.resolve(cfg.getString("gui-folder", "./gui"));
        this.file = new File(guiFolder, filePath);
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

    protected LoadedIcon decideIconByMarketItem(SearchGui instance, Player player, MarketItem item, ListPair<String, Object> r) {
        return iconItem;
    }

    @Override
    protected ItemStack applyMainIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes) {
        SearchGui gui = (SearchGui) instance;
        if (id == '物') {
            int i = appearTimes - 1;
            MarketItem item = gui.getItem(i);
            if (item == null) {
                return iconEmpty.generateIcon(player);
            } else {
                ItemStack baseItem = item.item();
                int displayAmount = baseItem.getAmount();
                String itemName = plugin.displayNames().getDisplayName(baseItem, player);
                List<String> itemLore = AdventureItemStack.getItemLoreAsMiniMessage(baseItem);

                ListPair<String, Object> r = new ListPair<>();
                r.addAll(gui.commonReplacements);
                r.add("%display%", itemName);
                applyMarketItemPlaceholders(plugin, item, r);

                ShopAdapterRegistry.Entry entry = ShopAdapterRegistry.inst().getByMarketItem(item);
                entry.updateReplacements(item, player, r);

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
                LoadedIcon loadedIcon = decideIconByMarketItem(gui, player, item, r);
                ItemStack icon = loadedIcon.generateIcon(baseItem, player, displayModifier, loreModifier);
                icon.setAmount(displayAmount);
                return entry.postProcessIcon(item, player, r, icon);
            }
        }
        return null;
    }

    @Override
    protected @Nullable ItemStack applyOtherIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes, LoadedIcon icon) {
        SearchGui gui = (SearchGui) instance;
        IModifier<String> displayModifier = oldName -> Pair.replace(oldName, gui.commonReplacements);
        IModifier<List<String>> loreModifier = oldLore -> Pair.replace(oldLore, gui.commonReplacements);
        return icon.generateIcon(player, displayModifier, loreModifier);
    }

    public abstract class SearchGui extends Gui implements IGuiRefreshable, IGuiPageable {
        public final SweetPlayerMarket plugin = AbstractGuiSearch.this.plugin;
        protected final ListX<MarketItem> items = new ListX<>();
        protected final int slotsSize;
        protected Searching searching;
        protected int pages = 1;
        protected boolean actionLock = false;
        protected final ListPair<String, Object> commonReplacements = new ListPair<>();
        protected int columnIndex = -1;
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
            doSearch();
        }

        @Nullable
        public MarketItem getItem(int index) {
            return index < 0 || index >= items.size() ? null : items.get(index);
        }

        public void setItem(int index, @NotNull MarketItem item) {
            if (index < 0 || index >= items.size()) return;
            items.set(index, item);
        }

        public int getItemsSize() {
            return items.size();
        }

        public void doSearch() {
            ListX<MarketItem> items = plugin.getMarketplace().getItems(pages, slotsSize, searching);
            this.items.clear();
            items.copyTo(this.items);
        }

        @Override
        public void refreshGui() {
            doSearch();
            updateInventory(getInventory());
            Util.submitInvUpdate(player);
        }

        @Override
        public void turnPageUp(int pages) {
            if (this.pages - pages < 1) return;
            this.pages -= pages;
            doSearch();
            open();
        }

        @Override
        public void turnPageDown(int pages) {
            ListX<MarketItem> items = plugin.getMarketplace().getItems(this.pages + pages, slotsSize, searching);
            if (items.isEmpty()) return;
            this.pages += pages;
            this.items.clear();
            items.copyTo(this.items);
            open();
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

        public Searching searching() {
            return searching;
        }

        public void searching(Searching searching) {
            this.searching = searching;
        }

        @Override
        public void updateInventory(BiConsumer<Integer, ItemStack> setItem) {
            updateReplacements();
            super.updateInventory(setItem);
            actionLock = false;
        }

        @Override
        protected Inventory create(int size, String title) {
            int maxPage = items.getMaxPage(slotsSize);
            return super.create(size, Pair.replace0(title,
                    Pair.of("%page%", pages),
                    Pair.of("%max_page%", maxPage == 0 ? "?" : maxPage)
            ));
        }

        protected void updateReplacements() {
            ListPair<String, Object> r = commonReplacements;
            r.clear();
            r.add("%search_type%", plugin.displayNames().getMarketTypeName(searching.type()));
            r.add("%search_currency%", plugin.displayNames().getCurrencyName(searching.currency()));
            r.add("%search_sort_column%", plugin.displayNames().getColumnName(searching.orderColumn()));
            r.add("%search_sort_type%", plugin.displayNames().getSortName(searching.orderType()));
            r.add("%search_outdate%", bool(searching.outdated()));
            r.add("%search_out_of_stock%", bool(searching.onlyOutOfStock()));
            Integer notice = searching.notice();
            r.add("%search_notice%", bool(notice != null && notice == 1));
        }

        private String bool(boolean b) {
            return (b ? Messages.Gui.common__yes : Messages.Gui.common__no).str();
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
            plugin.getScheduler().runTask(() -> {
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
            });
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

    public static void applyMarketItemPlaceholders(SweetPlayerMarket plugin, MarketItem item, ListPair<String, Object> r) {
        r.add("%player%", item.playerName());
        r.add("%type%", plugin.displayNames().getMarketTypeName(item.type()));
        r.add("%amount%", item.amount());
        r.add("%amount_original%", item.params().getInt("original-amount"));
        r.add("%price%", item.price());
        r.add("%currency%", plugin.displayNames().getCurrencyName(item.currencyName()));
        r.add("%create_time%", plugin.toString(item.createTime()));
        r.add("%outdate_time%", plugin.toString(item.outdateTime()));
    }
}
