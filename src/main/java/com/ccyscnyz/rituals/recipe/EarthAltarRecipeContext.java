package com.ccyscnyz.rituals.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public record EarthAltarRecipeContext(ItemStack center, List<List<ItemStack>> directionItems, Level level, BlockPos position) implements RecipeInput{
    @Override
    public ItemStack getItem(int index) {
        return center;
    }

    @Override
    public int size() {
        return 1;
    }

    public EarthAltarRecipeContext copy(){
        return new EarthAltarRecipeContext(center.copy(),directionItems.stream().map(l->l.stream().map(ItemStack::copy).toList()).toList(),level,position);
    }
    public EarthAltarRecipeContext withCenter(ItemStack center){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext withDirectionItems(List<List<ItemStack>> directionItems){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext withLevel(Level level){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext withPosition(BlockPos position){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public Container wrap(){
        return new Container(this);
    }

    public static class Container {
        public EarthAltarRecipeContext value;
        public Container(EarthAltarRecipeContext context){
            this.value = context;
        }
        public Container withCenter(ItemStack center){
            this.value = value.withCenter(center);
            return this;
        }
        public Container withDirectionItems(List<List<ItemStack>> directionItems){
            this.value = value.withDirectionItems(directionItems);
            return this;
        }
        public Container withLevel(Level level){
            this.value = value.withLevel(level);
            return this;
        }
        public Container withPosition(BlockPos position){
            this.value = value.withPosition(position);
            return this;
        }
    }

    @FunctionalInterface
    public interface Callback {
        void call(EarthAltarRecipeContext context);
    }

    public static class CallbackContainer {
        public Callback value;
        public CallbackContainer(Callback callback){
            this.value =callback;
        }
    }

    public record StartScriptResult(int processingTime, Callback callback){}
    public record FinishScriptResult(EarthAltarRecipeContext context, Callback callback){}
}