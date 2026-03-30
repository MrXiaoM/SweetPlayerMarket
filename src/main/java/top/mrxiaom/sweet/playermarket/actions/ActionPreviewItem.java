package top.mrxiaom.sweet.playermarket.actions;

import com.google.common.collect.Lists;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.gui.GuiPreview;

import java.util.ArrayList;
import java.util.List;

public class ActionPreviewItem extends AbstractActionWithMarketItem {
    public static final ActionPreviewItem INSTANCE = new ActionPreviewItem();
    public static final IActionProvider PROVIDER = (input) -> {
        if (input instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) input;
            if ("preview-item".equals(section.getString("type"))) {
                return INSTANCE;
            }
        } else {
            String s = String.valueOf(input);
            if (s.equals("[preview-item]") || s.equals("preview-item")) return INSTANCE;
        }
        return null;
    };
    @Override
    public void run(@NotNull Player player, @NotNull MarketItem item, @NotNull List<Pair<String, Object>> replacements) {
        IGuiHolder parent = GuiManager.inst().getOpeningGui(player);
        ItemMeta meta = item.item().getItemMeta();
        if (meta instanceof BundleMeta) {
            List<ItemStack> items = new ArrayList<>(((BundleMeta) meta).getItems());
            GuiPreview.create(player, parent, items);
            return;
        }
        if (meta instanceof BlockStateMeta) {
            BlockState state = ((BlockStateMeta) meta).getBlockState();
            if (state instanceof ShulkerBox) {
                List<ItemStack> items = Lists.newArrayList(((ShulkerBox) state).getInventory().getStorageContents());
                GuiPreview.create(player, parent, items).open();
            }
        }
    }
}
