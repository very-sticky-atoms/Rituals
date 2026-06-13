
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

一、脚本可用的变量
level       当前世界 (Level 对象)
pos         祭坛坐标 (BlockPos)，可调用 pos.getX()、pos.getY()、pos.getZ()
center      祭坛中心物品 (MutableItemStack)
directions  八个方向的物品 (List<List<MutableItemStack>>)
二、脚本可用的 API 对象：rituals
方法	说明
rituals.setOutput(itemId)	设置输出物品（仅 ID，数量=1，无组件）
rituals.setOutput(stack)	设置输出物品（MutableItemStack 对象）
rituals.setInput(direction, index, itemId)	返还物品到指定方向/位置（仅 ID）
rituals.setInput(direction, index, stack)	返还物品到指定方向/位置（MutableItemStack）
rituals.createItemStack(itemId, count)	创建一个新的 MutableItemStack 对象
三、MutableItemStack 对象的方法
方法	说明
stack.getId()	获取物品 ID，如 "minecraft:diamond"
stack.setId(itemId)	修改物品 ID
stack.getCount()	获取数量
stack.setCount(n)	修改数量
stack.hasDataComponent(componentId)	是否有某个数据组件
stack.getDataComponent(componentId)	获取数据组件的 JSON 字符串
stack.setDataComponent(componentId, jsonString)	设置数据组件

