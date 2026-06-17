const MobEffectInstance = Java.type('net.minecraft.world.effect.MobEffectInstance');
var output = Item.of('minecraft:iron_ingot');
Utils.setComponent(output, 'minecraft:custom_name', {
    text: "DAMN!",
    color: "gold",
    bold: true
});
context.withCenter(output)