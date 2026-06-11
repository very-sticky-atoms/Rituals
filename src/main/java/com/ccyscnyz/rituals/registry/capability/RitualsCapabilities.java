package com.ccyscnyz.rituals.registry.capability;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
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

                        if (side == Direction.UP) {
                            // 顶部：只插入输入槽
                            return new SidedItemHandler(entity.inventory, new int[]{0, 1, 2}, new int[]{}, null);
                        } else if (side == facing) {
                            // 正面：只插入火种槽，限制1个
                            return new SidedItemHandler(entity.inventory, new int[]{3}, new int[]{}, 1);
                        } else if (side == Direction.DOWN || side == facing.getOpposite()) {
                            // 底部和背面：只提取输出槽
                            return new SidedItemHandler(entity.inventory, new int[]{}, new int[]{4}, null);
                        }
                    }
                    return null;
                }
        );
    }
}