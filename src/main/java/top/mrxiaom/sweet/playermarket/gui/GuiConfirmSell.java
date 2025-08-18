package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
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
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.IModifier;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@AutoRegister
public class GuiConfirmSell extends AbstractGuiModule {
    public GuiConfirmSell(SweetPlayerMarket plugin) {
        super(plugin, plugin.resolve("./gui/confirm-sell.yml"));
    }

    @Override
    protected String warningPrefix() {
        return "[gui/confirm-sell.yml]";
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        if (!file.exists()) {
            plugin.saveResource("gui/confirm-sell.yml", file);
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
    protected ItemStack applyMainIcon(IGui instance, Player player, char id, int index, int appearTimes) {
        Impl gui = (Impl) instance;
        IModifier<String> displayModifier = oldName -> Pair.replace(oldName, gui.commonReplacements);
        IModifier<List<String>> loreModifier = oldLore -> Pair.replace(oldLore, gui.commonReplacements);
        if (id == '物') {
            MarketItem item = gui.marketItem;

            ItemStack baseItem = item.item();
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
            return iconItem.generateIcon(baseItem, player, displayModifier, loreMod);
        }
        if (id == '确') {
            return iconConfirm.generateIcon(player, displayModifier, loreModifier);
        }
        if (id == '返') {
            return iconBack.generateIcon(player, displayModifier, loreModifier);
        }
        LoadedIcon icon = otherIcons.get(id);
        if (icon != null) {
            return icon.generateIcon(player, displayModifier, loreModifier);
        }
        return null;
    }

    public static GuiConfirmSell inst() {
        return instanceOf(GuiConfirmSell.class);
    }

    public static Impl create(Player player, GuiMarketplace.Impl parent, MarketItem marketItem) {
        GuiConfirmSell self = inst();
        return self.new Impl(player, parent, marketItem);
    }

    public class Impl extends Gui implements InventoryHolder, ConfirmGUI, Refreshable {
        private final GuiMarketplace.Impl parent;
        private final MarketItem marketItem;
        private Inventory inventory;
        private final ListPair<String, Object> commonReplacements = new ListPair<>(), baseReplacements = new ListPair<>();
        private int count = 1;
        private boolean actionLock = false;
        protected Impl(Player player, GuiMarketplace.Impl parent, MarketItem marketItem) {
            super(player, guiTitle, guiInventory);
            this.parent = parent;
            this.marketItem = marketItem;

            ListPair<String, Object> r = baseReplacements;
            ItemStack baseItem = marketItem.item();
            String itemName = plugin.getItemName(baseItem);

            r.add("%player%", marketItem.playerName());
            r.add("%display%", itemName);
            r.add("%player%", marketItem.playerName());
            r.add("%type%", parent.getModel().getMarketTypeName(marketItem.type()));
            r.add("%amount%", marketItem.amount());
            r.add("%price%", marketItem.price());
            r.add("%currency%", parent.getModel().getCurrencyName(marketItem.currencyName()));
            r.add("%create_time%", plugin.toString(marketItem.createTime()));
            r.add("%outdate_time%", plugin.toString(marketItem.outdateTime()));
        }

        public int count() {
            return count;
        }

        public void count(int count) {
            this.count = count;
        }

        @Override
        public void countAdd(int count) {
            int target = count() + count;
            if (target > marketItem.amount()) {
                countAddMax();
                return;
            }
            count(target);
            refreshGui();
        }

        @Override
        public void countAddMax() {
            if (count() == marketItem.amount()) return;
            count(marketItem.amount());
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
            if (count > marketItem.amount()) {
                countAddMax();
                return;
            }
            count(count);
            refreshGui();
        }

        @Override
        public void refreshGui() {
            updateInventory(inventory);
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        @Override
        protected Inventory create(InventoryHolder holder, int size, String title) {
            return inventory = super.create(this, size, title);
        }

        @Override
        public void updateInventory(BiConsumer<Integer, ItemStack> setItem) {
            ListPair<String, Object> r = commonReplacements;
            r.clear();
            r.addAll(baseReplacements);
            r.add("%count%", count);
            r.add("%total_count%", count * marketItem.item().getAmount());
            r.add("%total_money%", String.format("%.2f", count * marketItem.price()));

            super.updateInventory(setItem);
            actionLock = false;
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
            if (clickedId == '物') return;
            if (clickedId == '确') {
                IEconomy currency = marketItem.currency();
                String currencyName = parent.getModel().getCurrencyName(marketItem.currencyName());
                if (currency == null) {
                    t(player, "&e在该子服不支持使用" + currencyName + "货币");
                    return;
                }
                OfflinePlayer owner;
                double totalMoney;
                actionLock = true;
                try (Connection conn = plugin.getConnection()){
                    MarketplaceDatabase db = plugin.getMarketplace();
                    MarketItem marketItem = db.getItem(conn, this.marketItem.shopId());
                    if (marketItem == null || marketItem.amount() == 0) {
                        t(player, "&e来晚了，该商品已下架");
                        parent.open();
                        return;
                    }
                    owner = plugin.getPlayer(marketItem.playerId());
                    if (owner == null) {
                        t(player, "&e店主的玩家数据在这个子服不存在，无法购买他的商品");
                        return;
                    }
                    int finalAmount = marketItem.amount() - count;
                    if (finalAmount < 0) {
                        t(player, "&e商品库存不足，减少一点购买数量吧~");
                        return;
                    }
                    totalMoney = count * marketItem.price();
                    if (!currency.has(player, totalMoney)) {
                        t(player, "&e你没有足够的" + currencyName);
                        return;
                    }
                    // 提交更改到数据库
                    if (!db.modifyItem(conn, marketItem.toBuilder()
                            .amount(finalAmount)
                            .build())) {
                        t(player, "&e数据库更改提交失败，可能该商品已下架");
                        return;
                    }
                } catch (SQLException e) {
                    warn(e);
                    player.closeInventory();
                    t(player, "&e出现数据库错误，已打印日志到控制台，请联系服务器管理员");
                    return;
                }
                // 拿走玩家的指定数量货币
                currency.takeMoney(player, totalMoney);
                // 给予卖家货币
                currency.giveMoney(owner, totalMoney);
                // 给予玩家物品
                for (int i = 0; i < count; i++) {
                    ItemStack item = marketItem.item();
                    ItemStackUtil.giveItemToPlayer(player, item);
                }
                // TODO: 提示玩家购买成功
                parent.open();
                return;
            }
            if (clickedId == '返') {
                actionLock = true;
                parent.open();
                return;
            }
            handleOtherClick(click, clickedId);
        }
    }
}
