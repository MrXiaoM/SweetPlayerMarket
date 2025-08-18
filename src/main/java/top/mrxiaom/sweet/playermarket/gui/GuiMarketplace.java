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
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.IModifier;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.EnumSort;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@AutoRegister
public class GuiMarketplace extends AbstractGuiModule {
    public GuiMarketplace(SweetPlayerMarket plugin) {
        super(plugin, plugin.resolve("./gui/marketplace.yml"));
    }

    @Override
    protected String warningPrefix() {
        return "[gui/marketplace.yml]";
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        if (!file.exists()) {
            plugin.saveResource("gui/marketplace.yml", file);
        }
        super.reloadConfig(cfg);
    }

    LoadedIcon iconItem, iconEmpty;

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
        Impl gui = (Impl) instance;
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

    public static GuiMarketplace inst() {
        return instanceOf(GuiMarketplace.class);
    }

    public static Impl create(Player player, Searching searching) {
        GuiMarketplace self = inst();
        return self.new Impl(player, searching);
    }

    public class Impl extends Gui implements InventoryHolder, Refreshable, Pageable {
        private final List<MarketItem> items = new ArrayList<>();
        private final int slotsSize;
        private Inventory inventory;
        private Searching searching;
        private int pages = 1;
        private boolean actionLock = false;
        private final ListPair<String, Object> commonReplacements = new ListPair<>();
        private int columnIndex = -1;
        protected Impl(Player player, Searching searching) {
            super(player, guiTitle, guiInventory);
            int itemsSize = 0;
            for (char c : super.inventory) {
                if (c == '物') itemsSize++;
            }
            this.slotsSize = itemsSize;
            this.searching = searching;
            List<String> columnList = plugin.displayNames().columnList();
            for (int i = 0; i < columnList.size(); i++) {
                if (columnList.get(i).equals("create_time")) {
                    columnIndex = i;
                    break;
                }
            }
            this.doSearch(false);
        }

        public GuiMarketplace getModel() {
            return GuiMarketplace.this;
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
        protected Inventory create(InventoryHolder holder, int size, String title) {
            return this.inventory = super.create(this, size, title.replace("%page%", String.valueOf(pages)));
        }

        @Override
        public void updateInventory(BiConsumer<Integer, ItemStack> setItem) {
            ListPair<String, Object> r = commonReplacements;
            r.clear();
            r.add("%search_type%", plugin.displayNames().getMarketTypeName(searching.type()));
            r.add("%search_currency%", plugin.displayNames().getCurrencyName(searching.currency()));
            r.add("%search_sort_column%", plugin.displayNames().getColumnName(searching.orderColumn()));
            r.add("%search_sort_type%", plugin.displayNames().getSortName(searching.orderType()));

            super.updateInventory(setItem);
            actionLock = false;
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
                if (item.amount() == 0) {
                    actionLock = false;
                    t(player, "&e来晚了，该商品已下架");
                    return;
                }
                if (click.isLeftClick()) {
                    MarketItem marketItem = refreshItem(item);
                    if (marketItem == null || marketItem.amount() == 0) {
                        items.set(i, item.toBuilder().amount(0).build());
                        actionLock = false;
                        t(player, "&e来晚了，该商品已下架");
                        return;
                    }
                    if (item.type().equals(EnumMarketType.SELL)) {
                        GuiConfirmSell.create(player, this, marketItem).open();
                        return;
                    }
                    if (item.type().equals(EnumMarketType.BUY)) {
                        // TODO: 转跳到下单结算菜单
                        return;
                    }
                    return;
                }
                return;
            }
            handleOtherClick(click, clickedId);
        }
        private MarketItem refreshItem(MarketItem item) {
            return plugin.getMarketplace().getItem(item.shopId());
        }
    }
}
