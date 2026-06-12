package com.ccyscnyz.rituals.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * <h2>SidedItemHandler — 面向方向的受限物品处理器</h2>
 *
 * <p>一个 {@link IItemHandler} 的包装器，用于根据方块的某一面
 * 来限制物品的插入和提取行为。它不改变原始物品栏的内部实现，而是通过<b>白名单</b>
 * 机制只开放指定的槽位，并可选择性地覆盖槽位容量限制。</p>
 *
 * <h3>典型使用场景</h3>
 * <p>在方块实体（{@code BlockEntity}）的能力注册中（{@code RegisterCapabilitiesEvent}），
 * 根据情况返回特定的处理器实例，从而让漏斗、管道等
 * 自动化设备可以在不同情况下访问不同的特定槽位。</p>
 *
 * <h3>参数说明</h3>
 * <ul>
 *   <li><b>{@code internal}</b> — 原始物品栏，所有操作最终委托给它。</li>
 *   <li><b>{@code insertSlots}</b> — <b>插入白名单</b>。只有当目标槽位在此数组中时，
 *       才允许调用 {@link #insertItem(int, ItemStack, boolean)}。</li>
 *   <li><b>{@code extractSlots}</b> — <b>提取白名单</b>。只有当目标槽位在此数组中时，
 *       才允许调用 {@link #extractItem(int, int, boolean)}。</li>
 *   <li><b>{@code overrideLimit}</b> — 可选的容量覆盖。如果为 {@code null}，
 *       则使用原始物品栏的 {@link IItemHandler#getSlotLimit(int)}；
 *       如果非空，则容量取 <i>min(原始容量, overrideLimit)</i>。</li>
 * </ul>
 *
 * <h3>基本用法</h3>
 * <pre>{@code
 * // 假设 machine.inventory 是一个包含 3 个槽位的 ItemStackHandler
 *
 * // 例1：只允许插入槽位0，不允许提取
 * new SidedItemHandler(machine.inventory,
 *     new int[]{0},   // 允许插入的槽位
 *     new int[]{},           // 允许提取的槽位（空数组 = 禁止提取）
 *     null);                 // 不覆盖容量限制
 *
 * // 例2：只允许插入槽位1，且限制容量为 1
 * new SidedItemHandler(machine.inventory,
 *     new int[]{1},          // 只允许插入槽位 3
 *     new int[]{},           // 禁止提取
 *     1);                    // 强制容量为 1（即火种槽最多一个物品）
 *
 * // 例3：只允许提取曹魏2
 * new SidedItemHandler(machine.inventory,
 *     new int[]{},           // 禁止插入
 *     new int[]{2},          // 只允许提取槽位 4
 *     null);
 *
 * // 例4：同时允许插入和提取
 * new SidedItemHandler(machine.inventory,
 *     new int[]{0, 1},       // 可插入槽位
 *     new int[]{0, 1},       // 可提取槽位
 *     null);
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>这个类<b>只是视图（View）</b>，不保存任何数据本身。所有数据依然存储在
 *       {@code internal} 中。</li>
 *   <li>{@link #isItemValid(int, ItemStack)} 直接委托给 {@code internal}，
 *       不加额外的槽位限制（槽位限制由插入/提取白名单控制）。</li>
 *   <li>插入时如果设置了 {@code overrideLimit}，插入数量会被自动裁剪为
 *       {@code min(stack.getCount(), overrideLimit)}。</li>
 *   <li>此类是<b>无状态</b>的，同一个 {@code internal} 可以创建多个
 *       不同配置的 {@code SidedItemHandler} 分别使用。</li>
 * </ul>
 *
 * @see IItemHandler
 * @see net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
 * @see net.neoforged.neoforge.capabilities.Capabilities.ItemHandler#BLOCK
 */
public class SidedItemHandler implements IItemHandler {
    private final IItemHandler internal;
    private final int[] insertSlots;
    private final int[] extractSlots;
    private final Integer overrideLimit;

    /**
     * 构造一个方向限定的物品处理器。
     *
     * @param internal      原始物品栏
     * @param insertSlots   允许插入的槽位白名单（空数组 = 禁止所有插入）
     * @param extractSlots  允许提取的槽位白名单（空数组 = 禁止所有提取）
     * @param overrideLimit 覆盖槽位容量，{@code null} 表示使用原始容量
     */
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
        int limit = overrideLimit != null
                ? Math.min(overrideLimit, internal.getSlotLimit(slot))
                : internal.getSlotLimit(slot);
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


    //小工具，检查数组是否有指定值
    private static boolean contains(int[] array, int value) {
        for (int i : array) if (i == value) return true;
        return false;
    }
}