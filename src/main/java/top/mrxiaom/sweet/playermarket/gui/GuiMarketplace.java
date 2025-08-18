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
import top.mrxiaom.pluginbase.economy.VaultEconomy;
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
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.economy.MPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.PlayerPointsEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@AutoRegister
public class GuiMarketplace extends AbstractGuiModule {
    private final Map<EnumMarketType, String> marketTypeNames = new HashMap<>();
    private final Map<EnumSort, String> sortNames = new HashMap<>();
    private final Map<String, String> currencyMPoints = new HashMap<>();
    private final Map<String, String> columnNames = new HashMap<>();
    private final List<String> columnList = new ArrayList<>();
    private String currencyVault, currencyPlayerPoints, currencyAll, marketTypeAll;

    public GuiMarketplace(SweetPlayerMarket plugin) {
        super(plugin, plugin.resolve("./gui/marketplace.yml"));
    }

    @Override
    protected String warningPrefix() {
        return "[gui/marketplace.yml]";
    }

    @NotNull
    public String getMarketTypeName(@Nullable EnumMarketType type) {
        if (type == null) {
            return marketTypeAll;
        }
        String s = marketTypeNames.get(type);
        return s != null ? s : type.name();
    }

    @NotNull
    public String getCurrencyName(@Nullable String currency) {
        if (currency == null) {
            return currencyAll;
        }
        if (currency.equals("Vault")) {
            return currencyVault;
        }
        if (currency.equals("PlayerPoints")) {
            return currencyPlayerPoints;
        }
        if (currency.startsWith("MPoints:") && currency.length() > 8) {
            String sign = currency.substring(8);
            return currencyMPoints.getOrDefault(sign, sign);
        }
        return currency;
    }

    @NotNull
    public String getCurrencyName(@Nullable IEconomy currency) {
        if (currency == null) {
            return currencyAll;
        }
        if (currency instanceof VaultEconomy) {
            return currencyVault;
        }
        if (currency instanceof PlayerPointsEconomy) {
            return currencyPlayerPoints;
        }
        if (currency instanceof MPointsEconomy) {
            String sign = ((MPointsEconomy) currency).sign();
            return currencyMPoints.getOrDefault(sign, sign);
        }
        return currency.getName();
    }

    @NotNull
    public String getColumnName(@NotNull String column) {
        return columnNames.getOrDefault(column, column);
    }

    @NotNull
    public String getSortName(@NotNull EnumSort sort) {
        String s = sortNames.get(sort);
        return s != null ? s : sort.name();
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        ConfigurationSection section;

        marketTypeNames.clear();
        marketTypeAll = "";
        section = cfg.getConfigurationSection("display-names.market-types");
        if (section != null) for (String key : section.getKeys(false)) {
            if (key.equals("all")) {
                marketTypeAll = section.getString(key);
                continue;
            }
            EnumMarketType type = Util.valueOr(EnumMarketType.class, key, null);
            if (type != null) {
                marketTypeNames.put(type, section.getString(key));
            }
        }
        currencyAll = cfg.getString("display-names.currency-types.all");
        currencyVault = cfg.getString("display-names.currency-types.vault");
        currencyPlayerPoints = cfg.getString("display-names.currency-types.points");
        currencyMPoints.clear();
        section = cfg.getConfigurationSection("display-names.currency-types.m-points");
        if (section != null) for (String key : section.getKeys(false)) {
            currencyMPoints.put(key, section.getString(key));
        }

        columnNames.clear();
        section = cfg.getConfigurationSection("display-names.columns");
        if (section != null) for (String key : section.getKeys(false)) {
            columnNames.put(key, section.getString(key));
        }
        columnList.clear();
        columnList.addAll(columnNames.keySet());

        sortNames.clear();
        section = cfg.getConfigurationSection("display-names.sort");
        if (section != null) for (String key : section.getKeys(false)) {
            EnumSort sort = Util.valueOr(EnumSort.class, key, null);
            if (sort != null) {
                sortNames.put(sort, section.getString(key));
            }
        }

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
                String itemName = plugin.getItemName(baseItem);
                List<String> itemLore = AdventureItemStack.getItemLoreAsMiniMessage(baseItem);

                ListPair<String, Object> r = new ListPair<>();
                r.addAll(gui.commonReplacements);
                r.add("%display%", itemName);
                r.add("%player%", item.playerName());
                r.add("%type%", getMarketTypeName(item.type()));
                r.add("%amount%", item.amount());
                r.add("%price%", item.price());
                r.add("%currency%", getCurrencyName(item.currency()));
                r.add("%create_time%", plugin.toString(item.createTime()));
                r.add("%create_time%", plugin.toString(item.outdateTime()));

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
                return iconItem.generateIcon(baseItem, player, displayModifier, loreModifier);
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
            r.add("%search_type%", getMarketTypeName(searching.type()));
            r.add("%search_currency%", getCurrencyName(searching.currency()));
            r.add("%search_sort_column%", getColumnName(searching.orderColumn()));
            r.add("%search_sort_type%", getSortName(searching.orderType()));

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
