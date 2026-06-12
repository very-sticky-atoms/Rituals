package com.ccyscnyz.rituals.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InvulnerableItemEntity extends ItemEntity {

    private int invulnerableTicks = 0;

    public InvulnerableItemEntity(Level level, double x, double y, double z, ItemStack stack, int invulnerableDuration) {
        super(level, x, y, z, stack);
        this.invulnerableTicks = invulnerableDuration;
        this.setInvulnerable(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && invulnerableTicks > 0) {
            invulnerableTicks--;
            if (invulnerableTicks <= 0) {
                this.setInvulnerable(false);
            }
        }
    }
}