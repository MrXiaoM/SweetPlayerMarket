package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.arguments.Arguments;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.gui.GuiMarketplace;

public class OpenArguments extends AbstractArguments<CommandSender> {
    private static final Arguments.Builder builder = Arguments.builder()
            .addStringOptions("type", "-t", "--type")
            .addStringOptions("currency", "-c", "--currency");
    private final String type;
    private final String currency;
    protected OpenArguments(Arguments arguments) {
        super(arguments);
        this.type = arguments.getOptionString("type", null);
        this.currency = arguments.getOptionString("currency", null);
    }

    public String type() {
        return type;
    }

    public String currency() {
        return currency;
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        Player player = getPlayerOrSelf(sender, "sweet.playermarket.open.other");
        if (player == null) {
            return true;
        }
        IEconomy currency = plugin.parseEconomy(currency());
        plugin.getScheduler().runTaskAsync(() -> GuiMarketplace.create(player, Searching.of(false)
                .type(Util.valueOr(EnumMarketType.class, type(), null))
                .currency(currency == null ? null : currency.id())).open());
        return true;
    }

    public static OpenArguments of(String[] args) {
        return builder.build(OpenArguments::new, args);
    }
}
