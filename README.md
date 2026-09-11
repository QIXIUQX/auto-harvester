# Auto Harvester（自动收割机）

Minecraft 26.2 Fabric 模组，添加一个自动收割机方块，可自动收割前方 9×9 范围内的成熟农作物。

## 功能

- **自动收割**：每秒扫描前方 9×9 区域，收割成熟作物并自动补种
- **箱子存储**：收割的物品自动存入方块后方的箱子
- **作物筛选**：右键打开 GUI，独立切换 7 种作物的收割开关
- **支持作物**：小麦、胡萝卜、马铃薯、甜菜根、地狱疣、火绒花、唱贝草

## 合成配方

```
SSS
SFS
SSS
```

- S = 任意石头类方块（圆石、石头、花岗岩、闪长岩、安山岩、深板岩、凝灰岩、平滑石头）
- F = 任意锄头（木锄、石锄、铁锄、金锄、钻石锄、下界合金锄、铜锄）

## 技术栈

| 组件 | 版本 |
|------|------|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.159.0+26.2 |
| Java | 25 |
| Loom | 1.17-SNAPSHOT |

## 项目结构

```
src/main/java/com/shiguang/
├── AutoHarvester.java              # 模组主入口
├── block/
│   ├── ModBlocks.java              # 方块注册
│   ├── ModBlockEntities.java       # 方块实体注册
│   ├── custom/
│   │   └── AutoHarvesterBlock.java # 方块逻辑（朝向、交互、ticker）
│   └── entity/
│       └── AutoHarvesterBlockEntity.java  # 核心收割逻辑
├── screen/
│   ├── ModScreenHandlers.java      # 菜单类型 + 作物数据定义
│   └── AutoHarvesterScreenHandler.java    # 菜单处理器（DataSlot 同步）
├── network/
│   └── CropTogglePayload.java      # 作物切换网络数据包
└── mixin/
    └── ExampleMixin.java           # 示例 Mixin

src/client/java/com/shiguang/client/
├── AutoHarvesterClient.java        # 客户端入口
├── AutoHarvesterDataGenerator.java # 数据生成入口
├── screen/
│   └── AutoHarvesterScreen.java    # GUI 界面
└── mixin/
    └── ExampleClientMixin.java     # 客户端示例 Mixin
```

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/auto-harvester-1.0.0.jar`。

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/) 0.19.5+
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) 0.159.0+
3. 将 `auto-harvester-1.0.0.jar` 放入 `mods` 文件夹

## 使用方法

1. 用合成配方制作自动收割机
2. 放置方块，面朝你希望收割的方向
3. 在方块后方放置箱子
4. 右键方块打开作物筛选 GUI，选择要收割的作物
5. 等待作物成熟，自动收割机每秒扫描一次