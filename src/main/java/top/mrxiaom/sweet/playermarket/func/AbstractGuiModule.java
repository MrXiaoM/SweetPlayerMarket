package top.mrxiaom.sweet.playermarket.func;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;

import java.io.File;

public abstract class AbstractGuiModule extends top.mrxiaom.pluginbase.func.AbstractGuiModule<SweetPlayerMarket> {
    public AbstractGuiModule(SweetPlayerMarket plugin, File file) {
        super(plugin, file);
    }

    public AbstractGuiModule(SweetPlayerMarket plugin, File file, @Nullable String mainIconsKey, @Nullable String otherIconsKey) {
        super(plugin, file, mainIconsKey, otherIconsKey);
    }
}
