package top.mrxiaom.sweet.playermarket.data;

import org.jetbrains.annotations.Nullable;

/**
 * 储存 notice_flag 不同数值的定义
 */
public enum NoticeFlag {
    /**
     * 没有消息
     */
    NOTHING(0),
    /**
     * 有物品可以领取
     */
    CAN_CLAIM_ITEMS(1),
    /**
     * 商品被管理员下架
     */
    TAKE_DOWN_BY_ADMIN(2),

    ;
    private final int value;
    NoticeFlag(int value) {
        this.value = value;
    }

    public int getIntValue() {
        return value;
    }

    @Nullable
    public static NoticeFlag byInt(int value) {
        for (NoticeFlag flag : values()) {
            if (flag.value == value) {
                return flag;
            }
        }
        return null;
    }
}
