package com.ccyscnyz.rituals.datagen;

import com.ccyscnyz.rituals.Rituals;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RitualsLanguageProvider extends LanguageProvider {

    private final Map<String, String> existingTranslations = new HashMap<>();
    private final PackOutput packOutput;

    public RitualsLanguageProvider(PackOutput output, String locale) {
        super(output, Rituals.MODID, locale);
        this.packOutput = output;
        loadExistingTranslations(locale);
    }

    private void loadExistingTranslations(String locale) {
        // 优先加载 DataGen 输出目录下的现有文件
        Path generatedPath = packOutput.getOutputFolder()
                .resolve("assets/" + Rituals.MODID + "/lang/" + locale + ".json");
        loadFromPath(generatedPath);

        // 后备加载主资源目录下的文件
        Path sourcePath = Path.of("src/main/resources/assets/" + Rituals.MODID + "/lang/" + locale + ".json");
        loadFromPath(sourcePath);
    }

    private void loadFromPath(Path path) {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> map = new Gson().fromJson(reader, type);
                if (map != null) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        existingTranslations.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }
            } catch (IOException e) {
                Rituals.LOGGER.warn("Failed to load language file from {}: {}", path, e.getMessage());
            }
        }
    }

    @Override
    public void add(String key, String value) {
        // 如果已从旧文件中加载了翻译，则使用旧值，否则使用新值（空字符串）
        String finalValue = existingTranslations.getOrDefault(key, value);
        try {
            super.add(key, finalValue);
        } catch (IllegalStateException e) {
            Rituals.LOGGER.error("Skipping duplicate translation key: {}", key);
        }
    }

    @Override
    protected void addTranslations() {
        // 方块
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id.getNamespace().equals(Rituals.MODID)) {
                add(block.getDescriptionId(), "");
            }
        }

        // 物品（跳过 BlockItem 避免重复）
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(Rituals.MODID)) continue;
            if (item instanceof BlockItem) continue;
            add(item.getDescriptionId(), "");
        }

        // 实体类型
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (id.getNamespace().equals(Rituals.MODID)) {
                add(entityType.getDescriptionId(), "");
            }
        }

        // 状态效果
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id.getNamespace().equals(Rituals.MODID)) {
                add(effect.getDescriptionId(), "");
            }
        }

        // 创造模式标签页
        BuiltInRegistries.CREATIVE_MODE_TAB.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(Rituals.MODID))
                .forEach(entry -> add(entry.getValue().getDisplayName().getString(), ""));

        // 手动添加其他模组键
        add("itemGroup.rituals.rituals_tab", "");
        add("jei.rituals.high_oven", "");
        add("jei.rituals.earth_altar", "");
        add("tooltip.rituals.flawlessness", "");
    }
}