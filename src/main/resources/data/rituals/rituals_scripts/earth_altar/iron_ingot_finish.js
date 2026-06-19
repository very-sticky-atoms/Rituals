var output = Item.of('minecraft:iron_ingot');

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

context.withCenter(output);