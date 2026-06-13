package com.ccyscnyz.rituals.script;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ScriptApi {
    private MutableItemStack outputOverride = null;
    private final List<InputModification> inputModifications = new ArrayList<>();

    /** 设置输出（仅物品ID，数量1） */
    public void setOutput(String itemId) {
        setOutput(new MutableItemStack(itemId, 1));
    }

    /** 设置输出（使用可变物品对象） */
    public void setOutput(MutableItemStack stack) {
        this.outputOverride = stack;
    }

    /** 设置某个方向的返还物品（仅物品ID） */
    public void setInput(int direction, int index, String itemId) {
        setInput(direction, index, new MutableItemStack(itemId, 1));
    }

    /** 设置某个方向的返还物品（可变物品对象） */
    public void setInput(int direction, int index, MutableItemStack stack) {
        inputModifications.add(new InputModification(direction, index, stack));
    }

    public MutableItemStack getOutputOverride() { return outputOverride; }
    public List<InputModification> getInputModifications() { return inputModifications; }

    public record InputModification(int direction, int index, MutableItemStack stack) {}
}