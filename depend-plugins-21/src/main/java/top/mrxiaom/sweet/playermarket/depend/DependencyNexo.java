package top.mrxiaom.sweet.playermarket.depend;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.ItemProvider;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = {"Nexo"})
public class DependencyNexo extends AbstractModule implements ItemProvider {
    public DependencyNexo(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.registerItemProvider(this);
        info("已挂钩 Nexo");
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("nexo:")) {
            String itemId = inputText.substring(5);
            ItemBuilder itemBuilder = NexoItems.itemFromId(itemId);
            if (itemBuilder != null) {
                return itemBuilder.build();
            }
            return null;
        }
        return null;
    }
}
