package top.mrxiaom.sweet.playermarket.commands.arguments;

import top.mrxiaom.pluginbase.utils.arguments.Arguments;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;

public class MeArguments extends CommandArguments {
    private static final Arguments.Builder builder = Arguments.builder()
            .addBooleanOption("notice", "-n", "--notice")
            .addBooleanOption("out-of-stock", "-o", "--only-out-of-stock");
    private final boolean notice, outOfStock;
    protected MeArguments(Arguments arguments) {
        super(arguments);
        notice = arguments.getOptionBoolean("notice");
        outOfStock = arguments.getOptionBoolean("out-of-stock");
    }

    public boolean notice() {
        return notice;
    }

    public boolean onlyOutOfStock() {
        return outOfStock;
    }

    public static MeArguments of(String[] args) {
        return builder.build(MeArguments::new, args);
    }
}
