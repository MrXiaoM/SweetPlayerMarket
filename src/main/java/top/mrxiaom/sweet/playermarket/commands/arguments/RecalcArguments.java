package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;

public class RecalcArguments extends AbstractArguments<CommandSender> {
    protected RecalcArguments(CommandArguments args) {
        super(args);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        Messages.Command.recalc__start.tm(sender);
        plugin.getScheduler().runTaskAsync(() -> {
            MarketplaceDatabase db = plugin.getMarketplace();
            int count = db.recalculateItemsTag();
            if (count >= 0) {
                Messages.Command.recalc__success.tm(sender, Pair.of("%count%", count));
            } else {
                Messages.Command.recalc__failed.tm(sender);
            }
        });
        return true;
    }

    public static RecalcArguments of(CommandArguments args) {
        return new RecalcArguments(args);
    }
}
