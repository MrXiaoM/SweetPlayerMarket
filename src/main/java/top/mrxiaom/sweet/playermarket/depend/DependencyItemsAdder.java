package top.mrxiaom.sweet.playermarket.depend;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.depend.IA;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.ItemProvider;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = {"ItemsAdder"})
public class DependencyItemsAdder extends AbstractModule implements ItemProvider {
    public DependencyItemsAdder(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.registerItemProvider(this);
        info("已挂钩 ItemsAdder");
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("itemsadder:")) {
            return IA.get(inputText.substring(11)).orElse(null);
        }
        return null;
    }
}
