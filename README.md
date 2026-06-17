
Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/




这是一份为你编写的技术文档草案。你可以将其直接放在你的 Mod 项目的 `docs` 目录中，或者集成到你的 Wiki/GitHub Readme 里。

---

# Rituals 脚本编写指南 (Rituals Scripting Guide)

本指南旨在帮助模组作者和配方编写者理解如何在 `rituals` 的自定义配方脚本中使用脚本引擎进行交互。

## 1. 核心概念

Rituals 的配方系统支持在“开始合成 (`craftStartScript`)”和“合成结束 (`craftFinishScript`)”时执行自定义 JavaScript 代码。

* **执行环境**：采用 GraalVM JS 引擎，支持标准 ES6+ 语法。
* **持久化上下文**：脚本运行在受限的服务器沙箱中，变量状态可在执行期间暂存。

---

## 2. 全局对象与工具 (Global API)

在脚本中，你可以直接使用以下全局对象：

### 2.1 `context` (环境上下文)

这是最重要的对象，包含当前合成操作的所有信息。

* **访问方法**：
* `context.level()`: 获取当前 `ServerLevel` 对象。
* `context.position()`: 获取祭坛的 `BlockPos` 坐标。
* `context.center()`: 获取当前处于祭坛中心的 `ItemStack`。


* **方法链**：
* `context.withCenter(itemStack)`: 修改合成的产物或中心物品。
* `context.level().destroyBlock(pos, drop)`: 在脚本中直接执行方块操作。



### 2.2 `Item` (物品工厂)

用于创建和操作物品堆。

* **`Item.of(itemId, count)`**: 快速创建一个新的 `ItemStack`。
* 示例：`var stick = Item.of('minecraft:stick', 1);`



### 2.3 `Utils` (实用工具)

用于处理复杂的物品组件和数据。

* **`Utils.setComponent(stack, componentId, value)`**:
* 设置物品的 Data Component（如名称、属性）。
* **参数说明**：支持 JSON 对象格式的属性设置。


* **`Utils.getComponent(stack, componentId)`**: 获取组件值。
* **`Utils.mergeCustomData(stack, compoundTag)`**: 合并自定义 NBT 数据。

---

## 3. 编写示例

### 示例 1：修改产物名称与样式

在 `craftFinishScript` 中，将产物命名为“神圣之棍”并添加金黄色加粗样式。

```javascript
// 创建物品
var output = Item.of('minecraft:stick');

// 设置名称组件 (支持标准 JSON 结构)
Utils.setComponent(output, 'minecraft:custom_name', {
    text: "TEST", 
    color: "gold", 
    bold: true
});

// 应用产物
context.withCenter(output);

```

### 示例 2：动态修改合成时间

在 `craftStartScript` 中，根据逻辑修改合成时间（`processingTime` 是一个数组，修改 index 0 即可）。

```javascript
// 如果中心物品是钻石，将时间缩短到 50 ticks
if (context.center().getItem().toString().contains('diamond')) {
    processingTime[0] = 50;
}

// 合成开始时，在祭坛位置产生一个爆炸效果
callback.value = (ctx) => {
    ctx.level().explode(null, ctx.position().getX(), ctx.position().getY(), ctx.position().getZ(), 2.0, true);
};

```

---

## 4. 常见问题 (FAQ)

**Q: 我可以调用原版的 Java 类吗？**
A: 是的，Rituals 的引擎允许通过全限定名调用 Minecraft 的 API。

* 示例：`const MobEffectInstance = Java.type('net.minecraft.world.effect.MobEffectInstance')` 

**Q: 脚本 crash 了怎么办？**
A: 请检查服务器日志，我们会生成 `==== RITUALS SCRIPT CRASH REPORT ====`，其中详细记录了脚本内容和失败原因。

---

## 5. 开发建议

* **调试**：在脚本中使用 `console.log()` 或 `Rituals.LOGGER.info()` 来输出变量值。
* **性能**：避免在脚本中执行过于复杂的循环操作，以免阻塞服务器主线程。

---

*如果您在编写过程中遇到困难，请查看最新的 [Data Component 列表](https://minecraft.wiki/w/Data_component_format) 以获取支持的组件 ID。*

