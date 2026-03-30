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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiCanGoBack;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiPageable;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

@AutoRegister
public class GuiPreview extends AbstractGuiModule {
    protected static final String filePath = "preview.yml";
    protected LoadedIcon iconEmpty;
    public GuiPreview(SweetPlayerMarket plugin) {
        super(plugin, plugin.resolve("./gui/" + filePath));
    }

    @Override
    public String warningPrefix() {
        return "[gui/preview.yml]";
    }

    @Override
    public void reloadConfig(MemoryConfiguration cfg) {
        String resourceFile = "gui/" + filePath;
        File guiFolder = plugin.resolve(cfg.getString("gui-folder", "./gui"));
        this.file = new File(guiFolder, filePath);
        if (!file.exists()) {
            plugin.saveResource(resourceFile, file);
        }
        super.reloadConfig(cfg);
        iconEmpty = Utils.requireIconNotNull(this, resourceFile, iconEmpty, "main-icons.空");
    }

    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        iconEmpty = null;
    }

    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {
        if (id.equals("空")) {
            iconEmpty = icon;
        }
    }

    @Override
    protected @Nullable ItemStack applyMainIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes) {
        Impl gui = (Impl) instance;
        if (id == '物') {
            List<ItemStack> items = gui.items;
            int i = (gui.pages - 1) * gui.perPageSize;
            if (i < 0 || i >= items.size()) {
                return iconEmpty.generateIcon(player);
            } else {
                return items.get(i).clone();
            }
        }
        return null;
    }

    public static GuiPreview inst() {
        return instanceOf(GuiPreview.class);
    }

    public static GuiPreview.Impl create(Player player, IGuiHolder parent, List<ItemStack> items) {
        GuiPreview self = inst();
        return self.new Impl(player, parent, items);
    }

    public class Impl extends Gui implements IGuiCanGoBack, IGuiPageable {
        private final IGuiHolder parent;
        private final List<ItemStack> items;
        private final int perPageSize;
        private final int maxPages;
        private int pages = 1;
        private boolean actionLock = false;
        protected Impl(Player player, IGuiHolder parent, List<ItemStack> items) {
            super(player, guiTitle, guiInventory);
            this.parent = parent;
            this.items = items;
            int perPageSize = 0;
            for (char ch : guiInventory) {
                if (ch == '物') {
                    perPageSize++;
                }
            }
            this.perPageSize = perPageSize;
            this.maxPages = (int) Math.ceil((double)items.size() / perPageSize);
        }

        @Override
        public void updateInventory(BiConsumer<Integer, ItemStack> setItem) {
            super.updateInventory(setItem);
            actionLock = false;
        }

        @Override
        public void turnPageUp(int pages) {
            if (this.pages - pages < 1) return;
            actionLock = true;
            plugin.getScheduler().runTask(() -> {
                this.pages -= pages;
                this.open();
            });
        }

        @Override
        public void turnPageDown(int pages) {
            if (this.pages + pages > maxPages) return;
            actionLock = true;
            plugin.getScheduler().runTask(() -> {
                this.pages += pages;
                this.open();
            });
        }

        @Override
        public void onClick(InventoryAction action, ClickType click, InventoryType.SlotType slotType, int slot, ItemStack currentItem, ItemStack cursor, InventoryView view, InventoryClickEvent event) {
            event.setCancelled(true);
            if (actionLock) return;
            Character clickedId = getClickedId(slot);
            if (clickedId == null) return;
            plugin.getScheduler().runTask(() -> handleOtherClick(click, clickedId));
        }

        @Override
        public void goBack() {
            if (parent != null) {
                plugin.getScheduler().runTask(parent::open);
            } else {
                player.closeInventory();
            }
        }
    }
}
