package com.ccyscnyz.rituals.registry;

import com.ccyscnyz.rituals.Rituals;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Rituals.MODID);

    // 注册无瑕度组件
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlawlessnessInfo>> FLAWLESSNESS =
            COMPONENTS.register("flawlessness", () -> DataComponentType.<FlawlessnessInfo>builder()
                    .persistent(FlawlessnessInfo.CODEC)
                    .networkSynchronized(FlawlessnessInfo.STREAM_CODEC)
                    .build());

    // 组件的数据载体
    public record FlawlessnessInfo(int value, int max) {
        public static final Codec<FlawlessnessInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("value").forGetter(FlawlessnessInfo::value),
                        Codec.INT.fieldOf("max").forGetter(FlawlessnessInfo::max)
                ).apply(instance, FlawlessnessInfo::new));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, FlawlessnessInfo> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, FlawlessnessInfo::value,
                        ByteBufCodecs.VAR_INT, FlawlessnessInfo::max,
                        FlawlessnessInfo::new
                );

        public float getRatio() {
            return max <= 0 ? 0.0f : (float) value / max;
        }
    }
}