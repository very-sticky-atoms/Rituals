package com.ccyscnyz.rituals.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class SidedItemHandler implements IItemHandler {
    private final IItemHandler internal;
    private final int[] insertSlots;
    private final int[] extractSlots;
    private final Integer overrideLimit; // null 表示不限

    public SidedItemHandler(IItemHandler internal, int[] insertSlots, int[] extractSlots, Integer overrideLimit) {
        this.internal = internal;
        this.insertSlots = insertSlots;
        this.extractSlots = extractSlots;
        this.overrideLimit = overrideLimit;
    }

    @Override
    public int getSlots() {
        return internal.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return internal.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!contains(insertSlots, slot)) return stack;
        int limit = overrideLimit != null ? Math.min(overrideLimit, stack.getMaxStackSize()) : internal.getSlotLimit(slot);
        ItemStack toInsert = stack.copy();
        toInsert.setCount(Math.min(toInsert.getCount(), limit));
        return internal.insertItem(slot, toInsert, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!contains(extractSlots, slot)) return ItemStack.EMPTY;
        return internal.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        int limit = internal.getSlotLimit(slot);
        if (overrideLimit != null && contains(insertSlots, slot)) {
            limit = Math.min(limit, overrideLimit);
        }
        return limit;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return internal.isItemValid(slot, stack);
    }

    private static boolean contains(int[] array, int value) {
        for (int i : array) if (i == value) return true;
        return false;
    }
}