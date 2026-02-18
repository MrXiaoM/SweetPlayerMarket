package top.mrxiaom.sweet.playermarket.depend;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.neige.neigeitems.manager.ItemManager;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.ItemProvider;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = {"NeigeItems"})
public class DependencyNeigeItems extends AbstractModule implements ItemProvider {
    public DependencyNeigeItems(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.registerItemProvider(this);
        info("已挂钩 NeigeItems");
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("neigeitems:")) {
            String argument = inputText.substring(11);
            if (argument.contains(";")) {
                String[] split = argument.split(";", 2);
                return ItemManager.INSTANCE.getItemStack(split[0], split[1]);
            } else {
                return ItemManager.INSTANCE.getItemStack(argument);
            }
        }
        return null;
    }
}
