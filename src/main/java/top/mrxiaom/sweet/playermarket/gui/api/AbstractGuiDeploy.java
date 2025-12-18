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
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.IModifier;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.limitation.BaseLimitation;
import top.mrxiaom.sweet.playermarket.data.limitation.CreateCost;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;
import top.mrxiaom.sweet.playermarket.func.LimitationManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class AbstractGuiDeploy extends AbstractGuiModule {
    private final String filePath;
    public AbstractGuiDeploy(SweetPlayerMarket plugin, String file) {
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
            plugin.saveResource("gui/" + filePath, file);
        }
        super.reloadConfig(cfg);
    }

    protected IEconomy getDefaultCurrency() {
        return plugin.getVault();
    }

    LoadedIcon iconEmptyItem, iconConfirm;
    List<String> limitMessagesHeader;
    String limitMessagesLine;
    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {
        if (id.equals("物")) {
            iconEmptyItem = icon;
            limitMessagesHeader = section.getStringList(id + ".limit-messages.header");
            limitMessagesLine = section.getString(id + ".limit-messages.line", "  %message%  ");
        }
        if (id.equals("确")) {
            iconConfirm = icon;
        }
    }

    @Override
    protected ItemStack applyMainIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes) {
        DeployGui gui = (DeployGui) instance;
        if (id == '物') {
            ItemStack sampleItem = gui.sampleItem;
            ListPair<String, Object> r = gui.commonReplacements;
            if (sampleItem == null) {
                IModifier<String> displayModifier = oldName -> Pair.replace(oldName, r);
                IModifier<List<String>> loreModifier = oldLore -> Pair.replace(oldLore, r);
                return iconEmptyItem.generateIcon(player, displayModifier, loreModifier);
            }
            return sampleItem; // TODO: 支持修改样例物品的 lore 等格式
        }
        if (id == '确') {
            ListPair<String, Object> r = gui.commonReplacements;
            IModifier<String> displayModifier = oldName -> Pair.replace(oldName, r);
            IModifier<List<String>> loreModifier = oldLore -> {
                List<String> lore = new ArrayList<>();
                for (String s : oldLore) {
                    if (s.equals("limit messages")) {
                        List<String> messages = gui.getLimitMessages();
                        if (!messages.isEmpty()) {
                            lore.addAll(limitMessagesHeader);
                            for (String message : messages) {
                                lore.add(limitMessagesLine.replace("%message%", message));
                            }
                        }
                        continue;
                    }
                    lore.add(s);
                }
                return Pair.replace(lore, r);
            };
            return iconConfirm.generateIcon(player, displayModifier, loreModifier);
        }
        return null;
    }

    @Override
    protected @Nullable ItemStack applyOtherIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes, LoadedIcon icon) {
        DeployGui gui = (DeployGui) instance;
        ListPair<String, Object> r = gui.commonReplacements;
        IModifier<String> displayModifier = oldName -> Pair.replace(oldName, r);
        IModifier<List<String>> loreModifier = oldLore -> Pair.replace(oldLore, r);
        return icon.generateIcon(player, displayModifier, loreModifier);
    }

    public abstract class DeployGui extends Gui implements IGuiRefreshable, IGuiDeploy {
        protected final ListPair<String, Object> commonReplacements = new ListPair<>();
        protected final EnumMarketType type;
        protected @Nullable ItemStack sampleItem = null;
        protected int amount = 1;
        protected double price = 0;
        protected CreateCost createCost = null;
        protected IEconomy currency = getDefaultCurrency();
        protected int currencyIndex = 0;
        protected boolean actionLock = false;
        protected List<String> limitMessages;

        protected DeployGui(Player player, EnumMarketType type) {
            super(player, guiTitle, guiInventory);
            this.type = type;
        }

        public List<String> getLimitMessages() {
            return limitMessages;
        }

        protected void setSampleItem(ItemStack item) {
            sampleItem = item == null ? null : item.clone();
        }

        @Override
        public void modifyAmount(NumberOperation operation, int value) {
            int newAmount = amount;
            switch (operation) {
                case SET:
                    newAmount = Math.max(1, value);
                    break;
                case ADD:
                    newAmount = Math.max(1, amount + value);
                    break;
                case MINUS:
                    newAmount = Math.max(1, amount - value);
                    break;
            }
            if (amount != newAmount) {
                amount = newAmount;
                updateReplacements();
                refreshGui();
            }
        }

        @Override
        public void modifyItemCount(NumberOperation operation, int value) {
            if (sampleItem == null) return;
            int oldCount = sampleItem.getAmount();
            int newCount = oldCount;
            int stackSize = sampleItem.getMaxStackSize();
            switch (operation) {
                case SET:
                    newCount = Util.between(value, 1, stackSize);
                    break;
                case ADD:
                    newCount = Util.between(oldCount + value, 1, stackSize);
                    break;
                case MINUS:
                    newCount = Util.between(oldCount - value, 1, stackSize);
                    break;
            }
            if (oldCount != newCount) {
                sampleItem.setAmount(newCount);
                updateReplacements();
                refreshGui();
            }
        }

        @Override
        public void modifyPrice(NumberOperation operation, double value) {
            double newPrice = price;
            switch (operation) {
                case SET:
                    newPrice = Math.max(1, value);
                    break;
                case ADD:
                    newPrice = Math.max(1, price + value);
                    break;
                case MINUS:
                    newPrice = Math.max(1, price - value);
                    break;
            }
            if (price != newPrice) {
                price = newPrice;
                updateReplacements();
                refreshGui();
            }
        }

        @Override
        public void setCurrency(IEconomy currency) {
            this.currency = currency;
            updateReplacements();
            refreshGui();
        }

        @Override
        public void switchCurrency(List<IEconomy> currencyList) {
            int size = currencyList.size();
            if (size == 0) return;
            if (size == 1) {
                setCurrency(currencyList.get(0));
                return;
            }
            int newIndex = currencyIndex + 1;
            if (newIndex >= size) {
                setCurrency(currencyList.get(currencyIndex = 0));
            } else {
                setCurrency(currencyList.get(currencyIndex = newIndex));
            }
        }

        @Override
        public void refreshGui() {
            updateInventory(getInventory());
            Util.submitInvUpdate(player);
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
            int itemCount = sampleItem == null ? 1 : sampleItem.getAmount();
            double totalMoney = amount * price;
            r.add("%currency%", plugin.displayNames().getCurrencyName(currency));
            r.add("%item_count%", itemCount);
            r.add("%total_count%", amount * itemCount);
            r.add("%count%", amount);
            r.add("%price%", String.format("%.2f", price).replace(".00", ""));
            r.add("%total_money%", String.format("%.2f", totalMoney).replace(".00", ""));

            List<String> limitMessages = new ArrayList<>();
            if (sampleItem != null) {
                BaseLimitation limitation = LimitationManager.inst().getLimitByItem(sampleItem);
                if (!player.hasPermission("sweet.playermarket.create.bypass.type") && !limitation.canUseMarketType(type)) {
                    limitMessages.add(Messages.Command.create__limitation__type_not_allow.str());
                }
                if (!player.hasPermission("sweet.playermarket.create.bypass.currency") && !limitation.canUseCurrency(currency)) {
                    limitMessages.add(Messages.Command.create__limitation__currency_not_allow.str(
                            Pair.of("%currency%", plugin.displayNames().getCurrencyName(currency))));
                }
                createCost = !limitMessages.isEmpty() ? null : limitation.getCreateCost(type);
            } else {
                createCost = null;
            }
            this.limitMessages = limitMessages;

            if (createCost == null) {
                r.add("%create_cost_money%", "");
                r.add("%create_cost_currency%", "");
            } else {
                IEconomy createCostCurrency;
                Double createCostMoney;
                if (!player.hasPermission("sweet.playermarket.create.bypass.cost") && createCost != null) {
                    createCostCurrency = createCost.currency(currency);
                    createCostMoney = createCost.money(totalMoney);
                    if (createCostMoney > 0 && !createCostCurrency.has(player, createCostMoney)) {
                        createCostCurrency = null;
                        createCostMoney = null;
                    }
                } else {
                    createCostCurrency = null;
                    createCostMoney = 0.0;
                }
                if (createCostMoney != null) {
                    r.add("%create_cost_money%", String.format("%.2f", createCostMoney).replace(".00", ""));
                    r.add("%create_cost_currency%", createCostCurrency == null ? "" : plugin.displayNames().getCurrencyName(createCostCurrency));
                } else {
                    r.add("%create_cost_money%", "");
                    r.add("%create_cost_currency%", "");
                }
            }
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
            if (event.getClickedInventory() instanceof PlayerInventory) {
                setSampleItem(currentItem);
                updateReplacements();
                refreshGui();
                return;
            }
            Character clickedId = getClickedId(slot);
            if (clickedId == null) return;
            checkNeedToLockAction(clickedId);
            plugin.getScheduler().runTask(() -> {
                if (clickedId == '确') {
                    onClickConfirm(action, click, slotType, slot, view, event);
                    return;
                }
                if (onClickMainIcons(action, click, slotType, slot, clickedId, view, event)) {
                    return;
                }
                handleOtherClick(click, clickedId);
            });
        }

        protected abstract void checkNeedToLockAction(char id);

        protected abstract void onClickConfirm(
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
