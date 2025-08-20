package top.mrxiaom.sweet.playermarket.gui.api;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.IModifier;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractGuiConfirm extends AbstractGuiModule {
    private final String filePath;
    public AbstractGuiConfirm(SweetPlayerMarket plugin, String file) {
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

    LoadedIcon iconItem, iconConfirm, iconBack;
    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {
        if (id.equals("物")) {
            iconItem = icon;
        }
        if (id.equals("确")) {
            iconConfirm = icon;
        }
        if (id.equals("返")) {
            iconBack = icon;
        }
    }

    @Override
    protected ItemStack applyMainIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes) {
        ConfirmGui gui = (ConfirmGui) instance;
        IModifier<String> displayModifier = oldName -> Pair.replace(oldName, gui.commonReplacements);
        IModifier<List<String>> loreModifier = oldLore -> Pair.replace(oldLore, gui.commonReplacements);
        if (id == '物') {
            MarketItem item = gui.marketItem;

            ItemStack baseItem = item.item();
            int displayAmount = baseItem.getAmount();
            List<String> itemLore = AdventureItemStack.getItemLoreAsMiniMessage(baseItem);

            IModifier<List<String>> loreMod = oldLore -> {
                List<String> lore = new ArrayList<>();
                for (String s : oldLore) {
                    if (s.equals("item lore")) {
                        lore.addAll(itemLore);
                        continue;
                    }
                    lore.add(Pair.replace(s, gui.commonReplacements));
                }
                return lore;
            };
            ItemStack icon = iconItem.generateIcon(baseItem, player, displayModifier, loreMod);
            icon.setAmount(displayAmount);
            return icon;
        }
        if (id == '确') {
            return iconConfirm.generateIcon(player, displayModifier, loreModifier);
        }
        if (id == '返') {
            return iconBack.generateIcon(player, displayModifier, loreModifier);
        }
        return null;
    }

    @Override
    protected @Nullable ItemStack applyOtherIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes, LoadedIcon icon) {
        AbstractGuiSearch.SearchGui gui = (AbstractGuiSearch.SearchGui) instance;
        IModifier<String> displayModifier = oldName -> Pair.replace(oldName, gui.commonReplacements);
        IModifier<List<String>> loreModifier = oldLore -> Pair.replace(oldLore, gui.commonReplacements);
        return icon.generateIcon(player, displayModifier, loreModifier);
    }

    public abstract class ConfirmGui extends Gui implements IGuiConfirm, IGuiRefreshable {
        protected final MarketItem marketItem;
        protected final ListPair<String, Object> commonReplacements = new ListPair<>(), baseReplacements = new ListPair<>();
        protected int count = 1;
        protected boolean actionLock = false;

        protected ConfirmGui(Player player, MarketItem marketItem) {
            super(player, guiTitle, guiInventory);
            this.marketItem = marketItem;

            updateBaseReplacements();
        }

        protected void updateBaseReplacements() {
            ListPair<String, Object> r = baseReplacements;
            ItemStack baseItem = marketItem.item();
            String itemName = plugin.getItemName(baseItem);

            r.add("%player%", marketItem.playerName());
            r.add("%display%", itemName);
            r.add("%player%", marketItem.playerName());
            r.add("%type%", plugin.displayNames().getMarketTypeName(marketItem.type()));
            r.add("%amount%", marketItem.amount());
            r.add("%price%", marketItem.price());
            r.add("%currency%", plugin.displayNames().getCurrencyName(marketItem.currencyName()));
            r.add("%create_time%", plugin.toString(marketItem.createTime()));
            r.add("%outdate_time%", plugin.toString(marketItem.outdateTime()));
        }

        public int count() {
            return count;
        }

        public void count(int count) {
            this.count = count;
        }

        public abstract int getMaxCount();

        @Override
        public void countAdd(int count) {
            int target = count() + count;
            if (target > getMaxCount()) {
                countAddMax();
                return;
            }
            count(target);
            refreshGui();
        }

        @Override
        public void countAddMax() {
            if (count() == getMaxCount()) return;
            count(getMaxCount());
            refreshGui();
        }

        @Override
        public void countMinus(int count) {
            int target = count() - count;
            if (target < 1) {
                countMinusMax();
                return;
            }
            count(target);
            refreshGui();
        }

        @Override
        public void countMinusMax() {
            if (count() == 1) return;
            count(1);
            refreshGui();
        }

        @Override
        public void countSet(int count) {
            if (count() == count) return;
            if (count < 1) {
                countMinusMax();
                return;
            }
            if (count > getMaxCount()) {
                countAddMax();
                return;
            }
            count(count);
            refreshGui();
        }

        @Override
        public void refreshGui() {
            updateInventory(getInventory());
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
            r.addAll(baseReplacements);
            r.add("%count%", count);
            r.add("%total_count%", count * marketItem.item().getAmount());
            r.add("%total_money%", String.format("%.2f", count * marketItem.price()));
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
                    onClickMarketItem(action, click, slotType, slot, view, event);
                    return;
                }
                if (clickedId == '确') {
                    onClickConfirm(action, click, slotType, slot, view, event);
                    return;
                }
                if (clickedId == '返') {
                    onClickBack(action, click, slotType, slot, view, event);
                    return;
                }
                if (onClickMainIcons(action, click, slotType, slot, clickedId, view, event)) {
                    return;
                }
                handleOtherClick(click, clickedId);
            });
        }

        protected void onClickMarketItem(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event) {}

        protected abstract void onClickConfirm(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event);

        protected abstract void onClickBack(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event);

        protected boolean onClickMainIcons(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                Character clickedId,
                InventoryView view, InventoryClickEvent event
        ) {
            return false;
        }
    }

}
