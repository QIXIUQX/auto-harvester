# Auto Harvester（自动收割机）

Minecraft 26.2 Fabric 模组，添加一个自动收割机方块，可自动收割其朝向一侧 9×9 范围内的成熟农作物。

## 功能

- **自动收割**：每秒（20 tick）扫描朝向一侧的 9×9 区域（81 格，从紧挨方块的一行起向外延伸 8 格），收割成熟作物并自动补种
- **箱子存储**：除收割侧（正面）外，**背面、左侧、右侧三个方向都会检测容器**（普通箱子、大箱子或其它容器方块）。收割物按「背面 → 左侧 → 右侧」依次存入，一个装满后自动转下一个；三个方向都没有容器或全部装满时散落在地上
- **作物筛选**：右键打开 GUI，独立切换 10 种作物的收割开关
- **多语言**：物品名、分类名与界面文字全部走翻译键，随游戏语言切换（内置中英文本地化）
- **附加开关**：GUI 右上角两个开关——「满箱停收」（默认开）与「静音」（默认关，即默认有收获提示音）
- **补种规则**：小麦、胡萝卜、马铃薯、甜菜根、下界疣收割后补种为幼苗（age=0）；甜浆果丛采摘后重置为 age=1 保留植株；火把花、瓶子草植株、西瓜、南瓜只破坏不补种
- **支持作物**：小麦、胡萝卜、马铃薯、甜菜根、下界疣、火把花、瓶子草植株、西瓜、南瓜、甜浆果
- **挖掘要求**：需要石镐及以上的镐子（石/铜/铁/钻石/下界合金镐）才能挖下方块本身；空手、木镐、金镐或其它工具挖掉时会直接摧毁方块，不掉落任何物品
- **独立分类**：方块只出现在模组自己的创造模式分类「自动收割机」中，不会混入原版分类

## 合成配方

```
SSS
SFS
SSS
```

- S = `#minecraft:stone_crafting_materials`（圆石、黑石、深板岩圆石）
- F = `#minecraft:hoes`（木锄、石锄、铜锄、铁锄、金锄、钻石锄、下界合金锄）

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
├── creativetab/
│   └── ModCreativeTabs.java        # 模组自己的创造模式分类（菜单）
├── screen/
│   ├── ModScreenHandlers.java      # 菜单类型 + 作物数据定义
│   └── AutoHarvesterScreenHandler.java    # 菜单处理器（DataSlot 同步）
├── network/
│   ├── CropTogglePayload.java      # 作物切换网络数据包
│   └── SettingTogglePayload.java   # 附加开关（满箱停收/静音）网络数据包
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

数据文件（`src/main/resources/data/`）：

| 文件 | 作用 |
|------|------|
| `auto-harvester/loot_table/blocks/auto_harvester.json` | 方块被正确工具开采时的掉落物 |
| `auto-harvester/recipe/auto_harvester.json` | 合成配方（S = `#minecraft:stone_crafting_materials`，F = `#minecraft:hoes`） |
| `auto-harvester/advancement/recipe/misc/auto_harvester.json` | 获得圆石时解锁该配方 |
| `minecraft/tags/block/mineable/pickaxe.json` | 声明镐子为正确工具类型 |
| `minecraft/tags/block/needs_stone_tool.json` | 声明至少需要石质工具（木镐/金镐掉落被拒绝） |

> 注意：方块标签必须放在 `data/minecraft/tags/block/` 下（MC 1.21+ 起注册表标签目录改为 `tags/<注册表名>/`），
> 写成旧版路径 `data/minecraft/tags/mineable/...` 会被游戏完全忽略，导致方块挖掉后永远不掉落。

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/auto-harvester-0.0.2.jar`（版本号取自 `gradle.properties` 的 `version`）。

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/) 0.19.5+
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) 0.159.0+
3. 将 `auto-harvester-0.0.2.jar` 放入 `mods` 文件夹

## GUI 开关说明

右键方块打开界面：中间是作物开关列表（可滚动），**右上角是两个附加开关**。

界面采用**原版容器风格**：面板按原版容器面板的像素结构绘制（1px 黑描边 + 2px 白色高光/深灰阴影 + `#C6C6C6` 底色），作物图标放在原版格子贴图 `container/slot` 里，滚动条用原版黑色轨道 + 亮灰手柄，按钮是原版按钮控件；文字用原版压在浅灰底上的配色（深灰标题、深绿/深红作物名）。

列表右侧有滚动条（内容超过一屏时显示）：可拖动滑块、点击轨道跳转，也支持鼠标滚轮；滚到最底/最顶时下方会显示滚动提示文字。

