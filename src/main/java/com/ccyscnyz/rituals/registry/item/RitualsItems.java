package com.ccyscnyz.rituals.registry.item;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.annotation.TabItem;
import com.ccyscnyz.rituals.item.FlawlessItem;
import com.ccyscnyz.rituals.registry.RitualsDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;


public class RitualsItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Rituals.MODID);


    @TabItem("rituals_tab")
    public static final DeferredItem<Item> CRUDE_CARVING_KNIFE = ITEMS.register("crude_carving_knife",
            () -> new Item(new Item.Properties()
                    .durability(16)
                    .stacksTo(1)
                    .attributes(ItemAttributeModifiers.builder()
                            .add(
                                    Attributes.ATTACK_DAMAGE,
                                    new AttributeModifier(
                                            ResourceLocation.withDefaultNamespace("base_attack_damage"),
                                            4.0d,
                                            AttributeModifier.Operation.ADD_VALUE
                                    ),
                                    EquipmentSlotGroup.MAINHAND
                            )
                            .build())));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot",
            () -> new Item(new Item.Properties()));

    // ==================== 铜系列工具 (COPPER) ====================
    @TabItem("rituals_tab")
    public static final DeferredItem<Item> COPPER_SWORD = ITEMS.register("copper_sword",
            () -> new SwordItem(RitualsTiers.COPPER, new Item.Properties()
                    .attributes(SwordItem.createAttributes(RitualsTiers.COPPER, 3.0F, -2.3F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> COPPER_PICKAXE = ITEMS.register("copper_pickaxe",
            () -> new PickaxeItem(RitualsTiers.COPPER, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(RitualsTiers.COPPER, 1.0F, -2.7F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> COPPER_AXE = ITEMS.register("copper_axe",
            () -> new AxeItem(RitualsTiers.COPPER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(RitualsTiers.COPPER, 6.5F, -3.1F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> COPPER_SHOVEL = ITEMS.register("copper_shovel",
            () -> new ShovelItem(RitualsTiers.COPPER, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(RitualsTiers.COPPER, 1.5F, -2.7F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> COPPER_HOE = ITEMS.register("copper_hoe",
            () -> new HoeItem(RitualsTiers.COPPER, new Item.Properties()
                    .attributes(HoeItem.createAttributes(RitualsTiers.COPPER, -1.5F, -2.0F))
            ));


    // ==================== 钢系列工具 (STEEL) ====================
    @TabItem("rituals_tab")
    public static final DeferredItem<Item> STEEL_SWORD = ITEMS.register("steel_sword",
            () -> new SwordItem(RitualsTiers.STEEL, new Item.Properties()
                    .attributes(SwordItem.createAttributes(RitualsTiers.STEEL, 3.0F, -2.1F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> STEEL_PICKAXE = ITEMS.register("steel_pickaxe",
            () -> new PickaxeItem(RitualsTiers.STEEL, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(RitualsTiers.STEEL, 1.0F, -2.5F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> STEEL_AXE = ITEMS.register("steel_axe",
            () -> new AxeItem(RitualsTiers.STEEL, new Item.Properties()
                    .attributes(AxeItem.createAttributes(RitualsTiers.STEEL, 5.5F, -2.9F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> STEEL_SHOVEL = ITEMS.register("steel_shovel",
            () -> new ShovelItem(RitualsTiers.STEEL, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(RitualsTiers.STEEL, 1.5F, -2.5F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> STEEL_HOE = ITEMS.register("steel_hoe",
            () -> new HoeItem(RitualsTiers.STEEL, new Item.Properties()
                    .attributes(HoeItem.createAttributes(RitualsTiers.STEEL, -2.5F, -1.0F))
            ));

    // ==================== 黑曜石系列工具 (OBSIDIAN) ====================
    @TabItem("rituals_tab")
    public static final DeferredItem<Item> OBSIDIAN_SWORD = ITEMS.register("obsidian_sword",
            () -> new SwordItem(RitualsTiers.OBSIDIAN, new Item.Properties()
                    .attributes(SwordItem.createAttributes(RitualsTiers.OBSIDIAN, 4.0F, -2.2F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> OBSIDIAN_PICKAXE = ITEMS.register("obsidian_pickaxe",
            () -> new PickaxeItem(RitualsTiers.OBSIDIAN, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(RitualsTiers.OBSIDIAN, 1.0F, -2.3F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> OBSIDIAN_AXE = ITEMS.register("obsidian_axe",
            () -> new AxeItem(RitualsTiers.OBSIDIAN, new Item.Properties()
                    .attributes(AxeItem.createAttributes(RitualsTiers.OBSIDIAN, 5.0F, -2.7F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> OBSIDIAN_SHOVEL = ITEMS.register("obsidian_shovel",
            () -> new ShovelItem(RitualsTiers.OBSIDIAN, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(RitualsTiers.OBSIDIAN, 1.5F, -2.3F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<Item> OBSIDIAN_HOE = ITEMS.register("obsidian_hoe",
            () -> new HoeItem(RitualsTiers.OBSIDIAN, new Item.Properties()
                    .attributes(HoeItem.createAttributes(RitualsTiers.OBSIDIAN, -4.0F, 0.0F))
            ));

    @TabItem("rituals_tab")
    public static final DeferredItem<FlawlessItem> EXQUISITE_DIAMOND = ITEMS.register("exquisite_diamond",
            () -> new FlawlessItem(
                    new Item.Properties()
                            .component(RitualsDataComponents.FLAWLESSNESS.get(),
                                    new RitualsDataComponents.FlawlessnessInfo(100, 100))
            )
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
