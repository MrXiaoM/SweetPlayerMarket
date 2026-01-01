package top.mrxiaom.sweet.playermarket.depend;

import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.MythicItem;
import top.mrxiaom.sweet.playermarket.depend.mythic.IMythic;
import top.mrxiaom.sweet.playermarket.depend.mythic.Mythic4;
import top.mrxiaom.sweet.playermarket.depend.mythic.Mythic5;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = "MythicMobs")
public class DependencyMythicMobs extends AbstractModule {
    private IMythic instance;
    MythicItem mythicItem;
    public DependencyMythicMobs(SweetPlayerMarket plugin) {
        super(plugin);
        if (Util.isPresent("io.lumine.mythic.bukkit.MythicBukkit")) {
            instance = new Mythic5();
        }
        if (Util.isPresent("io.lumine.xikage.mythicmobs.MythicMobs")) {
            instance = new Mythic4();
        }
        plugin.registerItemProvider(mythicItem = new MythicItem(instance));
    }

    @Override
    public void onDisable() {
        plugin.unregisterItemProvider(mythicItem);
    }
}
