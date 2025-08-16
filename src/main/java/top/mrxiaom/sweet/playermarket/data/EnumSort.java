package top.mrxiaom.sweet.playermarket.data;

/**
 * 查询物品时的排序方式
 */
public enum EnumSort {
    /**
     * 升序（从小到大排序）
     */
    ASC("ASC"),
    /**
     * 降序（从大到小排序）
     */
    DESC("DESC")

    ;
    private final String value;
    EnumSort(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }
}
