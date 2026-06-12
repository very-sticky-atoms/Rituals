package com.ccyscnyz.rituals.registry.capability;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.block.entity.EarthAltarBlockEntity;
import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
import com.ccyscnyz.rituals.block.entity.RitualPillarBlockEntity;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.util.SidedItemHandler;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Rituals.MODID)
public class RitualsCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RitualsBlockEntities.HIGH_OVEN.get(),
                (blockEntity, side) -> {
                    if (blockEntity instanceof HighOvenBlockEntity entity) {
                        if (side == null) return null;
                        Direction facing = entity.getBlockState().getValue(HighOvenBlock.FACING);
                        Direction leftSide = facing.getClockWise();

                        // 正面：输入槽
                        if (side == facing) {
                            return new SidedItemHandler(entity.inventory, new int[]{0, 1, 2}, new int[]{}, null);
                        }
                        // 左面：火种槽，限制1个
                        else if (side == leftSide) {
                            return new SidedItemHandler(entity.inventory, new int[]{3}, new int[]{}, 1);
                        }
                        // 底部和背面：输出槽
                        else if (side == Direction.DOWN || side == facing.getOpposite()) {
                            return new SidedItemHandler(entity.inventory, new int[]{}, new int[]{4}, null);
                        }
                        // 顶部、右侧等其他面：无漏斗交互
                        return null;
                    }
                    return null;
                }
        );


        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RitualsBlockEntities.EARTH_ALTAR.get(),
                (blockEntity, side) -> {
                    if (blockEntity instanceof EarthAltarBlockEntity entity) {
                        return entity.inventory;
                    }
                    return null;
                }
        );


        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RitualsBlockEntities.RITUAL_PILLAR.get(),
                (blockEntity, side) -> {
                    if (blockEntity instanceof RitualPillarBlockEntity entity) {
                        return entity.inventory;
                    }
                    return null;
                }
        );
    }
}