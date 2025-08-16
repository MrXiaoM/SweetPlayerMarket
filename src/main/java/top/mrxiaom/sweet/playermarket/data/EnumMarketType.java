package top.mrxiaom.sweet.playermarket.data;

import org.jetbrains.annotations.Nullable;

/**
 * 全球市场商品类型
 */
public enum EnumMarketType {
    /**
     * 收购商品
     */
    BUY(0),
    /**
     * 出售商品
     */
    SELL(1)

    ;
    private final int value;
    EnumMarketType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Nullable
    public static EnumMarketType valueOf(int value) {
        for (EnumMarketType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}
