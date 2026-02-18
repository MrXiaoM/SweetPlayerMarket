package top.mrxiaom.sweet.playermarket.depend;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.manager.TemplateManager;
import net.Indyuce.mmoitems.manager.TypeManager;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.ItemProvider;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = {"MMOItems"})
public class DependencyMMOItems extends AbstractModule implements ItemProvider {
    public DependencyMMOItems(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.registerItemProvider(this);
        info("已挂钩 MMOItems");
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("mmoitems:")) {
            return build(inputText.substring(9), false);
        }
        if (inputText.startsWith("mmoitems-display:")) {
            return build(inputText.substring(17), true);
        }
        return null;
    }

    private ItemStack build(String input, boolean forDisplay) {
        String[] split = input.split(":", 2);
        if (split.length == 2) {
            TypeManager typeManager = MMOItems.plugin.getTypes();
            TemplateManager templateManager = MMOItems.plugin.getTemplates();
            Type type = typeManager.get(split[0]);
            if (type == null) return null;
            MMOItemTemplate template = templateManager.getTemplate(type, split[1]);
            if (template == null) return null;
            MMOItemBuilder itemBuilder = template.newBuilder();
            ItemStackBuilder itemStackBuilder = itemBuilder.build().newBuilder();
            return itemStackBuilder.build(forDisplay);
        }
        return null;
    }
}