界面按容器界面处理：**打开时不会暂停游戏**（与箱子、熔炉一致），因此打开界面期间箱子开合动画、生物和收割机本身都照常运行；关闭界面时会通知服务端关闭容器菜单。

| 开关 | 默认 | 开启时的行为 |
|------|------|--------------|
| **满箱停收** | **开** | 当后方/左侧/右侧**存在容器，但所有容器都装不下本次收获的任何一件物品**时，暂停收割，作物保持成熟状态留在原地；一旦腾出空间，下次扫描（1 秒内）自动恢复收割 |
| **静音** | 关 | 不再播放收获提示音（默认会播放经验球音效） |

三点说明：

- 「满箱停收」只在**真的一个都放不进去**时触发。只要有任意一个容器还能塞下一件收获物，就会照常收割，塞不下的部分才散落在地上。
- **一个容器都没有**时不会停收，仍然照常收割并让产物散落在地上（即上表的散落兜底行为）。
- 两个开关都会随方块一起保存到存档（NBT），重新进入世界后保持原状态。
- 界面文字全部走翻译键，切换游戏语言后立即生效（见下节）。

## 本地化

物品名、创造模式分类名和界面文字都用翻译键，切换语言后无需重启即可生效。模组自带 `zh_cn` 与 `en_us`，其它语言回退到 `en_us`。

模组自己的键（`assets/auto-harvester/lang/`）：

| 键 | 用途 |
|----|------|
| `block.auto-harvester.auto_harvester` | 方块/物品名，同时用作 GUI 标题 |
| `itemGroup.auto-harvester.main` | 创造模式分类标题 |
| `setting.auto-harvester.stop_when_full` | 「满箱停收」开关名 |
| `setting.auto-harvester.mute` | 「静音」开关名 |
| `gui.auto-harvester.scroll_hint` | 列表滚动提示 |

直接复用原版键，模组无需维护，且所有语种都由原版提供：

- **作物名**：`block.minecraft.wheat`、`block.minecraft.carrots` 等（由 `ModScreenHandlers.cropNameKey()` 从 `CROP_IDS` 生成），显示的就是原版方块名，例如中文「小麦作物」「甜浆果丛」
- **开关状态**：`options.on` / `options.off`
- **开关按钮格式**：`options.generic_value`（原版的「%s: %s」）
- **GUI 标题**：服务端构建 `Component.translatable`，客户端按自己的语言解析

> 界面面板宽度按「标题 + 两个开关按钮」的实际文字宽度动态计算：中文时保持 200px，英文等较长的语言会自动加宽，避免标题与按钮重叠。

## 朝向与收割方向

方块的 `facing` 属性在放置时自动设为玩家朝向的反方向（与熔炉、箱子写法一致，即**方块正面朝向放置者**），因此：

| 部位 | 相对方块 | 相对放置时的你 |
|------|----------|----------------|
| 收割区（9×9） | `facing` 正方向一侧（正面） | **你所站的那一侧** |
| 容器位（共 3 个） | 背面 `facing.getOpposite()`、左侧 `getCounterClockWise()`、右侧 `getClockWise()` | 背面 = **你面对的那一侧**，左右 = 与朝向垂直的两侧 |

实际操作用一句话概括：**背对农田放置方块**——站在农田边上、面朝准备放箱子的方向放下方块，收割区就在你身后。

容器只需放在**除正面以外的任意一面或多面**：放 1 个、2 个或 3 个都能用，写入优先级为 背面 → 左侧 → 右侧。（左/右以方块自身朝向为基准，俯视顺时针方向为右侧；实际摆箱子时不用刻意区分，三面都会被检测。）

> 方块四面贴图相同，没有可见的「正面」，只能靠放置时的站位判断朝向。
> 如果你期望的是「面朝农田放置就收割农田」，那当前方向差了 180°（需要去掉 `AutoHarvesterBlock.getStateForPlacement()` 里的 `getOpposite()`）；本文档按现有代码行为编写。

## 使用方法

1. 在创造模式物品栏的模组分类页找到「自动收割机」分类并取出方块（生存模式用下面的合成配方制作）
2. 站在农田一侧、面朝准备放箱子的方向放下方块（收割区会在你身后，即方块正面朝向你的那一面）
3. 在方块的背面或左右两侧放置箱子（三面都会检测，放一个即可；放多个时按 背面 → 左侧 → 右侧 依次装满）
4. 右键方块打开 GUI，选择要收割的作物，并按需切换右上角的「满箱停收 / 静音」开关
5. 等待作物成熟，自动收割机每秒扫描一次