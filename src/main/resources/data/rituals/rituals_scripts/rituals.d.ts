// rituals.d.ts
// 将此文件放在项目根目录或任意源代码目录下

// ==================== Java 注入类 ====================

// ItemStack.class
declare class ItemStack {
    copy(): ItemStack;
    getCount(): number;
    getItem(): object;
    isEmpty(): boolean;
    setCount(count: number): void;
    isDamageableItem(): boolean;
}

// Items.class（静态字段）
declare namespace Items {
}

// CompoundTag.class
declare class CompoundTag {
    constructor();
    putString(key: string, value: string): void;
    putInt(key: string, value: number): void;
    putBoolean(key: string, value: boolean): void;
    putDouble(key: string, value: number): void;
    putFloat(key: string, value: number): void;
    putLong(key: string, value: number): void;
    put(key: string, value: any): void;
    putIntArray(key: string, value: number[]): void;
    putLongArray(key: string, value: number[]): void;
    getString(key: string): string;
    getInt(key: string): number;
    getBoolean(key: string): boolean;
    getCompound(key: string): CompoundTag;
    contains(key: string): boolean;
    remove(key: string): void;
    merge(other: CompoundTag): CompoundTag;
    isEmpty(): boolean;
}

// ==================== 自定义全局对象 ====================

declare var Item: {
    of(itemId: string, count?: number): ItemStack;
};

declare var Utils: {
    setComponent(stack: ItemStack, id: string, val: any): ItemStack;
    getComponent(stack: ItemStack, id: string): any;
    removeComponent(stack: ItemStack, id: string): ItemStack;
    mergeCustomData(stack: ItemStack, tag: CompoundTag): ItemStack;
    addCustomData(stack: ItemStack, key: string, value: any): ItemStack;
    removeCustomData(stack: ItemStack, key: string): ItemStack;
};

// ==================== 运行时上下文 ====================

interface Level {
    isRaining(): boolean;
    isThundering(): boolean;
    isNight(): boolean;
    getDayTime(): number;
    getMoonPhase(): number;
    random(): any;
}

interface BlockPos {
    getX(): number;
    getY(): number;
    getZ(): number;
}

interface EarthAltarRecipeContext {
    center(): ItemStack;
    directionItems(): ItemStack[][];
    level(): Level;
    position(): BlockPos;
    withCenter(center: ItemStack): EarthAltarRecipeContext;
    withDirectionItems(dirs: ItemStack[][]): EarthAltarRecipeContext;
    copy(): EarthAltarRecipeContext;
}

interface ContextContainer {
    value: EarthAltarRecipeContext;
    withCenter(center: ItemStack): ContextContainer;
    withDirectionItems(dirs: ItemStack[][]): ContextContainer;
}

declare var context: ContextContainer;
declare var callback: { value: (ctx: EarthAltarRecipeContext) => void };
declare var processingTime: number[];
declare var doOverwrite: boolean[];