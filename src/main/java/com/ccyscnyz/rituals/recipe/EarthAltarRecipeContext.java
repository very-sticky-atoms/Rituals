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
    public EarthAltarRecipeContext with(ItemStack center){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext with(List<List<ItemStack>> directionItems){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext with(Level level){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext with(BlockPos position){
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
        public Container with(ItemStack center){
            this.value = value.with(center);
            return this;
        }
        public Container with(List<List<ItemStack>> directionItems){
            this.value = value.with(directionItems);
            return this;
        }
        public Container with(Level level){
            this.value = value.with(level);
            return this;
        }
        public Container with(BlockPos position){
            this.value = value.with(position);
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