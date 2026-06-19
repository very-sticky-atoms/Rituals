package com.ccyscnyz.rituals.registry;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.block.entity.EarthAltarBlockEntity;
import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
import com.ccyscnyz.rituals.block.entity.RitualPillarBlockEntity;
import com.ccyscnyz.rituals.util.SidedItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Rituals.MODID)
public class RitualsCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 高炉：仅当炉门打开时允许漏斗交互；移除左面（火种）交互
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                RitualsBlockEntities.HIGH_OVEN.get(),
                (blockEntity, side) -> {
                    if (blockEntity instanceof HighOvenBlockEntity entity) {
                        BlockState state = entity.getBlockState();
                        if (!state.getValue(HighOvenBlock.DOOR_OPEN)) {
                            return null; // 门关着，禁止所有漏斗交互
                        }
                        Direction facing = state.getValue(HighOvenBlock.FACING);
                        // 顶部：输入槽
                        if (side == Direction.UP) {
                            return new SidedItemHandler(entity.inventory, new int[]{0, 1, 2}, new int[]{}, null);
                        }
                        // 底部或背面：输出槽
                        if (side == Direction.DOWN || side == facing.getOpposite()) {
                            return new SidedItemHandler(entity.inventory, new int[]{}, new int[]{4}, null);
                        }
                        // 左面（原火种面）和其他面不提供能力
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