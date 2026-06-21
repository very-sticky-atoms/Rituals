
let output = Item.of('minecraft:iron_ingot');

Utils.setComponent(output, 'minecraft:custom_name', {
    text: "DAMN!",
    color: "gold",
    bold: true
});

Utils.setComponent(output, 'minecraft:enchantments', {
    levels: {
        "minecraft:fire_aspect": 114
    }
});

let tag = new CompoundTag();
tag.putString("ritual_source", "earth_altar");
tag.putInt("power_level", 5);

Utils.mergeCustomData(output, tag);

Utils.addCustomData(output, 'active', true);

let temp_tag = new CompoundTag()
tag.putBoolean("bad", true);
tag.putInt("badness", 114514);

Utils.addCustomData(output, 'temp_tag', tag);

Utils.removeCustomData(output, 'active');

context.withCenter(output);