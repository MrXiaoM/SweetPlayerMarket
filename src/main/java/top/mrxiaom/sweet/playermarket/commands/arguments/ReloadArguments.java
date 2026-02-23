package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.func.I18nManager;

public class ReloadArguments extends AbstractArguments<CommandSender> {
    protected ReloadArguments(CommandArguments args) {
        super(args);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        if (match("database")) {
            plugin.options.database().reloadConfig();
            plugin.options.database().reconnect();
            return Messages.Command.reload__database.tm(sender);
        }
        if (match("assets")) {
            Messages.Command.reload__assets.tm(sender);
            plugin.getScheduler().runTaskAsync(() -> I18nManager.inst().reloadConfig());
            return true;
        }
        GuiManager manager = GuiManager.inst();
        for (Player p : Bukkit.getOnlinePlayers()) {
            IGuiHolder gui = manager.getOpeningGui(p);
            if (gui != null) {
                p.closeInventory();
            }
        }
        plugin.reloadConfig();
        return Messages.Command.reload__success.tm(sender);
    }

    public static ReloadArguments of(CommandArguments args) {
        return new ReloadArguments(args);
    }
}
