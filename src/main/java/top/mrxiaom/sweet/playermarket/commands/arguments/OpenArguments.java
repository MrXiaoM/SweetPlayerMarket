package top.mrxiaom.sweet.playermarket.commands.arguments;

import top.mrxiaom.pluginbase.utils.arguments.Arguments;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;

public class OpenArguments extends CommandArguments {
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

    public static OpenArguments of(String[] args) {
        return builder.build(OpenArguments::new, args);
    }
}
