package top.mrxiaom.sweet.playermarket.func;
        
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetPlayerMarket> {
    public AbstractPluginHolder(SweetPlayerMarket plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetPlayerMarket plugin, boolean register) {
        super(plugin, register);
    }
}
