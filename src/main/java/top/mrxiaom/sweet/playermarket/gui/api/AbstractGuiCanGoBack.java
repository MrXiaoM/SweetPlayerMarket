package top.mrxiaom.sweet.playermarket.gui.api;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;

import java.io.File;
import java.util.function.BiConsumer;

public abstract class AbstractGuiCanGoBack extends AbstractGuiModule {
    protected final String filePath;

    public AbstractGuiCanGoBack(SweetPlayerMarket plugin, String file) {
        super(plugin, plugin.resolve("./gui/" + file));
        this.filePath = file;
    }

    @Override
    public String warningPrefix() {
        return "[" + filePath + "]";
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
    }

    public abstract class CanGoBackGui<ParentGui extends IGuiHolder> extends Gui implements IGuiCanGoBack {
        public final SweetPlayerMarket plugin = AbstractGuiCanGoBack.this.plugin;
        protected final ParentGui parent;
        protected boolean actionLock = false;
        protected CanGoBackGui(Player player, ParentGui parent) {
            super(player, guiTitle, guiInventory);
            this.parent = parent;
        }

        @Override
        public void updateInventory(BiConsumer<Integer, ItemStack> setItem) {
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
            plugin.getScheduler().runTask(() -> handleOtherClick(click, clickedId));
        }

        @Override
        public void goBack() {
            if (parent != null) {
                actionLock = true;
                parent.open();
            }
        }
    }
}
