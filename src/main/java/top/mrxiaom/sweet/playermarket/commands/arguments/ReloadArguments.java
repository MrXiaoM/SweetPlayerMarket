package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;

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
        plugin.reloadConfig();
        return Messages.Command.reload__success.tm(sender);
    }

    public static ReloadArguments of(CommandArguments args) {
        return new ReloadArguments(args);
    }
}
