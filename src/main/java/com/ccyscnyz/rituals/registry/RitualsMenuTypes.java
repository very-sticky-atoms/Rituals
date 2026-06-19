package com.ccyscnyz.rituals.registry;

import com.ccyscnyz.rituals.Rituals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Rituals.MODID);
}
