package top.mrxiaom.sweet.playermarket.utils;

import java.util.*;

public class ListX<E> extends ArrayList<E> {
    private int totalCount;
    public ListX(Collection<E> collection) {
        super(collection);
    }
    public ListX() {
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getMaxPage(int perPage) {
        if (totalCount == 0) return 0;
        return (int) Math.ceil((double) totalCount / perPage);
    }

    public void copyTo(ListX<E> list) {
        list.addAll(this);
        list.setTotalCount(getTotalCount());
    }
}
