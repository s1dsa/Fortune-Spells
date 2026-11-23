# 命运法术（Fortune Spells）设计文档与前后端契约 v2.0

> 铁魔法（Iron's Spells 'n Spellbooks）× 迎战（Meet Your Fight）联动模组。第一轮 0.1.0 的第 1～12 节记录福尔图娜女爵的三个命运法术。第二轮 0.2.0～0.2.6 增加敲钟人、沼泽巨鳄怪和罗莎琳的八个法术，见[第 13 节](#sec-13)。

| 项 | 内容 |
|---|---|
| 规划、审美与实现 | 11-23 起由 astra 统筹，负责前后端代码、美术与构建。 |
| 文档、审查与验收 | 审查。此前各版的署名保留在历史记录中。 |
| 版本 | 第一轮为 0.1.0，第二轮从 0.2.0 起。当前版本与下载见[发布说明](发布说明.md)，宠物目标规则见 13.6。 |
| 适用范围 | 第 1～12 节记录第一轮，第 13 节记录第二轮八个法术，第 14 节记录觐见仪式与法杖。 |

- **改契约的规矩**：要改契约里的 ID、包、网络包字段或同步数据，必须先改本文，并在频道 #111 里说一声。
- **ID 常量类** `src/main/java/io/myf/fortune/FortuneIds.java`：第②步由 astra 按第 3、8 节的表建立，之后改动要先改本文。代码里一律引用这个类的常量，**不要另起字符串**。

> **已定（opus 11-21）**：沿用工作区里现有的 `FortuneIds.java`（内容和第 3、8 节逐项一致），astra 第②步按只读使用，没有重建。

---

<a id="sec-0"></a>
## 0. 目录

1. [版本锁定与依赖](#sec-1)
2. [modid、包结构、文件归属（前端 / 后端）](#sec-2)　· [2.1 包结构与归属](#sec-2-1)
3. [注册 ID 总表](#sec-3)
4. [命运学派定义（学派物品、合成、升级）](#sec-4)　· [4.1](#sec-4-1) · [4.2](#sec-4-2) · [4.3](#sec-4-3) · [4.4](#sec-4-4)
5. [三个法术完整规格](#sec-5)　· [5.1 命运之选](#sec-5-1) · [5.2 盲眼赌局](#sec-5-2) · [5.3 孤注一掷](#sec-5-3)
6. [状态效果：两个 buff + 孤注一掷标记效果](#sec-6)　· [6.1](#sec-6-1) · [6.2](#sec-6-2) · [6.3](#sec-6-3)
7. [命运伤害类型与「无视护甲 / 防御 / 限伤」的实现](#sec-7)　· [7.1](#sec-7-1) · [7.2](#sec-7-2) · [7.3](#sec-7-3)
8. [猜牌小游戏：服务端状态机、卡牌实体、网络协议](#sec-8)　· [8.1](#sec-8-1) · [8.2](#sec-8-2) · [8.3](#sec-8-3) · [8.4](#sec-8-4) · [8.5](#sec-8-5) · [8.6](#sec-8-6)
9. [美术规范（贴图、特效、声音、HUD）](#sec-9)　· [9.1](#sec-9-1) · [9.2](#sec-9-2) · [9.3](#sec-9-3) · [9.4](#sec-9-4) · [9.5](#sec-9-5) · [9.6](#sec-9-6) · [9.7](#sec-9-7)
10. [语言键清单](#sec-10)
11. [构建与运行步骤、验收清单](#sec-11)　· [11.1](#sec-11-1) · [11.2](#sec-11-2)
12. [已知限制 / 风险](#sec-12)
13. [第二轮：钟鸣、腐沼、蔷薇三学派与超位法术（0.2.0～0.2.6）](#sec-13)　· [13.1](#sec-13-1) · [13.2](#sec-13-2) · [13.3](#sec-13-3) · [13.4](#sec-13-4) · [13.5](#sec-13-5) · [13.6](#sec-13-6) · [13.7](#sec-13-7) · [13.8](#sec-13-8)

14. [觐见仪式、法杖与爆裂魔法](#sec-14)

附：[变更记录](#changelog)

相关文档：[`README.md`](../README.md)（构建与安装）、[`docs/PLAYER_GUIDE.md`](PLAYER_GUIDE.md)（玩家说明）、[`docs/需求记录.md`](需求记录.md)、[`docs/需求记录-第二轮.md`](需求记录-第二轮.md)（用户原始需求）。

---

<a id="sec-1"></a>
## 1. 版本锁定与依赖

| 项 | 版本 | 坐标 / 来源 |
|---|---|---|
| Minecraft | 1.20.1 | — |
| Forge | **47.4.0**（铁魔法 3.16.x 要求 `[47.4.0,)`） | `net.minecraftforge:forge:1.20.1-47.4.0` |
| Java | 17 | Temurin 17 |
| Gradle / ForgeGradle | Gradle 8.1.1 wrapper / FG `[6.0.16,6.2)` | 与 1.20.1 MDK 一致 |
| 映射 | `official` / `1.20.1` | 不用 parchment，少一个插件 |
| Iron's Spells | **1.20.1-3.16.4**（锁死这个版本写代码） | `io.redspace:irons_spellbooks:1.20.1-3.16.4` @ `https://code.redspace.io/releases` |
| Iron's Lib | 1.20.1-2.1.0（铁魔法 3.16.4 要求 `[1.20.1-2,1.20.1-3)`） | `io.redspace:irons_lib:1.20.1-2.1.0` 同上仓库 |
| Meet Your Fight | 1.20.1-1.6.2 | Modrinth maven：`maven.modrinth:meet-your-fight:f5pZH4Tl`（版本 id）@ `https://api.modrinth.com/maven` |
| Curios | 5.14.1+1.20.1 | `top.theillusivec4.curios:curios-forge:5.14.1+1.20.1` @ `https://maven.theillusivec4.top` |
| GeckoLib | 4.8.2 | `software.bernie.geckolib:geckolib-forge-1.20.1:4.8.2` @ `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/` |
| Player Animator（铁魔法运行时必需） | 1.0.2-rc1+1.20 | `dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20` @ `https://maven.kosmx.dev/` |

`build.gradle` 依赖块（后端照抄；解析失败就换 cursemaven，并在频道报）：

```groovy
repositories {
    maven { url = "https://code.redspace.io/releases" }
    maven { url = "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/" }
    maven { url = "https://maven.theillusivec4.top" }
    maven { url = "https://maven.kosmx.dev/" }
    maven { url = "https://api.modrinth.com/maven"; content { includeGroup "maven.modrinth" } }
    maven { url = "https://cursemaven.com"; content { includeGroup "curse.maven" } }
}
dependencies {
    minecraft "net.minecraftforge:forge:1.20.1-47.4.0"
    implementation fg.deobf("io.redspace:irons_spellbooks:1.20.1-3.16.4")   // 用完整包而非 :api，需要 SyncManaPacket 等内部类
    runtimeOnly    fg.deobf("io.redspace:irons_lib:1.20.1-2.1.0")
    implementation fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.2")
    implementation fg.deobf("top.theillusivec4.curios:curios-forge:5.14.1+1.20.1")
    runtimeOnly    fg.deobf("dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20")
    implementation fg.deobf("maven.modrinth:meet-your-fight:f5pZH4Tl")
}
minecraft {
    runs.configureEach {
        // 铁魔法带 mixin，开发环境要重映射 refmap，否则 runClient 崩
        property 'mixin.env.remapRefMap', 'true'
        property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
    }
}
```

**API 版本注意（后端必读）**：网上很多示例是 1.20.1-legacy（3.4.x）的写法，和 3.16.x 不一样：
- `SchoolType` 用 **Supplier 版构造器**（9 参：`id, focusTag, displayName, Supplier<Attribute> power, Supplier<Attribute> resist, Supplier<SoundEvent> castSound, ResourceKey<DamageType>, requiresLearning, allowLooting`），不要用 LazyOptional 的老构造器（已 @Deprecated）。
- 网络工具类是 `io.redspace.ironsspellbooks.setup.PacketDistributor`（不是 `Messages`），同步法力用 `io.redspace.ironsspellbooks.network.SyncManaPacket`。
- 法术配置是数据包驱动（`SpellConfigManager`），默认值来自 `getDefaultConfig()`。
- 1.20.1 原版没有 `ResourceLocation.fromNamespaceAndPath`，我们自己的代码一律 `new ResourceLocation(ns, path)`。

---

<a id="sec-2"></a>
## 2. modid、包结构、文件归属

- **modid**：`fortune_spells`
- **Maven group / 包根**：`io.myf.fortune`
- **显示名**：`命运法术 Fortune Spells`；版本 `0.1.0`；license `All Rights Reserved`（自用 / 整合包）
- **mods.toml 依赖**（side 全部 BOTH，mandatory=true）：`forge [47.4.0,)`、`minecraft [1.20.1,1.20.2)`、`irons_spellbooks [1.20.1-3.16.4,)`、`meetyourfight [1.5.0,)`、`curios [5.14.1,)`、`geckolib [4.8.2,)`；ordering：对 `irons_spellbooks`、`meetyourfight` 写 `AFTER`。

<a id="sec-2-1"></a>
### 2.1 包结构与归属

| 路径（`src/main/java/io/myf/fortune/…`） | 内容 | 归属 |
|---|---|---|
| `FortuneSpells.java` | `@Mod("fortune_spells")` 入口；挂 DeferredRegister、网络、配置 | 后端 |
| `FortuneIds.java` | **全部 ID 与同步枚举值常量（契约）**；第②步由 astra 按第 3、8 节的表建立 | 后端（astra 建立）；之后改动要先改本文 |
| `FortuneTuning.java` | 数值常量（第 5～8 节的数字全在这里） | 后端 |
| `registry/FSSchools.java` | 命运学派注册 | 后端 |
| `registry/FSSpells.java` | 3 个法术注册 | 后端 |
| `registry/FSEffects.java` | 3 个效果注册 | 后端 |
| `registry/FSEntities.java` | `fate_card` 实体类型 | 后端 |
| `registry/FSSounds.java` | 全部 SoundEvent 注册（sounds.json 归前端） | 后端 |
| `registry/FSParticles.java` | 3 个 `SimpleParticleType` 注册（Provider 归前端） | 后端 |
| `registry/FSAttributes.java` | 学派强度 / 抗性属性 + `EntityAttributeModificationEvent` | 后端 |
| `registry/FSDamageTypes.java` | 两个 `ResourceKey<DamageType>` + 构造 DamageSource 的工具 | 后端 |
| `spell/CardSelectionSpell.java` `spell/BlindGambleSpell.java` `spell/AllInSpell.java` | 法术逻辑（服务端） | 后端 |
| `effect/BlessedByFortuneEffect.java` `effect/CursedByFateEffect.java` `effect/AllInEffect.java` | 效果类（无客户端代码） | 后端 |
| `cardgame/CardGameManager.java` `cardgame/CardGameSession.java` | 猜牌对局状态机（仅服务端） | 后端 |
| `entity/FateCardEntity.java` | 卡牌实体（共享类，见 8.3） | 后端 |
| `combat/FortuneCombatHandler.java` `combat/ManaLockHandler.java` | 伤害翻倍 / 命运伤害 / 法力锁 0 | 后端 |
| `network/FSNetwork.java` + 5 个包类 | 通道注册、编解码、服务端处理；S2C 的 handle 只转发给 `client.ClientPacketHandler` | 后端 |
| `client/**` | **全部客户端代码** | astra（opus 只负责规划和美术把关，代码全部由 astra 写） |
| `client/ClientPacketHandler.java` | S2C 包的客户端入口（签名见 8.6；第②步后端先建空壳让编译过，第④步起归前端） | 前端 |
| `client/FSClientSetup.java` | MOD 总线、`Dist.CLIENT`：渲染器、模型层、粒子 Provider | 前端 |
| `client/FSClientEvents.java` | FORGE 总线、`Dist.CLIENT`：输入拦截、HUD、客户端 tick | 前端 |
| `client/render/FateCardRenderer.java` `client/render/FateCardModel.java` | 卡牌渲染 | 前端 |
| `client/cardgame/ClientCardGame.java` `CardPickInput.java` `CardGameHud.java` | 客户端对局状态、点选、HUD | 前端 |
| `client/fx/FortuneFx.java` `client/particle/*.java` | 粒子与特效 | 前端 |

| 资源路径 | 归属 |
|---|---|
| `src/main/resources/META-INF/mods.toml`、`pack.mcmeta` | 后端 |
| `src/main/resources/data/**`（伤害类型、标签） | 后端 |
| `src/main/resources/assets/fortune_spells/**`（贴图、模型、lang、sounds.json、particles/*.json） | 前端 |

> **分工（Tommyxu 11-21 定）**：本文里的「前端」「后端」只表示代码属于客户端还是服务端，**不表示谁来写**。两端的代码、资源文件、构建配置，以及审查退回后的修复，全部由 astra 按 opus 的规划实现。opus 只负责规划和审美，不改代码。

**跨端铁律**（第⑦步按这 4 条审）：
1. `client` 包以外的类不得 import `net.minecraft.client.*` 或 `io.myf.fortune.client.*`。
2. S2C 包的 `handle` 里只能写 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.onXxx(msg))`。
3. 客户端订阅类一律 `@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT, bus = …)`。
4. 实体里的客户端动画字段只用基本类型（int），不引用客户端类。

---

<a id="sec-3"></a>
## 3. 注册 ID 总表

所有 ID 的命名空间都是 `fortune_spells`，对应的常量都定义在 `FortuneIds` 里。

| 类别 | ID | 中文名 | 英文名 | 备注 |
|---|---|---|---|---|
| 学派（Iron's `schools` 注册表） | `fortune_spells:fortune` | 命运 | Fortune | |
| 法术 1 | `fortune_spells:card_selection` | 命运之选 | Fate's Choice | |
| 法术 2 | `fortune_spells:blind_gamble` | 盲眼赌局 | Blind Gamble | |
| 法术 3 | `fortune_spells:all_in` | 孤注一掷 | All In | |
| 效果 | `fortune_spells:blessed_by_fortune` | 被命运选中 | Fortune's Chosen | 增益 |
| 效果 | `fortune_spells:cursed_by_fate` | 被命运背弃 | Forsaken by Fate | 减益 |
| 效果 | `fortune_spells:all_in` | 孤注一掷 | All In | 「下一击翻倍」的可见标记（设计新增，见 6.3） |
| 伤害类型 | `fortune_spells:fortune` | 命运伤害 | Fate | buff 转换后的真伤 |
| 伤害类型 | `fortune_spells:fortune_magic` | 命运魔法 | Fortune Magic | 学派默认伤害类型，给以后的攻击法术用，本轮无人使用 |
| 属性 | `fortune_spells:fortune_spell_power` | 命运法术强度 | Fortune Spell Power | 默认 1.0，范围 -100～100 |
| 属性 | `fortune_spells:fortune_magic_resist` | 命运魔法抗性 | Fortune Magic Resist | 同上 |
| 实体 | `fortune_spells:fate_card` | 命运之牌 | Fate Card | |
| 粒子 | `fortune_spells:fate_spark` / `card_shard` / `forsaken_mote` | | | `SimpleParticleType(false)` |
| 网络通道 | `fortune_spells:main` | | | 协议版本 `"1"` |
| 物品标签 | `fortune_spells:fortune_focus` | | | 学派物品标签 |

声音事件：全部带 `fortune_spells:` 前缀；sounds.json 用 `"type":"event"` 指向迎战或原版的现成声音，我们不复制任何音频文件。

| SoundEvent ID | 指向 | 用途 |
|---|---|---|
| `spell.fortune.cast` | `meetyourfight:entity.dame_fortuna.snap` | 学派默认施法完成音（响指） |
| `spell.fortune.charge` | `meetyourfight:entity.dame_fortuna.chips.start` | 命运之选 / 盲眼赌局开始吟唱 |
| `card.appear` | `meetyourfight:entity.dame_fortuna.card.start` | 每张牌出现 |
| `card.shuffle` | `meetyourfight:entity.dame_fortuna.spin.start` | 开始洗牌 |
| `card.flip` | `minecraft:item.book.page_turn` | 翻牌 |
| `card.right` | `meetyourfight:entity.dame_fortuna.card.right` | 猜中 |
| `card.wrong` | `meetyourfight:entity.dame_fortuna.card.wrong` | 猜错 |
| `fate.laugh` | `meetyourfight:entity.dame_fortuna.attack` | 盲眼赌局输了，女爵笑声 |
| `fate.strike` | `meetyourfight:item.proc.slicers_dice` | 「被命运选中」触发双倍（音高 1.2） |
| `all_in.cast` | `meetyourfight:entity.dame_fortuna.chips.fire` | 孤注一掷施放 |
| `all_in.proc` | `meetyourfight:item.proc.slicers_dice` | 孤注一掷翻倍命中（音高 0.8） |

---

<a id="sec-4"></a>
## 4. 命运学派定义

<a id="sec-4-1"></a>
### 4.1 SchoolType（后端，`FSSchools`）

```java
new SchoolType(
    FortuneIds.SCHOOL_FORTUNE,                                   // fortune_spells:fortune
    FortuneIds.TAG_FORTUNE_FOCUS,                                // #fortune_spells:fortune_focus
    Component.translatable("school.fortune_spells.fortune").withStyle(Style.EMPTY.withColor(0xF0506E)),
    FSAttributes.FORTUNE_SPELL_POWER,                            // Supplier<Attribute>
    FSAttributes.FORTUNE_MAGIC_RESIST,
    FSSounds.SPELL_FORTUNE_CAST,                                 // Supplier<SoundEvent>
    FSDamageTypes.FORTUNE_MAGIC,                                 // 学派默认伤害类型
    false,                                                       // requiresLearning
    false)                                                       // allowLooting：不进铁魔法随机战利品，只能合成
```

- 学派文字色：**筹码红 `#F0506E`**（和火的金橙、血的暗红、末影的亮紫区分得开）。
- 注册：`DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, MODID)`，注册名 `fortune`。
- 属性：`MagicRangedAttribute("attribute.fortune_spells.fortune_spell_power", 1.0, -100, 100).setSyncable(true)`，抗性同理；在 MOD 总线 `EntityAttributeModificationEvent` 里给 `e.getTypes()` 全部加上（铁魔法自己也是这么做的）。

<a id="sec-4-2"></a>
### 4.2 学派物品 = 命运之眷（数据文件，后端）

```
data/fortune_spells/tags/items/fortune_focus.json      {"replace":false,"values":["meetyourfight:fortunes_favor"]}
data/irons_spellbooks/tags/items/school_focus.json     {"replace":false,"values":["meetyourfight:fortunes_favor"]}
```
第二个文件必须有：卷轴合成台的学派物品槽只收 `#irons_spellbooks:school_focus`。

<a id="sec-4-3"></a>
### 4.3 合成与升级（全走铁魔法原机制，我们不写配方）

- **合成**：卷轴合成台（Scroll Forge）放 墨水 + 纸 + 命运之眷 → 列表出现 3 个命运法术 → 选一个出卷轴。墨水稀有度决定起始等级；低于法术最低稀有度时铁魔法自己会把该法术标灰（「墨水不够」）。
- **升级**：奥术铁砧（Arcane Anvil）放 卷轴 + 墨水，墨水稀有度 = 下一级的稀有度才能升。
- 按铁魔法默认稀有度配置 `[.3,.25,.2,.15,.1]` 推算出的结果（第⑤步实测核对）：

| 法术 | 最低稀有度 | 最高等级 | 史诗墨水 | 传说墨水 | 升级路线 |
|---|---|---|---|---|---|
| 命运之选 | 史诗 | 3 | 1 级 | 2 级 | 1→2、2→3 都要传说墨水 |
| 盲眼赌局 | 传说 | 2 | 不可做 | 1 级 | 1→2 要传说墨水 |
| 孤注一掷 | 传说 | 1 | 不可做 | 1 级 | 不可升级 |

> 「史诗或传说墨水才能做」由此满足；用传说墨水直接出 2 级是铁魔法统一规则，不改。

<a id="sec-4-4"></a>
### 4.4 卷轴与亲和戒指模型（前端）

铁魔法会按学派自动注册额外模型 `fortune_spells:item/scroll_fortune` 和 `fortune_spells:item/affinity_ring_fortune`，**两个都必须提供**，否则会报缺模型：
```
assets/fortune_spells/models/item/scroll_fortune.json         → layer0 = fortune_spells:item/scroll_fortune
assets/fortune_spells/models/item/affinity_ring_fortune.json  → layer0 = fortune_spells:item/affinity_ring_fortune
```
贴图自己画（铁魔法素材禁止复用，见第 9 节）。

---

<a id="sec-5"></a>
## 5. 三个法术完整规格

**三个法术的公共项**：
- 都 `extends AbstractSpell`；`getSpellResource()` 返回 `FortuneIds.SPELL_*`；学派 `fortune_spells:fortune`。
- 施法完成音 `getCastFinishSound()` 用学派默认（响指）；命运之选和盲眼赌局的 `getCastStartSound()` 用 `spell.fortune.charge`。
- 吟唱时间受铁魔法「施法速度」属性影响，冷却受「冷却缩减」属性影响（铁魔法统一规则）；**等级不影响吟唱时间**。
- 从卷轴施放时，按铁魔法规则不耗法力、不进冷却。
- 创造模式下铁魔法默认不算冷却，测冷却要用生存模式。

<a id="sec-5-1"></a>
### 5.1 命运之选 `card_selection`

| 项 | 值 |
|---|---|
| CastType | `LONG`，`castTime = 60`（3 秒） |
| 冷却 | 180 秒 |
| 法力 | `baseManaCost = 60`，`manaCostPerLevel = 10`（60 / 70 / 80） |
| 最低稀有度 / 最高等级 | EPIC / 3 |
| 牌数 | 5 |
| 猜中奖励 | 「被命运选中」，时长 `(10 + 20 × 等级)` 秒 → 30 / 50 / 70 秒（亲和戒指加的额外等级按同一公式延伸） |
| 猜错 | 无奖励、无惩罚 |
| 流程 | 吟唱 3 秒 → 面前发 5 张明牌 → 目标牌晃动 + 金光提示 → 全部翻成背面 → 洗牌（女爵原版的环绕旋转 + 落位） → 限时 10 秒点选 → 翻牌揭晓 |
| `checkPreCastConditions` | 已在牌局中 → `false`，动作栏提示 `ui.fortune_spells.cast_error_in_game` |
| `onCast` | 施法者不是 `ServerPlayer` → 直接 return（怪物不会玩牌）；否则 `CardGameManager.start(player, FATES_CHOICE, level)` |
| `getUniqueInfo` | `ui.fortune_spells.chosen_duration`（秒数）、`ui.fortune_spells.card_count`（5） |

<a id="sec-5-2"></a>
### 5.2 盲眼赌局 `blind_gamble`

| 项 | 值 |
|---|---|
| CastType | `LONG`，`castTime = 60` |
| 冷却 | 3600 秒（1 小时） |
| 法力 | `baseManaCost = 80`，`manaCostPerLevel = 0` |
| 最低稀有度 / 最高等级 | LEGENDARY / 2 |
| 牌数 | 1 级 5 张；**≥2 级 4 张** |
| 胜 | 「被命运选中」15 分钟（18000 tick） |
| 负 | 「被命运背弃」30 分钟（36000 tick） |
| 流程 | 吟唱 3 秒 → 直接发背面朝上的牌（不提示、不洗牌） → 限时 10 秒点选 → 揭晓；输了时把真正的那张也翻出来 |
| 赢牌牌面 | 赢牌固定是我们画的「命运女爵（Joker）」牌，其余随机花色——揭晓时一眼看出差距 |
| `getUniqueInfo` | `card_count`、`ui.fortune_spells.blind_win`（15）、`ui.fortune_spells.blind_lose`（30） |

<a id="sec-5-3"></a>
### 5.3 孤注一掷 `all_in`

| 项 | 值 |
|---|---|
| CastType | `INSTANT`（`castTime = 0`） |
| 冷却 | 5 秒 |
| 法力 | `baseManaCost = 20` |
| 最低稀有度 / 最高等级 | LEGENDARY / 1 |
| `onCast` | 给施法者加 `fortune_spells:all_in` 效果，时长**无限**（`MobEffectInstance.INFINITE_DURATION`），已有则不叠加；发 `S2CFortuneFx(ALL_IN_PRIMED)`；播 `all_in.cast` |
| 效果 | 施法者造成的**下一次伤害**（伤害量 > 0 才算）×2，然后移除效果 |
| 与「被命运选中」叠加 | **乘算**：50% 判定在 `hurt()` 入口先做（7.2 第 1 步），孤注一掷与命运的两个 ×2 在进入原版 `actuallyHurt` 前一起乘上（7.2 第 3 步）→ 最高 ×4 |
| `getUniqueInfo` | `ui.fortune_spells.next_hit_double` |
| 怪物施放 | 允许（效果对任何 LivingEntity 都生效） |

---

<a id="sec-6"></a>
## 6. 状态效果

三个效果的共同点：`getCurativeItems()` 返回空列表（**牛奶不能解**）；效果颜色同时也是原版药水旋涡粒子的颜色。

<a id="sec-6-1"></a>
### 6.1 被命运选中 `blessed_by_fortune`
- `MobEffectCategory.BENEFICIAL`，颜色 `0xF5C542`（牌桌金）。
- 效果：持有者造成的任意伤害（近战、弹射物、法术，凡 `source.getEntity() == 持有者` 且目标不是自己、伤害量 > 0，排除 `minecraft:thorns` 和命运伤害自身）有 **50%** 概率被命运选中：在原来那次命中里直接把伤害类型换成 `fortune_spells:fortune`，数值为 **原伤害 × 2**。仍是同一次命中，只走一次原版伤害结算，不取消原伤害、也不补打第二下（规则见第 7 节）。没选中的保持原伤害和原伤害类型。
- 重复获得：取更长的剩余时间（原版 `MobEffectInstance.update` 默认就是这样）。
- 死亡后正常清除。

<a id="sec-6-2"></a>
### 6.2 被命运背弃 `cursed_by_fate`
- `MobEffectCategory.HARMFUL`，颜色 `0x6B5B7B`（背弃灰紫）。
- 效果：持续期间法力恒为 0。
  - 订阅铁魔法 `ChangeManaEvent`：持有该效果 → `setNewMana(0)`。
  - 订阅 `MobEffectEvent.Added`（服务端）：获得时立即把法力设为 0，并用 `PacketDistributor.sendToPlayer(sp, new SyncManaPacket(magicData))` 同步。
  - 铁魔法每 10 tick 的回蓝也会经过 `setMana` → 被压回 0，不用额外处理。
- **不能被净化**：加入 `data/irons_spellbooks/tags/mob_effect/cleanse_immune.json`（`{"replace":false,"values":["fortune_spells:cursed_by_fate"]}`），铁魔法的净化法术会跳过它。
- **死亡不清除**：`PlayerEvent.Clone`（`wasDeath()` 为真）时把剩余时长原样加回新玩家（「命运不会因死亡放过你」，堵住自杀洗 debuff）。
- 例外：从卷轴施法本来就不耗法力，背弃期间仍可用卷轴（铁魔法规则，不拦）。

<a id="sec-6-3"></a>
### 6.3 孤注一掷（标记）`all_in`
- `BENEFICIAL`，颜色 `0xF0506E`，无限时长，图标表示「下一击已就绪」。用户规格里没要求这个图标，加上是为了让玩家知道自己已经蓄好，不影响任何数值。
- 消耗逻辑在 `FortuneCombatHandler`（7.2）。

---

<a id="sec-7"></a>
## 7. 命运伤害类型

<a id="sec-7-1"></a>
### 7.1 数据文件（后端）

```jsonc
// data/fortune_spells/damage_type/fortune.json
{"message_id":"fortune_spells.fortune","exhaustion":0.0,"scaling":"never","effects":"hurt"}
// data/fortune_spells/damage_type/fortune_magic.json
{"message_id":"fortune_spells.fortune_magic","exhaustion":0.1,"scaling":"when_caused_by_living_non_player","effects":"hurt"}
```

`fortune_spells:fortune` 加入以下伤害类型标签（每个都 `{"replace":false,"values":["fortune_spells:fortune"]}`）：

| 标签文件 | 作用 |
|---|---|
| `data/minecraft/tags/damage_type/bypasses_armor.json` | 无视护甲与韧性 |
| `data/minecraft/tags/damage_type/bypasses_effects.json` | 无视抗性提升药水 |
| `data/minecraft/tags/damage_type/bypasses_resistance.json` | 同上（双保险） |
| `data/minecraft/tags/damage_type/bypasses_enchantments.json` | 无视保护类附魔 |
| `data/minecraft/tags/damage_type/bypasses_shield.json` | 不被盾挡（伤害类型在 `hurt()` 入口、盾判定之前就已换成命运伤害） |
| `data/minecraft/tags/damage_type/bypasses_cooldown.json` | 定义原始命运伤害类型；从 0.2.14 起，赌局祝福转换的命中单独沿用原攻击的受击保护规则。普通攻击在保护期内连打两下，等伤害的第二击为零，更强的第二击只计算差额。 |
| `data/irons_spellbooks/tags/damage_type/bypass_evasion.json` | 铁魔法「闪避」躲不掉命运 |

**不加** `bypasses_invulnerability`：创造模式、Boss 无敌阶段（含福尔图娜女爵洗牌阶段的护甲）照样免疫。

<a id="sec-7-2"></a>
### 7.2 结算流程（后端 `FortuneCombatHandler` + 7 个 Mixin，仅服务端）

`fortune_spells.mixins.json` 注册了 7 个 Mixin（包 `io.myf.fortune.mixin`）：

| Mixin | 目标 | 管什么 | 步骤 |
|---|---|---|---|
| `LivingEntityMixin` | 原版 `LivingEntity.hurt` | 非玩家目标在入口判命运、换伤害来源；用 `@Redirect` 包住两处 `actuallyHurt` 调用，按应有伤害结算 | 1、3 |
| `PlayerMixin` | 原版 `Player.hurt` | 玩家目标在入口判命运、换伤害来源 | 1 |
| `IronProjectileImpactMixin` | 铁魔法 `ServerPlayerEvents.onProjectileImpact` | 投射物碰撞时的闪避和深渊庇佑：先判命运，选中就跳过 | 2 |
| `BossProjectileAccessor` | 迎战 `ProjectileBossAbstract` | 只读 `fired` 字段，让第 2 步分辨 Boss 弹幕是还在摆位还是已经发射 | 2 |
| `FortuneEventAmountMixin` | Forge `LivingHurtEvent.setAmount`、`LivingDamageEvent.setAmount` | 命运命中只接受上调、忽略下调 | 4 |
| `ForgeDamageEventsMixin` | Forge `ForgeHooks.onLivingDamage` | 最终伤害确定后，消耗孤注一掷、发特效 | 5 |
| `IronHeartstopMixin` | 铁魔法 `ServerPlayerEvents.onBeforeDamageTaken` | 命运伤害不进止心术的延后账 | 6 |

**原则**：被命运选中的命中，**在原来那次命中里直接把伤害来源换成命运伤害**，只调用一次原版伤害结算。不取消原伤害，也不补打第二下。死亡、不死图腾、击杀归属，以及受击声和击退，都随这一次命中各结算一次。

1. **判定：`hurt()` 入口**。`LivingEntityMixin`（`LivingEntity.hurt`）和 `PlayerMixin`（`Player.hurt`）在方法开头调用 `FortuneCombatHandler.selectSource`。条件见 6.1，`random.nextFloat() < FortuneTuning.FATE_CHANCE`（0.5）时把伤害来源换成 `FSDamageTypes.fortune(level, attacker, original)`：
   - 返回 `FortuneStrike`，causing = 攻击者；direct = 原来源的 `getDirectEntity()`，弹射物保留弹射物；难度缩放沿用原来源。
   - 玩家只在 `Player.hurt` 判一次，`LivingEntityMixin` 跳过玩家，避免重复判定。
   - 换来源发生在 `LivingAttackEvent`、受击无敌帧和盾判定之前，所以后面这些检查看到的已经是命运伤害。
2. **铁魔法投射物的提前闪避：`IronProjectileImpactMixin`**。铁魔法在投射物碰撞时（`ServerPlayerEvents.onProjectileImpact`）就判「闪避」，比 `hurt()` 早。这里先对攻击者判命运：
   - 「闪避」和「深渊庇佑」两处都套了命运判定，同一枚投射物对同一目标只判一次。
   - 选中：跳过闪避（或深渊庇佑），不消耗闪避层数。没选中：照常闪避。
   - 代码按弹体类型、飞行状态和目标预判碰撞是否会造成伤害。例如，迎战 Boss 弹幕发射后才参与判定，迟缓喷溅药水照常走铁魔法闪避。这个预判对第三方纯效果箭、再次撞到同一目标的铁魔法魔法箭有例外，见第 12 节第 10、11 条。
   - 判定结果按「这枚投射物 + 这个目标」缓存，随后的 `hurt()` 直接复用，不再重判。没用上的缓存在每个服务器 tick 结束时清空。
3. **算应有伤害：包住原版 `actuallyHurt`**。`LivingEntityMixin` 用 `@Redirect` 包住 `hurt()` 里对 `actuallyHurt` 的调用，交给 `FortuneCombatHandler.apply`。此时的 `amount` 就是原版本来要结算的这一下：
   - 攻击者有 `all_in` 效果（目标≠攻击者、`amount > 0`、来源不是其它途径造成的 `fortune_spells:fortune` 伤害）→ ×2；
   - 来源是 `FortuneStrike` → 再 ×2，两者同时成立最高 ×4（5.3）；
   - 结果记作 `intended`，存进 `ThreadLocal` 里的本击记录，再用 `intended` 调用**一次**原版 `actuallyHurt`。本击记录支持嵌套命中，同一个 `all_in` 效果实例不会被嵌套命中重复预定。
4. **两个伤害事件：只保留增伤，忽略下调**。`FortuneEventAmountMixin` 拦截 `LivingHurtEvent` 和 `LivingDamageEvent` 的 `setAmount`。来源是 `FortuneStrike` 时，新值取「当前值」和「监听器想设的值」中较大的那个：
   - 其它模组的增伤照常生效。例如基础伤害为 2 点时，命运和迎战的切割者之骰各翻一倍，结果为 8 点。通过 `setAmount` 下调伤害的减伤和单次伤害上限会被忽略。
   - 每个监听器看到的是上一次被接受的值。限伤监听器即使排在增伤监听器前面，也改不了后者的基数。
   - `LivingHurtEvent` 的初始值是第 3 步的 `intended`。Hurt 阶段结束后，原版先计算护甲和魔法减伤（见 7.1 的绕过标签），再扣黄心。剩余伤害才进入 `LivingDamageEvent`，所以黄心吸收照常生效。
   - 事件被取消时，这次命中照常作废，不干预。
5. **按最终伤害消耗标记、发送特效**。`ForgeDamageEventsMixin` 只注入 `ForgeHooks.onLivingDamage`。所有监听器执行完毕、事件未被取消时，Forge 读取最终伤害，此处调用 `FortuneCombatHandler.afterDamage`。
   - `> 0`：本击预定了 `all_in`、且攻击者身上还是同一个效果实例 → 移除效果，发 FX `ALL_IN_STRIKE`，播 `FSSounds.ALL_IN_PROC`。命运命中 → 发 FX `FATE_STRIKE`，播 `FSSounds.FATE_STRIKE`。两种 FX 带的数值都是最终伤害，同一击可以两种都发。
   - `≤ 0`，或事件被取消、目标免疫：不消耗 `all_in`，不发特效。
6. **止心术的延后账**。铁魔法的 `ServerPlayerEvents.onBeforeDamageTaken` 是 `LivingDamageEvent` 的监听器，在第 5 步读取最终伤害之前执行。`IronHeartstopMixin` 让 `fortune_spells:fortune` 伤害跳过本次记账，按当前命中结算。其它伤害仍按止心术原规则记账，效果结束时照常结清已有欠账。

命运命中可以穿过护甲、保护附魔、抗性提升、盾和铁魔法闪避。通过受击判定后，它可以突破结算阶段的限伤，包括下界亚波伦在可受伤阶段的单次上限。黄心、首领阶段免疫和原攻击的受击保护继续生效。整次命中只结算一次击退、受击声和击杀归属。

> **补充（11-21）**：伤害类型在 `hurt()` 入口就已换成命运伤害，比 `LivingAttackEvent` 早。所以 Attack 阶段里**按伤害类型判断**的免疫，例如焰护之戒按火焰标签免疫，看到的是命运伤害，不会生效。上面「不无视 `LivingAttackEvent` 里取消的免疫和闪避」只指不看伤害类型的那一类，例如铁之王牌的概率免伤。见第 12 节第 9 条。

<a id="sec-7-3"></a>
### 7.3 死亡信息（前端写 lang）
- `death.attack.fortune_spells.fortune` = `%1$s 的命运走到了尽头`
- `death.attack.fortune_spells.fortune.player` = `%1$s 的命运被 %2$s 改写了`
- `death.attack.fortune_spells.fortune_magic` / `.player` 同理（文案见第 10 节）

---

<a id="sec-8"></a>
## 8. 猜牌小游戏

<a id="sec-8-1"></a>
### 8.1 总原则（防作弊）
1. **对局全部由服务端决定**：发几张、哪张是对的、洗牌顺序、计时、判定。
2. **客户端拿不到答案**：卡牌一旦翻到背面，服务端就把同步牌面设为 `HIDDEN(-1)`；「哪张是对的」只存在服务端的 `CardGameSession` 里，**永不同步**。揭晓时才把真实牌面写回。
3. 客户端只能发一种包：`C2SCardPick(sessionId, cardEntityId)`；服务端逐项校验，每局只认第一次合法点选。
4. 已知且接受的限制：命运之选目标一开始就是明牌，作弊客户端可以靠实体坐标一路跟踪（女爵原版也是这样）。这是技巧小游戏，奖励 30～70 秒，不处理；盲眼赌局完全不泄露信息。

<a id="sec-8-2"></a>
### 8.2 服务端状态机（`CardGameSession`，每个玩家最多 1 局）

阶段枚举 `CardGamePhase`（值写死在 `FortuneIds`）：`DEALING=0, SHOW_TARGET=1, SHUFFLING=2, AWAIT_PICK=3, REVEAL=4, ENDED=5`。
模式 `CardGameMode`：`FATES_CHOICE=0, BLIND_GAMBLE=1`。

**命运之选时间线**（t = 开局后 tick，20 tick = 1 秒）：

| t | 事件 | 卡牌同步数据 | 发包 |
|---|---|---|---|
| 0 | 生成 5 张牌，`ANIM=NOT_HERE`，`FACE=真实牌面`（5 种牌面随机分配，各不相同） | | `S2CCardGameStart`、`S2CCardGamePhase(DEALING, 40)` |
| 6·i（i=0..4） | 第 i 张出现：`ANIM=APPEAR`，播 `card.appear` | | |
| 6·i + 10 | `ANIM=IDLE_SHOW` | | |
| 40 | 目标牌 `ANIM=HINT`（晃 30 tick） | 目标牌 `ASK=目标牌面`（客户端用来画金光） | `Phase(SHOW_TARGET, 40)` |
| 70 | 目标牌 `ANIM=IDLE_SHOW` | | |
| 80 | 全部 `ANIM=HIDE`（10 tick 翻面） | | |
| 85 | 全部 `FACE=HIDDEN`，`ASK=NONE`（翻到一半换背面） | | |
| 90 | 全部 `ANIM=IDLE_HIDDEN`，开始洗牌：环绕 80 tick + 落位 35 tick（8.4），播 `card.shuffle` | | `Phase(SHUFFLING, 115)` |
| 205 | 全部 `ANIM=IDLE_QUESTION`，`ASK=目标牌面`（背面上浮现要找的花色，和女爵原版一样） | | `Phase(AWAIT_PICK, 200)` |
| 205～405 | 等点选；超时 → 服务端随机替玩家选一张（「命运替你选」） | | |

**盲眼赌局时间线**：

| t | 事件 | 发包 |
|---|---|---|
| 0 | 生成 N 张（1 级 5 张、≥2 级 4 张），`FACE=HIDDEN`，`ASK=NONE`；服务端随机定对的那张，并在服务端记下每张的真实牌面（对的=JOKER，其余随机不重复花色） | `Start`、`Phase(DEALING, 40)` |
| 6·i | 第 i 张出现（背面朝上 `APPEAR`） | |
| 40 | 全部 `ANIM=IDLE_QUESTION`，`ASK=UNKNOWN`（背面画「?」） | `Phase(AWAIT_PICK, 200)` |
| 40～240 | 等点选；超时同上随机代选 | |

**揭晓（两种模式共用，p = 收到合法点选或超时的那一 tick）**：

| t | 事件 |
|---|---|
| p | 立刻判定 `won`，存入 `pendingEffect`；被选牌 `ANIM=REVEAL`；其余牌 `ASK=NONE`；发 `Phase(REVEAL, 60)` |
| p+10 | 被选牌 `FACE=真实牌面`（翻到一半） |
| p+20 | 被选牌 `RESULT = PICKED_WIN / PICKED_LOSE`；**施加效果**；播 `card.right` / `card.wrong`（盲眼赌局输了再加 `fate.laugh`）；发 `S2CCardGameResult`；输了时对的那张 `ANIM=REVEAL` |
| p+30 | （输了）对的那张 `FACE=真实牌面` |
| p+40 | （输了）对的那张 `RESULT=TRUTH`；其余无关牌 `ANIM=FADE_OUT` |
| p+60 | 全部牌 `discard()`；发 `Phase(ENDED, 0, reason=FINISHED)`；会话移除 |

**中途作废**（`Phase(ENDED, 0, reason=CANCELLED)` + 清牌）：玩家死亡、下线（`PlayerLoggedOutEvent`）、换维度、离牌桌中心超过 32 格、服务器关闭、任一卡牌实体意外消失。
- 作废时如果已经判定（REVEAL 阶段），**立即补发效果**（下线事件在存档之前触发，来得及）。
- 判定前作废：无奖无罚；冷却和法力照扣（铁魔法在 `onCast` 后就已经结算了）。

**多人**：卡牌是普通实体，附近玩家都看得到（女爵原版也一样），但只有对局玩家的 `C2SCardPick` 有效（8.6 校验第 2、4 条），别人不能代猜；别人的 HUD 不显示。同一时间多人各开一局互不影响（按玩家 UUID 分局）。

**冷却**：完全走铁魔法的 `PlayerCooldowns`——按服务器 tick 计时（游戏暂停 / 玩家下线时不走），随玩家数据存盘，换维度不丢；死亡后是否保留沿用铁魔法规则，第⑤步实测后写进玩家文档。我们不另写冷却逻辑。

**管理器** `CardGameManager`：`Map<UUID, CardGameSession>`；`AtomicInteger` 递增 sessionId；在 `TickEvent.ServerTickEvent`（`Phase.END`）里逐局 tick；提供 `start / hasSession / get / cancel / onPick`。

<a id="sec-8-3"></a>
### 8.3 卡牌实体 `fortune_spells:fate_card`（`FateCardEntity extends Entity`）

- `EntityType.Builder.of(FateCardEntity::new, MobCategory.MISC).sized(1.25f, 1.75f).clientTrackingRange(8).updateInterval(1).setShouldReceiveVelocityUpdates(true)`
- `noPhysics = true`、`setNoGravity(true)`、`isPickable() = false`（不让原版准星和攻击碰它；点选走我们自己的射线）、`isAttackable() = false`、`skipAttackInteraction() = true`、`hurt() = false`、`shouldBeSaved() = false`（不存盘）、`getAddEntityPacket()` 用 `NetworkHooks.getEntitySpawningPacket(this)`。
- 服务端移动：在 `tick()` 里按 8.4 设 `deltaMovement` 再 `move(MoverType.SELF, …)`，和女爵原版一致；客户端只做插值。

**同步数据（SynchedEntityData，取值写死在 `FortuneIds`）**：

| 字段 | 类型 | 取值 |
|---|---|---|
| `FACE` | BYTE | `-1 HIDDEN`，`0 CLUB 梅花`，`1 HEART 红心`，`2 DIAMOND 方块`，`3 SPADE 黑桃`，`4 JOKER 命运女爵` |
| `ASK` | BYTE | `-1 NONE`，`0..4` = 背面上要画的目标牌面，`5 UNKNOWN`（画「?」） |
| `ANIM` | BYTE | `0 NOT_HERE`，`1 APPEAR(10t)`，`2 IDLE_SHOW`，`3 HIDE(10t)`，`4 IDLE_HIDDEN`，`5 IDLE_QUESTION`，`6 REVEAL(20t)`，`7 HINT(30t)`，`8 FADE_OUT(10t)` |
| `RESULT` | BYTE | `0 NONE`，`1 PICKED_WIN`，`2 PICKED_LOSE`，`3 TRUTH` |
| `OWNER_ID` | INT | 对局玩家的实体 id（客户端用来判断是不是自己的牌） |
| `SESSION_ID` | INT | 对局 id |

0～7 号动画编号和女爵 `FortunaCardEntity` 一致，方便对照移植。客户端动画计时字段 `public int clientAnim, animTimer;` 写在实体里，`tick()` 在客户端调用 `updateClientAnimation()`（`ANIM` 变化时把 `animTimer` 设为该动画时长，否则每 tick 减 1）——纯 int 逻辑，不引用客户端类，由后端照这一段实现，前端的渲染器只读。

<a id="sec-8-4"></a>
### 8.4 布局与洗牌运动（服务端，参数在 `FortuneTuning`）

- 朝向 `dir = Direction.fromYRot(player.getYRot())`（吸附到东南西北，和女爵原版一样保证各朝向表现一致）；`side = dir.getClockWise()`；卡牌 `yRot = dir.getOpposite().toYRot()`（牌面朝向玩家）。
- 牌桌中心 `C = (player.x, player.y + 0.25, player.z) + dir × 5.0`。
- 第 k 个槽位：`slot[k] = C + side × (k − (n−1)/2) × 1.75`，n = 牌数。
- 第 i 张牌出生在 `slot[i]`。
- 洗牌：随机排列 `perm`（服务端 `RandomSource`，Fisher–Yates），**保证至少有 2 张牌换了位置**；目标落点 `dest[i] = slot[perm[i]]`。
- 环绕：沿用女爵原版公式重写（不整段复制代码）——前 20 tick 飞到环上（限速 `(已过 tick)×0.05`），然后 30 tick 加速、30 tick 减速，角度 `0.5·x²`，总转 900°；环心 `C + (0, 2.0, 0)`；半径 **2.4**（原版 3）；第 i 张初始角 `angleOffset = (i·(360/n) + 360 − (int)dir.toYRot()) % 360`。
- 落位：35 tick；第 i 张延迟 `angleOffset/36 + 1` tick 出发，最后 10 tick 线性逼近（同原版 GOTODEST）。
- 半径、间距比原版小，是因为我们把牌缩到 0.7 倍渲染（9.3），5 张牌要能同时出现在玩家视野里。

<a id="sec-8-5"></a>
### 8.5 点选交互（客户端，前端实现；写在这里供后端对接和审查）

- 只在本地有 `AWAIT_PICK` 状态的对局时启用。
- 每个客户端 tick：从相机位置沿视线做 16 格射线，对本局卡牌的 `getBoundingBox().inflate(0.1)` 求交，取最近的一张作为「悬停牌」（渲染时上浮 0.15、放大 1.05、白色描边）。
- `InputEvent.InteractionKeyMappingTriggered`：攻击键或使用键按下且有悬停牌 → 发 `C2SCardPick`，`setCanceled(true)`、`setSwingHand(true)`；本地标记「已选」，不再重复发。没有悬停牌时不拦截按键。
- 原版近战够不着 5 格外的牌，所以必须用自定义包，而不是靠原版攻击实体。

<a id="sec-8-6"></a>
### 8.6 网络协议

通道 `fortune_spells:main`，`SimpleChannel`，协议版本 `"1"`，客户端和服务端都要求版本完全一致（两端都必须装本模组）。**包 id 按下表顺序注册**：

| id | 类 | 方向 | 字段（按编码顺序） | 发给谁 |
|---|---|---|---|---|
| 0 | `S2CCardGameStart` | 服→客 | `varInt sessionId`、`byte mode`、`byte spellLevel`、`byte cardCount`、`varInt[cardCount] cardEntityIds`（按槽位 0..n−1）、`byte targetFace`（盲眼赌局 = −1）、`varInt pickWindowTicks` | 对局玩家 |
| 1 | `S2CCardGamePhase` | 服→客 | `varInt sessionId`、`byte phase`、`varInt durationTicks`（0 = 不定）、`byte reason`（仅 ENDED：0 FINISHED、1 CANCELLED；其它阶段为 0） | 对局玩家 |
| 2 | `S2CCardGameResult` | 服→客 | `varInt sessionId`、`varInt pickedCardId`、`varInt correctCardId`、`boolean won`、`boolean timedOut`、`byte effect`（0 无、1 被命运选中、2 被命运背弃）、`varInt effectTicks` | 对局玩家 |
| 3 | `C2SCardPick` | 客→服 | `varInt sessionId`、`varInt cardEntityId` | — |
| 4 | `S2CFortuneFx` | 服→客 | `byte kind`、`varInt entityId`、`float amount` | 追踪该实体的玩家 + 自己 |

`S2CFortuneFx.kind`：`0 FATE_STRIKE`（entity = 挨打的目标）、`1 ALL_IN_STRIKE`（同上）、`2 ALL_IN_PRIMED`（entity = 施法者）、`3 BLESSED_GAINED`（玩家）、`4 FORSAKEN_GAINED`（玩家）。`amount` 是实际打出的伤害，其它 kind 为 0。

`targetFace` 发给客户端没问题：命运之选的目标本来就是明牌展示的。`correctCardId` 只在 Result 里、也就是揭晓之后才出现。

**`C2SCardPick` 服务端校验**（主线程 `enqueueWork`，任一不过就静默丢弃，只打 debug 日志）：
1. `sender != null`；
2. `CardGameManager.get(sender.getUUID())` 存在，且 `session.id == sessionId`；
3. `session.phase == AWAIT_PICK` 且尚未选过；
4. `cardEntityId` 属于本局卡牌，该实体存活、和玩家在同一维度；
5. `sender.distanceToSqr(card) ≤ 24²`。

（玩家选哪张都是合法选择，不需要校验视线角度。）

**S2C 的客户端入口**（第②步后端建空方法让编译通过；第④步起这个文件归前端）：

```java
package io.myf.fortune.client;
public final class ClientPacketHandler {
    public static void onCardGameStart(io.myf.fortune.network.S2CCardGameStart msg) {}
    public static void onCardGamePhase(io.myf.fortune.network.S2CCardGamePhase msg) {}
    public static void onCardGameResult(io.myf.fortune.network.S2CCardGameResult msg) {}
    public static void onFortuneFx(io.myf.fortune.network.S2CFortuneFx msg) {}
}
```
包类要给前端暴露字段的 public getter（或 record 风格 public final 字段），名字和上表一致。

---

<a id="sec-9"></a>
## 9. 美术规范（前端负责，第⑦步按此审）

<a id="sec-9-1"></a>
### 9.1 版权边界（硬规定）
- **迎战**：素材保留全部权利。我们**只在运行时引用**它的贴图路径（`meetyourfight:textures/entity/fortuna_card_*.png`）和声音事件，**不复制、不描摹、不打包**任何文件。所以本模组只能和迎战一起用，适合自用 / 整合包，不能单独发布素材。
- **铁魔法**：README 明确禁止使用其任何素材（个人使用也不行）。卷轴、亲和戒指、图标**全部自己画**，只模仿尺寸和构图规范，不描摹。
- 原版素材可以按原版资源路径引用（如 `minecraft:item.book.page_turn`）。

<a id="sec-9-2"></a>
### 9.2 调色板（全部素材只用这些颜色，明度可上下浮动 10%）

| 名 | 色值 | 用途 |
|---|---|---|
| 筹码红 | `#F0506E` | 学派主色、孤注一掷 |
| 深红 | `#A51E3A` | 主色暗部 |
| 天鹅绒 | `#5A0F1E` | 描边 / 最暗阴影 |
| 牌桌金 | `#F5C542` | 被命运选中、胜利、高光 |
| 暗金 | `#B8862B` | 金色暗部 |
| 象牙白 | `#FFF4E0` | 牌面底色、亮高光 |
| 牌背墨 | `#2A1A2E` | 牌背、蒙眼布 |
| 背弃灰紫 | `#6B5B7B` / 暗 `#3E3349` / 亮 `#A99BB8` | 被命运背弃、失败 |

<a id="sec-9-3"></a>
### 9.3 像素规范
- 无抗锯齿、无半透明渐变（粒子和发光层除外）；光源左上；外轮廓 1px `#5A0F1E`（灰紫系素材用 `#3E3349`）；单张图标不超过 8 色。
- 卡牌渲染缩放 **0.7 倍**（模型 28×40 像素 → 约 1.23×1.75 格），渲染朝向和翻转沿用女爵原版渲染器的变换（`scale(-1,-1,1)`、`translate(0,-1.5,0)` 再乘 0.7）；满亮度（`getBlockLightLevel` 返回 15）。

<a id="sec-9-4"></a>
### 9.4 贴图清单

| 文件（`assets/fortune_spells/textures/…`） | 尺寸 | 内容 |
|---|---|---|
| `gui/spell_icons/card_selection.png` | 16×16 | 三张扇形展开的牌，中间一张正面朝上带金色红心，右上一颗金星 |
| `gui/spell_icons/blind_gamble.png` | 16×16 | 两张背面牌，牌背墨色蒙眼布横过，中央金色「?」 |
| `gui/spell_icons/all_in.png` | 16×16 | 一摞红金筹码被推倒，前方一枚金筹码带「×2」反光 |
| `mob_effect/blessed_by_fortune.png` | 18×18 | 金色四角星压在一张牌上 |
| `mob_effect/cursed_by_fate.png` | 18×18 | 裂开的灰紫色牌 |
| `mob_effect/all_in.png` | 18×18 | 一枚红色筹码，中间是「2」 |
| `item/scroll_fortune.png` | 16×16 | 自绘卷轴：象牙白纸身、筹码红封蜡、金色牌纹 |
| `item/affinity_ring_fortune.png` | 16×16 | 自绘金戒指，戒面是一枚红色小筹码 |
| `entity/fate_card/joker.png` | 64×64 | 第 5 种牌面「命运女爵」：象牙白底，金框，中央是女爵的骰子头剪影（红白），四角是小筹码 |
| `entity/fate_card/ask_joker.png` | 64×64 | 背面浮现的 Joker 标记（发光层） |
| `entity/fate_card/ask_unknown.png` | 64×64 | 背面浮现的金色「?」（发光层） |
| `particle/fate_spark_0..3.png` | 8×8 | 金色十字闪烁 4 帧 |
| `particle/card_shard_0..3.png` | 8×8 | 红 / 白小纸片 4 种 |
| `particle/forsaken_mote_0..2.png` | 8×8 | 灰紫下坠微尘 3 帧 |

发光边框和倒计时条由代码绘制；贴图由 astra 调用生图模型制作，opus 审核。

**卡牌 64×64 的 UV 布局**（必须和模型 `addBox(-14,-40,-0.5, 28,40,1)`、`texOffs(0,0)` 对上）：正面区域 x 1–28、y 1–40；背面区域 x 30–57、y 1–40；左右侧面 1px 宽分别在 x=0 和 x=29；顶面和底面在 y=0 一行。我们的模型层 `fortune_spells:fate_card` 自己定义同样的几何（只是一个盒子，不是素材），**不依赖迎战的客户端类**。

迎战现成贴图的引用表（客户端常量，牌面编号对应 `FACE`）：
- 牌面 0..3：`meetyourfight:textures/entity/fortuna_card_{club,heart,diamond,spade}.png`；牌面 4：`fortune_spells:textures/entity/fate_card/joker.png`
- 牌背：`meetyourfight:textures/entity/fortuna_card_hidden.png`
- 背面目标标记 0..3：`meetyourfight:textures/entity/fortuna_card_ask_{club,heart,diamond,spade}.png`（`entityTranslucentEmissive`）

<a id="sec-9-5"></a>
### 9.5 特效时序（每次爆发 ≤ 60 粒子；不在渲染帧里分配对象）

> **11-21 起**：标题里的「每次爆发 ≤ 60 粒子」和「不在渲染帧里分配对象」改为**尽量做到，不作硬性验收**。这两条是设计时自加的约束，不是用户需求。

| 时机 | 表现 |
|---|---|
| 吟唱中（3 秒） | 施法者双手间每 tick 1 个 `fate_spark`，绕半径 0.8 的圈；铁魔法默认的长吟唱动作 |
| 发牌 | 每张牌出现时 6 个 `card_shard` + `card.appear` |
| 提示目标 | 目标牌 `glow_target` 每秒呼吸一次（透明度 0.4↔1.0）+ 女爵原版的左右晃动 |
| 洗牌 | 每张牌每 2 tick 在身后留 1 个 `fate_spark` 拖尾 |
| 等待点选 | 背面浮现目标标记（`ASK`）；悬停牌白描边、上浮 0.15、放大 1.05；HUD 倒计时条 |
| 猜中 | 被选牌 `glow_win`，30 个 `fate_spark` 向外爆开 + 20 个 `card_shard` 上抛；玩家脚下一圈 16 个金色 spark |
| 猜错 | 被选牌 `glow_lose`，15 个 `forsaken_mote` 下坠；对的那张翻出来时套 `glow_target` |
| 淡出 | `FADE_OUT` 10 tick 内透明度 1→0，同时缩小到 0.8 |
| 被命运选中触发（`FATE_STRIKE`） | 目标胸口一道金色斜斩（8 个 spark 连成一线）+ 12 个 spark 散开 + 6 个 `card_shard`；`fate.strike` 音高 1.2 |
| 孤注一掷蓄好（`ALL_IN_PRIMED`） | 8 个红色 `card_shard` 绕施法者转一圈（10 tick）；之后本地玩家每 10 tick 在主手冒 1 个红色 spark，直到消耗 |
| 孤注一掷命中（`ALL_IN_STRIKE`） | 目标处红色爆开 20 个 `card_shard`；`all_in.proc` 音高 0.8 |
| 获得被命运选中 | 玩家头顶金色星环 16 个 spark |
| 获得被命运背弃 / 背弃期间 | 获得时 20 个 `forsaken_mote` 从头顶落下；之后本地玩家每 20 tick 1 个 |
| 他人视角 | 依赖原版药水旋涡（颜色 = 效果颜色）和 `S2CFortuneFx`；不额外做同步 |

<a id="sec-9-6"></a>
### 9.6 HUD（`CardGameHud`，只给对局玩家看）
- 位置：屏幕水平居中、距顶部 25% 高度；文字带阴影；倒计时条在文字下方 4px。
- 倒计时条：代码绘制：1px 天鹅绒色描边，牌背墨色底，填充色从金色渐变到筹码红。
- 各阶段文案（lang 见第 10 节）：DEALING → 空；SHOW_TARGET →「记住这张牌」+ 目标花色的小图；SHUFFLING →「洗牌中……」；AWAIT_PICK →「选一张牌」+ 第二行灰字提示「准星对准卡牌，按攻击键或使用键」+ 倒计时条（剩 3 秒时条变红并闪烁）；REVEAL → 胜「命运垂青！」（金）/ 负「命运背弃了你……」（灰紫）/ 超时代选时前面加一行「命运替你选了一张」；CANCELLED →「牌局作废」（灰，1.5 秒后消失）。
- 胜负除颜色外还有不同文字和声音，色弱玩家也能分清。

<a id="sec-9-7"></a>
### 9.7 前端拍板的实现细节（opus 11-21，代码由 astra 写）

**客户端类分工**
- `CardPickInput` 不单独建类：射线、悬停和点选并进 `ClientCardGame`（`tick()` 算悬停，`tryPick()` 发包）。2.1 节表里的 `CardPickInput.java` 作废。
- `ClientPacketHandler` 只转发：`onCardGameStart/Phase/Result` → `ClientCardGame.onStart/onPhase/onResult`，`onFortuneFx` → `FortuneFx.onFx`，不写别的逻辑。
- `FSClientSetup`（MOD 总线，`Dist.CLIENT`）：`RegisterRenderers` 注册 `FateCardRenderer`；`RegisterLayerDefinitions` 注册 `FateCardModel.LAYER`；`RegisterParticleProvidersEvent.registerSpriteSet` 注册 3 个 Provider；`RegisterGuiOverlaysEvent.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "card_game", CardGameHud.INSTANCE)`。
- `FSClientEvents`（FORGE 总线，`Dist.CLIENT`）：`ClientTickEvent`（只在 `Phase.END`）依次调用 `ClientCardGame.tick()` 和 `FortuneFx.tick()`；`InputEvent.InteractionKeyMappingTriggered` 在 `isAttack() || isUseItem()` 且 `ClientCardGame.tryPick()` 为真时 `setCanceled(true)` + `setSwingHand(true)`；`ClientPlayerNetworkEvent.LoggingOut` 调用 `ClientCardGame.reset()` 和 `FortuneFx.reset()`。
- 射线起点用 `mc.getCameraEntity()` 的 `getEyePosition(partialTick)` 和视线，和原版准星取实体的方式一致。
- 服务端拒收了点选（例如超过 24 格）时，客户端保持「已选」不再重发，最后由服务端超时代选。这是有意的，防止连点刷包。

**渲染**
- 卡牌变换：先整体 ×0.7（悬停时再 ×1.05、上移 0.15），再 `scale(-1,-1,1)`、`translate(0,-1.5,0)`。按模型尺寸换算，常态牌高 1.75 格（40 ÷ 16 × 0.7），与碰撞箱同高。牌底对齐和悬停抬升的屏内表现待进游戏实测。
- 翻面（HIDE / REVEAL）：动画时长内绕 Y 轴转一整圈（360°）。服务端在动画过半时换牌面，转完正面朝向玩家；HINT 用 ±30° 正弦摆动 1.5 个来回。
- 牌面 JOKER：先画迎战牌背，再用 `joker.png` 按同一几何叠一层 cutout。
- 背面标记（ASK）：只在 `IDLE_QUESTION` 且牌面为 HIDDEN 时，用 `entityTranslucentEmissive` 叠画。
- 发光边框代码绘制，用**普通透明混合**（无贴图、不受光照的顶点色，当前实现是 `RenderType.gui()`）正反各画一次。不用 `RenderType.lightning()` 的加法混合：在亮天空背景下，加法混合会把灰紫失败边框冲成白色，红、金边框也会发虚，换成普通透明混合是牺牲一点发光感换颜色清楚（opus 11-21 认可）。框宽 0.1（部件坐标），优先级：结果 > 悬停/已选 > 提示。颜色：猜中金色呼吸 0.75↔1.0；猜错亮灰紫 0.8 常亮；揭晓真牌和提示目标都用金色呼吸，周期 20 tick（真牌 0.4↔0.8，提示 0.4↔1.0）；悬停/已选用象牙白 0.85。
- 渲染帧里尽量不 new（11-21 起不作硬性验收，见 9.5）：边框两面的 z 值用 `static final float[]`，RenderType 用工厂的缓存实例。

**特效（对 9.5 的调整）**
- 孤注一掷蓄好（ALL_IN_PRIMED）：改成 10 tick 内每 tick 2 个红色 `fate_spark`，绕腰部转一圈，共 20 个。原定的 8 个 `card_shard` 带重力会直接掉下去，围不成圈。
- 孤注一掷命中：除了 20 个 `card_shard`，再加 10 个红色 spark，合计 30 个，在 60 以内（60 的上限 11-21 起不作硬性验收，见 9.5）。
- 所有爆发都按「粒子」画质设置缩减：全部 ×1、减少 ×½、最少 ×¼，至少留 1 个。最少档不画洗牌拖尾。
- 吟唱金光：读铁魔法 `ClientMagicData.getSyncedSpellData(p)`，只对 48 格内正在吟唱命运之选或盲眼赌局的玩家画。
- 声音全部由服务端播，客户端不播任何音效。

**HUD**
- SHOW_TARGET 的「目标花色小图」改用字体字形 `♣ ♥ ♦ ♠ ★` 显示：红心、方块用筹码红，梅花、黑桃用象牙白，Joker 用金色。这样不另做贴图，缩放 GUI 也不会糊。
- 倒计时条宽 120、高 5 像素，顶上 1px 白色高光（透明度 0x60）；剩 60 tick 时整条变成筹码红，每 4 tick 闪一次。
- 揭晓文字在 ENDED 后再留 30 tick；作废提示留 30 tick（1.5 秒），颜色 `#AAAAAA`。

**给贴图制作（第③步）的硬要求**
- `joker.png`：只画正面 UV 区域（x 1–28、y 1–40），其余全部透明，因为它叠在迎战牌背上画。
- `ask_joker.png`、`ask_unknown.png`：只画背面 UV 区域（x 30–57、y 1–40），其余透明。允许半透明发光。
- `fate_spark_*`、`forsaken_mote_*`：主体用**浅中性灰到白**，代码会染色（金 / 红 / 灰紫），所以不要画成金色或紫色。用中性色是为了染色后不偏色，带暖色调的象牙白乘上去会发黄。现有贴图已经是这样，不用返工。
- `card_shard_0..3`：自带颜色、不染色，4 张的分配是 2 张筹码红系、1 张象牙白、1 张牌桌金。
- 法术图标走铁魔法约定路径 `textures/gui/spell_icons/<法术名>.png`，效果图标走原版路径 `textures/mob_effect/<id>.png`，都不需要模型 json。

---

<a id="sec-10"></a>
## 10. 语言键清单（前端写 `zh_cn.json` 与 `en_us.json`）

```
school.fortune_spells.fortune                              命运 / Fortune
spell.fortune_spells.card_selection                        命运之选 / Fate's Choice
spell.fortune_spells.card_selection.guide                  在面前发出五张牌并告诉你目标，洗牌后猜中它即可被命运选中。
spell.fortune_spells.blind_gamble                          盲眼赌局 / Blind Gamble
spell.fortune_spells.blind_gamble.guide                    不看牌，直接押注。猜中则长久被命运选中；猜错则被命运背弃，法力枯竭。
spell.fortune_spells.all_in                                孤注一掷 / All In
spell.fortune_spells.all_in.guide                          把一切押在下一击上：下一次攻击伤害翻倍。
effect.fortune_spells.blessed_by_fortune                   被命运选中 / Fortune's Chosen
effect.fortune_spells.blessed_by_fortune.description       攻击有 50% 概率造成双倍命运伤害，无视护甲、防御与伤害上限。
effect.fortune_spells.cursed_by_fate                       被命运背弃 / Forsaken by Fate
effect.fortune_spells.cursed_by_fate.description           法力值始终为 0。死亡与净化都无法摆脱。
effect.fortune_spells.all_in                               孤注一掷 / All In
effect.fortune_spells.all_in.description                   下一次攻击伤害翻倍。
attribute.fortune_spells.fortune_spell_power               命运法术强度
attribute.fortune_spells.fortune_magic_resist              命运魔法抗性
entity.fortune_spells.fate_card                            命运之牌
death.attack.fortune_spells.fortune                        %1$s 的命运走到了尽头
death.attack.fortune_spells.fortune.player                 %1$s 的命运被 %2$s 改写了
death.attack.fortune_spells.fortune_magic                  %1$s 被命运魔法吞没
death.attack.fortune_spells.fortune_magic.player           %1$s 被 %2$s 的命运魔法吞没
ui.fortune_spells.chosen_duration                          被命运选中：%s 秒
ui.fortune_spells.card_count                               牌数：%s
ui.fortune_spells.blind_win                                胜：被命运选中 %s 分钟
ui.fortune_spells.blind_lose                               负：被命运背弃 %s 分钟
ui.fortune_spells.next_hit_double                          下一次攻击伤害 ×2
ui.fortune_spells.cast_error_in_game                       你还在一场牌局里
hud.fortune_spells.card_game.remember                      记住这张牌
hud.fortune_spells.card_game.shuffling                     洗牌中……
hud.fortune_spells.card_game.pick                          选一张牌
hud.fortune_spells.card_game.pick_hint                     准星对准卡牌，按攻击键或使用键
hud.fortune_spells.card_game.win                           命运垂青！
hud.fortune_spells.card_game.lose                          命运背弃了你……
hud.fortune_spells.card_game.timeout                       命运替你选了一张
hud.fortune_spells.card_game.cancelled                     牌局作废
subtitles.fortune_spells.<每个 SoundEvent 去掉点号前缀后>     （每个声音一条字幕）
```

---

<a id="sec-11"></a>
## 11. 构建、运行与验收

<a id="sec-11-1"></a>
### 11.1 构建与运行（Windows）

> 完整步骤已写进 [`README.md`](../README.md)。第②步后端实测时如果发现出入，先改本节，再同步改 README。
1. 装 JDK 17，`JAVA_HOME` 指向它。
2. 工程根目录：`gradlew.bat build` → 产物 `build/libs/fortune_spells-0.1.0.jar`。
3. `gradlew.bat runClient` 启动开发客户端（首次会下载依赖并反编译 MC，需要几分钟）。
4. 装进整合包：把产物 jar 和铁魔法 1.20.1-3.16.4、Iron's Lib、迎战 1.20.1-1.6.2、Curios、GeckoLib、Player Animator 放进 `mods/`。

<a id="sec-11-2"></a>
### 11.2 游戏内验收清单（第⑤步逐条打勾）
1. 生存世界用指令拿材料：`/give @s irons_spellbooks:scroll_forge`、`irons_spellbooks:arcane_anvil`、`irons_spellbooks:epic_ink 4`、`irons_spellbooks:legendary_ink 8`、`minecraft:paper 8`、`meetyourfight:fortunes_favor 8`。
2. 卷轴合成台：史诗墨水 + 纸 + 命运之眷 → 列表里有 3 个命运法术，只有命运之选可选，出 1 级卷轴；换传说墨水 → 三个都可选，命运之选出 2 级。
3. 奥术铁砧：1 级命运之选 + 传说墨水 → 2 级 → 3 级；盲眼赌局 1→2；孤注一掷不能升。
4. 卷轴和三个法术图标显示正常，学派名为红色「命运」，日志里没有缺模型 / 缺贴图报错。
5. 用法术书施放命运之选：吟唱 3 秒 → 发牌、提示、洗牌、点选、揭晓全流程；猜中得 buff，时长 30 / 50 / 70 秒；冷却 3 分钟。
6. 被命运选中期间打僵尸：约一半攻击出金色特效，伤害为双倍且不受护甲影响（给僵尸穿钻石甲对比）；死亡信息是命运伤害。
7. 盲眼赌局：1 级 5 张、2 级 4 张；赢了 15 分钟 buff；输了 30 分钟 debuff、法力条一直是 0、喝牛奶不掉、死了也不掉；冷却 1 小时。
8. 孤注一掷：瞬发，出现图标；下一击伤害翻倍后图标消失；冷却 5 秒。
9. 对局中下线 / 死亡 / 走远 32 格以上 → 牌消失、HUD 提示牌局作废，日志无异常。
10. 专用服务器（`gradlew.bat runServer`）能启动不崩（验证没有跨端引用客户端类）。

---

<a id="sec-12"></a>
## 12. 已知限制 / 风险

1. 盲眼赌局的命运命中仍受原攻击的无敌判定约束。迎战「铁之王牌」的概率免伤仍有机会挡下这次攻击，首领的阶段免疫也继续生效。通过判定后，命运命中按第 7.2 节突破限伤。
2. 命运之选可被作弊客户端靠坐标跟踪自动解（8.1）；盲眼赌局不受影响。
3. 被命运选中会把铁魔法法术伤害也转成命运伤害，部分法术转换后的那一下会丢掉自带的附加效果。第⑤步实测（`b0f60e3`）：火焰箭的点燃、冰锥的冻结会丢；地狱浮现的点燃、地震的迟缓保留；火球术普通命中本来就没点燃，无法判断。只记录，不修。
4. 命运伤害靠 7 个 Mixin 接入原版、Forge、铁魔法和迎战（7.2 表）。兼容风险：
   - `LivingEntityMixin` 用 `@Redirect` 包住 `LivingEntity.hurt()` 里的两处 `actuallyHurt` 调用，另一个模组如果也 Redirect 同一调用，可能在启动时冲突报错。
   - `FortuneEventAmountMixin` 改了 Forge 事件类 `LivingHurtEvent` / `LivingDamageEvent` 的 `setAmount`，别的模组如果也改这个方法，可能冲突。
   - `ForgeDamageEventsMixin` 依赖 Forge 47.x 里 `ForgeHooks.onLivingDamage` 的写法，升级 Forge 时要重新核对。
   - `IronProjectileImpactMixin` 依赖铁魔法 3.16.4 的 `ServerPlayerEvents.onProjectileImpact`，`IronHeartstopMixin` 依赖同版本的 `ServerPlayerEvents.onBeforeDamageTaken`，升级铁魔法时两处都要重新核对。
   - `BossProjectileAccessor` 读迎战 `ProjectileBossAbstract` 的 `fired` 字段，升级迎战时要重新核对。
   - 和其它模组同装的实际兼容性留到第⑤步实测。（旧版的「两次受击声和两次击退」已不存在，现在触发时只结算一次原版命中。）
5. 依赖的仓库坐标（尤其 Modrinth maven 上的迎战）第②步如果解析失败，换 cursemaven 并在频道报。
6. 只适合自用 / 整合包：运行时引用了迎战的贴图和声音，不能单独发布素材。
7. 铁魔法的魔法飞弹没触发命运时，同一枚飞弹可能一次吃掉两层闪避，闪避后还会继续命中。这是铁魔法 3.16.4 自身的问题：它的飞弹碰撞入口只看事件有没有被取消，忽略了闪避写入的「跳过实体」。不装本模组也能复现，按现状接受（opus 11-21 定）。
8. 其它模组如果在 Hurt 阶段把伤害转给另一个目标、再取消原命中，孤注一掷可能不生效：转出去的那一下不翻倍，标记也不消耗。正常游戏碰不到，按现状接受（opus 11-21 定）。
9. 命运伤害无视焰护之戒这类按伤害类型免疫的效果。例如戴焰护之戒的玩家被火球打中时本来免疫，如果攻击者有「被命运选中」且触发了命运，就会按双倍扣血。这是 7.2「无视防御」的设计结果：伤害类型在 Attack 阶段之前就已换成命运伤害，按现状接受（opus 11-21 定）。
10. 其它模组里不造成伤害、只施加效果的箭，射手带「被命运选中」时，命运判定成功会让这支箭绕过铁魔法闪避，把效果打到目标身上。修法只能是对不认识的投射物一律不提前判定，会把「有伤害的第三方箭先判命运」一起改坏，按现状接受（opus 11-21 定）。
11. 同一支铁魔法魔法箭再次碰到已经命中过的目标时，射手身上的「被命运选中」会多做一次命运判定，但不造成伤害，也不消耗闪避。多留的判定缓存在当 tick 结束时清掉，玩家感觉不到，按现状接受（opus 11-21 定）。

---

<a id="round2"></a>
<a id="sec-13"></a>
## 13. 第二轮：钟鸣、腐沼、蔷薇三学派与超位法术（0.2.0～0.2.6）

| 项 | 内容 |
|---|---|
| 依据 | 用户需求 [`需求记录-第二轮.md`](需求记录-第二轮.md)；opus 规划 `build/reports/plan-v2/规划.md`；数值以实现和验收为准 |
| 实现 | 0.2.0 `a0f654e` → 0.2.1 `c040c47` → 0.2.2 `2c3ba5e` → 0.2.3 `7834810` → 0.2.4 `ba01c9c` → 0.2.5 `0cdee88` → **0.2.6 `b87fc32`** |
| 验收 | [第二轮初版](../build/reports/expansion/验收记录.md) · [0.2.2 命中与卷轴](../build/reports/spells-022-release/验收.md) · [0.2.3 剑舞与骨翼美术](../build/reports/spells-023-art/美术验收.md) · [0.2.4 施法手与纯骨翼](../build/reports/spells-024/验收.md) · [0.2.5 释放法阵与持续吞噬](../build/reports/ultra-release-review/验收.md) · [0.2.6 吟唱法阵与鳄颅](../build/reports/grand-chant/验收.md) |
| 原则 | **用户没说到的地方，尽可能遵循铁魔法原版机制**（用户 11-22）。本节里标「建议值」的都是用户没说、opus 定的数 |

本节中的「法强」为铁魔法通用法术强度乘以学派强度（`getSpellPower(level, caster)`，裸装为 1.0），「法力上限」取 `irons_spellbooks:max_mana`。表列吟唱与冷却为默认值，通常受施法速度与冷却缩减影响。落日剑舞固定吟唱 160 tick。

<a id="sec-13-1"></a>
### 13.1 三个新学派

和命运学派同一做法（第 4 节）：学派物品就是对应 Boss 的掉落物，本模组不新加物品。注册在 `registry/BossSchools.java`。

| 学派 id | 中文 | Boss | 学派物品（焦点标签 `fortune_spells:<学派>_focus`） | 名字颜色 | 伤害类型 | 属性 |
|---|---|---|---|---|---|---|
| `bell` | 钟鸣 | 敲钟人 | 幽魂质 `meetyourfight:phantoplasm` | `#7FE3D8` | `fortune_spells:bell_magic` | `bell_spell_power` / `bell_magic_resist` |
| `swamp` | 腐沼 | 沼泽巨鳄怪 | 藓牙 `meetyourfight:mossy_tooth` | `#B7D45A` | `fortune_spells:swamp_magic` | `swamp_spell_power` / `swamp_magic_resist` |
| `rose` | 蔷薇 | 罗莎琳 | 暮光紫罗兰 `meetyourfight:violet_bloom` | `#D7263D` | `fortune_spells:rose_magic` | `rose_spell_power` / `rose_magic_resist` |

- modid 仍是 `fortune_spells`，和命运学派同一个 jar（改 modid 会让已有存档里的命运法术失效）。已装 0.1.0 的实例直接替换 jar。
- 每个学派有自己的亲和戒指贴图（`affinity_ring_<学派>`）。
- **卷轴外观**（用户 11-22 要求，0.2.2 起）：钟鸣、腐沼、蔷薇的卷轴用铁魔法原版卷轴材质（`models/item/scroll_<学派>.json` 只写 `"parent": "irons_spellbooks:item/scroll"`），旧存档里的卷轴也一样显示；只有命运卷轴保持专属外观。0.2.0 / 0.2.1 的三张学派卷轴贴图还在 jar 里，但已没有模型引用。

<a id="sec-13-2"></a>
### 13.2 超位等级：备用方案

**结论**：没有给铁魔法真的加上第 6 档稀有度，用的是 opus 规划里的备用方案。用户的要求是「先尝试加稀有度，加不了就按备用方案」。

**加不了的原因**：铁魔法 3.16.4 的稀有度 `SpellRarity` 是写死 5 档的 Java 枚举，没有提供 Forge 的可扩展枚举接口；它的稀有度配置也限定只能有 5 项。硬加第 6 档会碰到按 5 档写死的 `switch`、配置读取和按等级分配稀有度的算法。astra 的探针记录：`build/reports/expansion-crafting/native-tier-probe.log`。

**备用方案怎么做**（`expansion/crafting/ExpansionCrafting.java` 加若干 Mixin）：
- 四个超位法术（亡鳄吞噬、幽钟齐鸣、落日剑舞、骨翼）在铁魔法内部登记为 **LEGENDARY、只有 1 级**。
- 新物品 **超位墨水** `fortune_spells:transcendent_ink`（内部也是传说墨水类型，放在创造模式「原材料」页）。
- 卷轴合成台、奥术铁砧、JEI 共用同一个墨水判定：**超位法术只认超位墨水，超位墨水只能做超位法术**，普通传说墨水和超位墨水不能互换（`BossScrollForgeMixin`、`TranscendentAnvilMixin`、`BossJeiRecipesMixin`）。
- 卷轴和法术书提示里，稀有度那一行显示为金色「超位」`#F5C542`，替换「传说」（`TranscendentTooltipMixin`）。
- 超位法术名字加前缀：中文「超位·」，英文 `Transcendent: `。
- 超位法术不进铁魔法的随机战利品卷轴。

**超位墨水配方**（铁魔法炼金釜，和原版墨水升级同一种数据格式）：
- 釜里 1000 mB 传说墨水（即 4 瓶）+ 投入 1 个 Boss 掉落物 → 250 mB 超位墨水（即 1 瓶）（`recipes/alchemist_cauldron/brew_transcendent_ink.json`）。再用玻璃瓶装出（`fill_transcendent_ink.json`），也能倒回釜里（`empty_transcendent_ink.json`）。
- 可用的掉落物（标签 `fortune_spells:boss_ink_catalysts`）：幽魂质、藓牙、暮光紫罗兰、命运之眷，四选一。用户说的「迎战四大 Boss」就是敲钟人、沼泽巨鳄怪、罗莎琳和福尔图娜女爵。

四个超位法术的基础法力消耗均为 1000。铁魔法玩家默认法力上限为 100，可通过装备和属性提升。消耗与冷却取决于施放来源。
- **从法术书施放**，在默认属性下，999 点当前法力不能起手，1000 点可施放并扣费一次，默认冷却 5 分钟。亡鳄吞噬、幽钟齐鸣和骨翼在成功释放时结算。从 0.2.4 起，落日剑舞在第一刀起手时扣费并开始冷却，之后被打断仍会保留这次消耗。
- **直接用卷轴施放**，即使零法力或法术书正在冷却也可使用。成功施放消耗一张，不扣法力、不加冷却。亡鳄、幽钟和骨翼在吟唱完成之前取消会保留卷轴。从 0.2.4 起，落日剑舞起手即攻击，开始后即使取消也消耗一张。已有骨翼时仍不能重复施放骨翼。
- 0.2.0 / 0.2.1 里超位卷轴也要 1000 法力、扣 1000、进冷却的做法已废弃。
- 伤害公式里用的是**法力上限**，和施法时剩多少法力、扣不扣 1000 都无关。

亡鳄全程五次咬合的基础总伤害、幽钟终鸣和剑舞终爆的伤害各为 `法力上限 × 法强`。幽钟前八拍、剑舞的普通斩击与花瓣各有自己的较低伤害。亡鳄吸血按施法者生命上限计算，见 13.4。

<a id="sec-13-3"></a>
### 13.3 总表

注册在 `expansion/BossSpell.java` 的 `Kind` 枚举。法术 ID 一律是 `fortune_spells:<id>`。

| 法术 | id | 学派 | 等级 | 墨水 → 起始等级 | 吟唱 | 冷却 | 法力 |
|---|---|---|---|---|---|---|---|
| 虚化 | `phase_shift` | 钟鸣 | 4 | 史诗 → 1；传说 → 3 | 瞬发 | 60 / 70 / 80 / 90 秒 | 50 |
| 地遁 | `burrow` | 腐沼 | 6 | 罕见 → 1；稀有 → 3；史诗 → 5；传说 → 6 | 2 秒 | 30 秒 | 30 |
| 金钟冲击 | `bell_shockwave` | 钟鸣 | 5 | 稀有 → 1；史诗 → 3；传说 → 5 | 2 秒 | 10 / 12 / 14 / 16 / 18 秒 | 25 / 30 / 35 / 40 / 45 |
| 蔷薇怨灵 | `rose_wraiths` | 蔷薇 | 1 | 传说 → 1 | 3 秒 | 2 分钟 | 100 |
| 超位·亡鳄吞噬 | `croc_devour` | 腐沼 | 1 | 超位 → 1 | 10 秒 | 5 分钟 | 1000 |
| 超位·幽钟齐鸣 | `phantom_belfry` | 钟鸣 | 1 | 超位 → 1 | 10 秒 | 5 分钟 | 1000 |
| 超位·落日剑舞 | `sunset_blade_dance` | 蔷薇 | 1 | 超位 → 1 | 起手即斩，持续吟唱 8 秒 | 5 分钟 | 1000 |
| 超位·骨翼 | `bone_wings` | 腐沼 | 1 | 超位 → 1 | 5 秒 | 5 分钟 | 1000 |

- 每级对应的稀有度（`ExpansionCrafting.LEVEL_TIERS`）：虚化 史诗、史诗、传说、传说；地遁 罕见、罕见、稀有、稀有、史诗、传说；金钟冲击 稀有、稀有、史诗、史诗、传说。卷轴合成台里墨水决定起始等级（该稀有度的第一级），奥术铁砧升级沿用铁魔法规则，墨水要等于下一级对应的稀有度。
- 冷却公式为 `基础冷却 + 每级增量 × (等级 − 1)`（`BossSpell.cooldownFor`）。虚化、地遁、金钟冲击、蔷薇怨灵的法力以及骨翼的 5 分钟冷却沿用 opus 的建议值。

<a id="sec-13-4"></a>
### 13.4 八个法术的行为

以下按 0.2.6 记录。三种攻击型超位法术沿用 0.2.2 扩大的水平范围，纵向均为上下各 8 格。骨翼只作用于施法者。目标资格见 13.6。

**虚化 `phase_shift`**（`expansion/movement/MovementSpells.java`、`MovementCollisionMixin`、`MovementPacketMixin`）
用户 11-22 要求：「就是进入穿墙的状态，穿透所有方块，剩余的不用多管。」0.2.2 按这句话简化，之前 opus 加的悬浮、潜行下沉 / 跳跃上浮、基岩和屏障拦截、世界边界拦截、到期找落点和传送回施法点全部删掉。
- 获得「虚化」效果 5 / 10 / 15 / 20 秒（100 × 等级 tick），冷却、法力不变。
- 期间**穿透所有方块**：移动时跳过全部碰撞（`MovementCollisionMixin`）。用原版重力，效果本身不改移速。
- 在方块里不窒息；客户端不会把人「推出方块」，也不显示方块内的视野遮罩；服务端单个移动包不超过 12 格就不判撞墙。
- 到期直接结束，不移动玩家。如果这时还在方块里，就按原版规则处理（会窒息或被推出）。
- 实测中，玩家松开移动键后保持 X、Z 穿过地板，Y 从 210 降到 184.92。六个方向均能穿过基岩和屏障，相同腾空条件下，横穿厚墙与原版对照的前 40 tick 水平位置逐帧一致。靠近世界底部施法时可能掉进虚空，详见 [13.8 第 14 条](#sec-13-8)。[虚化专项](../build/reports/phase-022/实测.md)

**地遁 `burrow`**（`MovementSpells.java`）
- 沉入地下 1.5 格，持续 4 秒，**不随等级变**。移速 +50%（六个等级一样），不能上下移动，可以水平穿过方块。
- 期间不能攻击（近战和以自己为来源的伤害都取消）。64 格内以玩家为目标的怪物丢失目标，期间不会被选为新目标。
- 结束时破土而出（上浮 1.5 格或找附近空位），半径 4 格内目标受冲击：`20 × 法强 × (50% + 5% × (等级 − 1))`，裸装 1 级 10 点、6 级 15 点（实测）。用户只给了「法强的 50%，每级 +5%」，基数 20 是 opus 定的。

**金钟冲击 `bell_shockwave`**（`expansion/offense/SpellFigure.java`）
- 从眼睛位置发出金色声环，每 tick 1.6 格、飞 20 tick，共 32 格。
- **穿墙、穿透目标**，每个目标只打一次：`(8 + 2 × (等级 − 1)) × 法强`，1 级 8 点、5 级 16 点（实测）。用户说的「中等伤害」就是这个公式，opus 定。

**蔷薇怨灵 `rose_wraiths`**（`expansion/rose/RoseGuard.java`、`RoseWraith.java`）
- **召唤**。每个预定出现位置先用 12 个游戏刻（通常为 0.6 秒）绘出五芒星，再显现一只友方召唤物。每次共 1～2 只。客户端复用迎战的 `RoseSpiritRenderer`，使用罗莎琳召唤物的模型、材质和发光层。
- **守卫**。怨灵用锁链连接施法者，在其身旁悬浮。防御架势持续 3 秒；即使目标贴身，怨灵也要守到架势结束。有效近战命中会提前结束架势：此次减伤 80%，对攻击者反击 `15 × 法强` 并眩晕 1 秒。远程命中不会触发这次格挡。
- **出击**。架势自然到期或因格挡而结束后，怨灵才在施法者周围 16 格内搜寻目标。它们会避开施法者与其他友方怨灵。每只怨灵近身后自爆一次，半径 3 格，每个命中目标的基础伤害为 `10 × 施法时的法强`。法强为 1 和 2 时分别为 10 和 20 点，最终掉血还受目标自身减免影响。显形后最多存在 10 秒，距主人超过 32 格会消失。

**超位·亡鳄吞噬 `croc_devour`**（`SpellFigure.java`、`OffensiveControl.java`）
- 吟唱 10 秒，法阵按进度逐层展开。吟唱完成时前方升起巨型亡灵鳄颅。0.2.6 将鳄颅模型整体放大至原来的 3.25 倍，闭口时吻尖距施法锚点约 19.6 格。
- 释放后即向巨口牵引扇区内的生物。鳄头和伤害扇区沿竖阵锁定的朝向，覆盖前方 20 格、140°，地面预警标出同一范围。第 12、28、44、60、76 tick 各咬合一次，中途进入的目标也会被卷入。
- 每口的基础份额为 `法力上限 × 法强 ÷ 5`。前四口各取该份额与目标最大生命 10% 中的较小值，命中后把差额为该目标单独记下。第五口造成本口份额加此前仍保留的差额。目标离开扇区即清除其差额，中途入场也只计实际命中的各口。首次受伤且存活的目标获得 10 秒腐朽（攻击力 −30%，每秒掉 1 点血）。最后一口命中且存活的目标被拖入地下定身 3 秒。
- 吸血：第 84 tick 一次性回给施法者 **`施法者生命上限 × 法强 × 0.1`**（裸装 20 上限回 2 点），命中多个也只回一次；一个都没咬到就不回。施法者在结算前死亡则不回。

**超位·幽钟齐鸣 `phantom_belfry`**（`SpellFigure.java`）
- 吟唱 10 秒，法阵特效同上。释放后在施法位置升起三维钟架、中空钟体和摆动的钟舌，半径 24 格（0.2.2 加大；释放冲击波第 4 tick 扩到 24 格，标出实际范围）。
- 每 15 tick 敲一拍，共 8 拍。每拍在半径约 20.5 格内升起 3 根声波柱，第一根落在随机一个目标身旁；柱半径 3.5 格。同一拍每个目标最多挨一次，伤害是小数值 **`法力上限 / 20 × 法强`**。
- 第 8 拍后停 30 tick，「大钟齐鸣」：半径 24 格内所有目标受 **`法力上限 × 法强`**，并眩晕 3 秒。实测法力上限 100、法强 1 时前 8 拍每次 5 点、终鸣 100 点。

**超位·落日剑舞 `sunset_blade_dance`**（`SpellFigure.java`、`OffensiveSpells.java`、`OffensiveControl.java`）
- 起手即进入铁魔法吟唱（`CastType.LONG`），在固定 160 tick 内均匀连斩 8～12 刀，每次挥刀持续 13 tick，侧阵同步射出剑光与花瓣。主手施法用主手挥，副手施法用副手挥，正反手刀光交替。每刀在挥手的同一服务端 tick 按当前朝向结算，覆盖前方 6 格、左右各 80°，造成 `6 × 法强`，可连续命中同一目标。
- 每刀向左右各甩出两片大花瓣，每片飞约 7.8 格，对同一目标最多造成一次 `3 × 法强` 的伤害。落地花瓣留在原处发暗红光。
- 8 秒吟唱完成时，所有花瓣同时引爆，白闪后火浪扩至半径 14 格。范围内目标受一次 `法力上限 × 法强` 的伤害并被点燃 5 秒。自制贴地火焰持续 100 tick 后淡出，期间每 4 tick 点燃范围内目标。火海不放置方块。
- 吟唱期间占用施法手，不能另行普攻或举盾，收招即恢复。剑舞期间免伤，带 `bypasses_invulnerability` 标签的伤害（如 `/kill`、虚空）仍然生效。主动取消或被眩晕打断时立即解除免伤，清除花瓣并停止后续连斩和终爆。[8 秒剑舞与打断专服记录](../build/reports/ultra-release-review/sword/验收.md)

**超位·骨翼 `bone_wings`**（`MovementSpells.java`、`BoneHealthMixin`、`BoneHeartsHudMixin`）
- 永久「骨翼」效果：移速 +50%，可以飞（和创造模式一样的操作），最大生命 +20 并立即回满这 20 点。这 20 点显示为**灰色的心**。
- 受伤先扣灰心；普通治疗补不回已经扣掉的灰心。
- 灰心扣光：退出骨翼，生命上限、移速恢复，收回飞行（创造和旁观模式除外），多出的余伤扣红心，活着的话给 3 秒缓降防摔死。死亡时移除。
- 骨翼期间不能再放。背后两翼在 12 tick 内逐节展开，每侧 12 段翼骨，骨节之间完全镂空。灰心耗尽时两翼碎裂，每侧散落 12 块骨片。[骨翼与施法手实测](../build/reports/spells-024/验收.md)
- 下线重进、从末地返回后，骨翼、剩余灰心和飞行都保留。两次实测都保留了 4 点红血和 11 点灰血，相当于 2 颗红心和 5 颗半灰心。[骨翼存档实测](../build/reports/round2-fixes/wings/checks.json)

<a id="sec-13-5"></a>
### 13.5 共用状态效果

| id | 中文 | 类型 | 效果 | 牛奶能解 |
|---|---|---|---|---|
| `phased` | 虚化 | 增益 | 见虚化 | 不能 |
| `burrowed` | 地遁 | 增益 | 见地遁 | 不能 |
| `parry_stance` | 防御架势 | 增益 | 见蔷薇怨灵 | 不能 |
| `bone_wings` | 骨翼 | 增益 | 见骨翼 | 不能 |
| `decay` | 腐朽 | 有害 | 攻击伤害 −30%，每秒 1 点魔法伤害。目前只由亡鳄吞噬施加，10 秒 | 能 |
| `stunned` | 眩晕 | 有害 | 见下 | 能 |

**眩晕**（`expansion/offense/OffensiveControl.java`）：
- 移速 −100%，每 tick 把目标拉回眩晕时的位置、速度清零；非玩家生物（包括 Boss）暂停 AI，结束时恢复。
- 玩家：打断正在使用的物品和吟唱，不能施法、攻击、交互、使用物品。
- 被眩晕者亲自造成的伤害全部取消（已经飞出的弹射物不拦）。
- 来源：幽钟齐鸣终鸣 3 秒、蔷薇怨灵反击 1 秒。

<a id="sec-13-6"></a>
### 13.6 目标判定

0.2.2 至 0.2.5 使用 `SpellTargets.canHit`。下列规则用于具有攻击行为的新法术，包括范围伤害、追踪、反击和眩晕。
- **排除**：施法者自己；已死亡、不可攻击、旁观的实体；盔甲架；创造模式玩家。
- **玩家之间**：按服务器的 PvP 开关和队伍友伤设置，和原版一样。
- **其余生物全部算目标**：怪物、中立和被动生物、村民、铁傀儡，包括**施法者自己的宠物**和同队伍里的非玩家生物。
- 铁魔法自带的友伤判断（`DamageSources.applyDamage` 里的 `isFriendlyFireBetween`）对这八个法术改用上面的判定（`BossSpellDamageMixin`）。
- 末影龙这类多部位生物：超位法术和金钟冲击按部位判范围、伤害给本体，每次结算只打一下；地遁、怨灵、反击只看本体碰撞箱。眩晕对末影龙强制生效。
- 0.2.0 和 0.2.1 只打怪物和交过手的实体，并排除自己的宠物和队友。0.2.2 专服已验证牛、村民、铁傀儡、施法者自己的驯狼、僵尸及未交战的生存玩家都能在范围边缘受伤。[范围、友伤与卷轴验收](../build/reports/spells-022-release/验收.md)
- **待实现的用户要求（11-22 22:30）**：「对，队友也算，但是宠物不算，范围要打。」截至 0.2.5，自己的驯狼仍可被击中。原话未细分宠物归属或逐项指定直击与范围伤害的例外。

<a id="sec-13-7"></a>
### 13.7 美术与特效

astra 负责特效设计与制作，审查核对实机表现。鳄颅、钟架和中空钟体用几何模型绘制。法阵以线条、旋转阵盘和光束表现。蔷薇怨灵沿用迎战的小怪外观。

| 学派 | 调色板（沿用 9.3 的像素规范：无抗锯齿、左上光、1px 最暗色描边） |
|---|---|
| 钟鸣 | 幽青 `#7FE3D8`、深青 `#2E7C78`、夜蓝 `#1B2B3A`（描边）、古铜 `#B07A3A`、钟金 `#F2C14E`、骨白 `#EDEFE6` |
| 腐沼 | 苔绿 `#6B8F3A`、深苔 `#3D5A24`、泥褐 `#2B2418`（描边）、浊黄 `#8C7B4F`、腐绿 `#B7D45A`、骨白 `#E8E2C8` |
| 蔷薇 | 绯红 `#D7263D`、酒红 `#7A1030`、暗紫红 `#2A0E1A`（描边）、荆绿 `#3F6B3A`、落日橙 `#FF8C42`、晚霞 `#FFD6A5` |
| 超位 | 暗红 `#8B1E2D` + 金 `#F5C542` |

- 初版的 30 张贴图涵盖法术图标、学派物品、墨水、效果、实体和法阵。0.2.2 起三学派卷轴引用铁魔法的原版卷轴模型。八个法术图标已重绘为 16×16、每张 4～8 色，四个超位图标采用暗红框与四角金点。[新旧对照与实机核验](../build/reports/ritual-art/美术交付.md)
- 亡鳄与幽钟的 10 秒吟唱保留直径 24 格的内阵、外缘卫星阵和空中光环。释放时亡鳄竖阵标出前方 20 格的咬合方向，幽钟冲击波标出 24 格半径。金钟冲击呈金色声环，蔷薇怨灵在出生点绘制五芒星，虚化玩家半透明。
- 从 0.2.6 起，亡鳄、幽钟、剑舞、骨翼各按自身吟唱进度展开十九面阵。地面主阵直径约 96 格，十二面副阵沿外缘分批立起，三层空中环冠反向交错旋转，两面交叉竖阵在中央展开。末段点亮顶阵，最高约 56 格。阵纹按顺序绘出，由客户端复用同一组几何笔画，四个法术分别着色。法阵外缘表示吟唱演出的大小，实际判定见 13.4。
- 0.2.5 的四个超位法术共用十二面阵盘：地面主阵、两层天环、施法者身后的一面竖阵，以及分布在周围的八面副阵。阵盘由简单几何线条组成。交错光线连接上下两端，高处光核在释放时发出光束。主阵展开时直径 68 格，剑舞连斩期间先展开直径 42 格的阵盘，终爆时再扩到 68 格。攻击的实际命中范围见 13.4。
- 0.2.3 重做剑舞和骨翼特效。剑舞白刃、橙色刀光与绯红拖尾覆盖斩击扇区，花瓣在实际落点一起爆开，火焰面片贴地。0.2.4 保留这些效果，将刀光与实际施法手同步，并去除骨翼灵膜，保留逐节展开和灰心耗尽时的骨片碎裂。[0.2.3 昼夜对照](../build/reports/spells-023-art/美术验收.md) · [0.2.4 双视角实拍](../build/reports/spells-024/验收.md)

<a id="sec-13-8"></a>
### 13.8 第二轮已知限制

1. 超位是模拟的第 6 档（13.2）。铁魔法自己的界面、配置和其它模组里，超位法术仍会被当作「传说」处理。
2. 默认属性下，法术书施放超位要求至少 1000 点当前法力。卷轴可以在零法力时施放，剑舞出刀后取消也消耗一张（13.2）。
3. 同一施法者同时只有一组怨灵负责守卫。再次成功召唤时，新一组替换仍在绘阵或守卫的旧组；已经离阵追击的怨灵继续本次出击。最后一个守卫消失时，防御架势同步结束。
4. 地遁只有伤害随等级变，持续时间和移速六级一样。
5. 落日剑舞的火海只持续 5 秒，贴地火焰是视觉效果，区域内生物会被点燃，地面不生成火方块。
6. 初版图标的辨识度问题已在 `501c3b9` 重绘时处理，见 13.7。
7. 双人真人联机没有测过。
8. 阶段 B、C 的 `0d2f009` 复核曾记录第 9～12 条。亡鳄的朝向修复已发布，虚化于 0.2.2 改为到期原地结束，详见 13.4。
9. **历史限制（0.2.1 及以前）**：虚化期间若世界边界缩进玩家身体，会短暂卡在界外。0.2.2 移除了虚化对边界的额外拦截。
10. **止心术、黄心、骨翼灰心三者同时生效**。条件：玩家同时有铁魔法止心术、伤害吸收（黄心）和骨翼灰心，然后受伤。后果：黄心会先于灰心被扣，玩家多掉几点黄心，灰心和生命不受影响。要三样凑齐才会出现。
11. **网络积压超过 10 tick 时打断吟唱**。条件：施法者和旁观者之间的网络积压超过 10 tick，此时超位法术的吟唱被打断。后果：旁观的玩家看不到法阵的碎裂动画，只是画面问题，不影响伤害、法力和冷却。
12. **其它模组在施法事件里传送玩家**。条件：另一个模组在铁魔法的施法事件里把施法者传送走。后果：本次法术不生效，但法力已经扣掉，白扣一次。只有第三方模组这么做才会发生。
13. **历史限制（0.2.1）**：8 格内唯一能站的出口是栅栏或围墙顶面时，会退回施法点。0.2.2 起到期原地恢复碰撞，已停用落点搜索。
14. **虚化可以穿过地板和底层基岩**。专服已测到玩家静止时向下穿过地板，墙内到期时恢复窒息伤害（见 13.4）。按 0.2.2 的移动代码，底层基岩和世界边界同样不能阻挡虚化玩家。生存玩家落到维度最低建造高度以下超过 64 格时，会触发原版虚空伤害，留在该处可致死。本轮仅从代码核实了虚空伤害和越界路径，实测记录覆盖穿地板和六向穿基岩、屏障。[穿地板的位置记录](../build/reports/phase-022/runtime/session.log)
15. **攻击法术会伤及宠物**。施法者自己的狼可被范围伤害命中，蔷薇怨灵也会追踪并自爆在狼身上。后续宠物排除要求见 13.6。

---

<a id="sec-14"></a>
## 14. 觐见仪式、法杖与爆裂魔法

「觐见」（`fortune_spells:audience`）是神圣学派的传说Ⅰ仪式法术。在铁魔法的炼金釜中加入四瓶超位墨水，再投入一件迎战首领掉落物，即可制作一张觐见卷轴。催化材料可选幽魂质、藓牙、暮光紫罗兰或命运之眷。觐见只能直接使用卷轴施放，无法抄入法术书。基础吟唱 700 刻（35 秒），施法加速后最短 620 刻（31 秒）。

| 位置 | 数量 | 每个位置的供品 |
|---|---:|---|
| 外围彩色五芒星 | 8 | 每处一瓶超位墨水（`fortune_spells:transcendent_ink`），共八瓶 |
| 内圈较大的五芒星 | 4 | 敲钟人的幽魂质、沼泽巨颌怪的藓牙、罗莎琳的暮光紫罗兰、福尔图娜女爵的命运之眷，各一件 |
| 中央五芒星 | 1 | 铁魔法死者之王掉落的猩红神杖（`irons_spellbooks:blood_staff`） |

外圈供位距阵心 24 格，内圈距阵心 11 格。内圈四种材料可以任意排序。施法者用主手拿对应材料，右键五芒星下的方块即可放入一件，也可以亲手把材料丢到阵位上。主手留空再右键，可以在集齐前取回。放入的物品悬在各自的星阵上，缓慢上下浮动。

从法阵出现起，每 20 刻消耗施法者 1 点法力。十三个供位配齐后，各供位亮起，粒子和阵环向中心汇聚。催动 60 刻后，仪式消耗供品，在阵心产出一把法杖并停止扣蓝，再用 40 刻播放收束特效。法力耗尽会结束未完成的仪式，已经放入的材料按原物品掉回阵心，保留附魔及自定义名称。

新法杖（`fortune_spells:audience_staff`）使用立体模型。玩家首次拿到未命名的法杖时，在命名界面自行输入名字。暂时关闭界面后，仍可给这根杖命名。名字确认后保存在该物品上，按文字逐字显示渐变色。

服务端的仪式实体记录阵心、每个供位的物品、当前阶段和阶段开始的游戏刻。客户端用这些值绘制供阵、催动和收束，中途进入视野的玩家会从当前阶段看到法阵。绘制沿用参考项目的局部坐标思路，先建立环、星和门形阵纹，再在各个阵位分别平移、着色和旋转。[参考工程](https://github.com/Michaelihc/projectmer-unity-exporter)

### 法杖的五个法术

法杖自带亡鳄吞噬、幽钟齐鸣、落日剑舞和骨翼，轮盘中均显示 10 级。前三个法术用杖释放时，伤害、作用距离和法阵特效尺寸均为普通版本的两倍。亡鳄扇区向前延伸到 40 格，幽钟终鸣半径变为 48 格，落日剑舞终爆半径变为 28 格。骨翼是自身增益，双倍法阵尺寸适用。各法术沿用原有吟唱时间。

第五项是法杖专属的火焰学派法术「爆裂魔法」，轮盘等级显示为「达到世界的上限」。起手时按准星在前方 80 格内确定阵心。吟唱固定 600 刻（30 秒），展开直径 1088 格的地面主阵、十二座地面副阵与十八座空中副阵。目标上方九层阵环由高到低显现，周围的粒子连向中心。成功释放时，服务端将当前法力清零；12 刻后光束落地，对阵心半径 64 格、上下各 32 格内的有效目标结算一次伤害。基础伤害为施法者法力上限 × 法强 × 50。

用这把杖施法时，施法者的镜头缓慢升空。按 I 可接管镜头，用鼠标环视、移动键自由选取机位，直至法术演出结束。按 U 可提前回到玩家视角。镜头移动期间，玩家本体和服务端的轰击坐标都留在原处。爆裂的九道阵环相邻两层的高度差为 42 格，最高的一层位于轰击点上方 358 格。

「觐见」的绘阵从卷轴吟唱起手开始。地面、地下与天空的阵纹随吟唱进度逐步显现。吟唱完成后，十三个供位开放，从此时起每秒扣除 1 点法力。提前打断时，正在绘制的阵纹消散。

### 破绽

落日剑舞每次挥剑命中后，给该目标添加一层「破绽」，最高十层。每层使目标受到的伤害增加 10%，按层数相加。第一剑给原本没有破绽的目标挂上Ⅰ层；第二剑享受 10% 增伤，再把目标升到Ⅱ层。每次挥剑命中都会把持续时间刷新为 8 秒。其他伤害也享受目标身上的增伤。

### 进度与法杖称号

| 进度 | 取得条件 | 描述 |
|---|---|---|
| 窥见未来 | 获得超位墨水 | 获得来自未知命运的物品。 |
| 传奇 | 首次获得超位法术 | 成为举世无敌的传奇法师。 |
| 超脱命运 | 获得觐见卷轴 | 觐见亘古长存的无上 |
| 创世者 | 获得觐见法杖 | 你已经不在命运之中，我无法描述你的命运 |

「创世者」的描述使用四色文字。称号前缀采用玩家获得的第一把已命名法杖的名字。服务端根据原版死亡次数判断，已有死亡记录的玩家获得金色的「〈杖名〉之主·命运的超脱者」，零死亡玩家获得「〈杖名〉之主·掌控一切的创世神」，灰色光泽沿字逐个移动。首次死亡并重生后，称号转为金色。

### 法杖属性、伤害与极限复生（0.2.14）

主手或副手持觐见法杖时，法力上限增加 1000 点，总法强乘以 2，铁魔法法术的冷却归零。两手各持一杖只应用一份属性。提高法力上限后，当前法力仍按铁魔法的回复规则补充。

从杖上施放的攻击按自身公式结算，使用杖版伤害系数，并计入目标已有的破绽。法杖可击穿护甲、限伤和首领无敌阶段。「盲眼赌局」的祝福可突破限伤，目标处于无敌阶段时仍能免疫这次攻击。目标选择继续遵循既有的施法者排除和玩家 PvP 规则。

安装诡厄巫法：启示录后，下界亚波伦进入「末日终结」时仍可持杖起手并持续吟唱，法杖提供的法力与法强继续生效。安装莱特兰恶意后，持杖者可抵御「诸神黄昏」的封印和怪物的毒性光环。这两项联动按已安装的模组启用。

极限模式中，服务端在玩家真正死亡时检查其物品栏。持有法杖就以死亡时刻为起点计时 60 个游戏刻，死亡处显现法阵，时间到后让玩家重生。死亡次数和物品掉落遵循存档规则；图腾成功救下的玩家仍然生存，不会触发这次复生。按住 Shift 可阅读带自定义杖名的神力铭文，Alt 显示另一段箴言。客户端用银灰色流光与细小粒子绘制文字。

<a id="changelog"></a>
## 附：变更记录

| 时间 | 谁 | 变更 |
|---|---|---|
| 11-21 | opus | v1.0 草稿：12 节契约定稿 |
| 11-21 | 文档 | 整理排版：补目录锚点、文首信息表、交叉链接；`FortuneIds.java` 改为「第②步由 astra 按第 3、8 节的表建立」，2.1 节归属同步改；11.1 指向 README。ID、包名、数值、时间线、网络包字段和顺序、同步数据取值、文件归属（除 `FortuneIds.java` 那一行按 opus 要求改）均未改动 |
| 11-21 | opus | 文首「待 opus 确认」注改为已定：沿用现有 `FortuneIds.java`。记下第②③步后端两处实现差异（不改 ID 和包字段）：学派焦点参数按铁魔法 3.16.4 包成 `TagKey<Item>`；命中 FX 在 `LivingDamageEvent`（LOWEST）发送最终伤害，命中音效由服务端广播，前端只处理粒子 |
| 11-21 | opus | 新增 9.7：前端拍板的实现细节（类分工、渲染、特效调整、HUD、贴图 UV 和染色要求）。`CardPickInput` 并进 `ClientCardGame`；孤注一掷蓄好的特效由 8 个 card_shard 改为 20 个红色 spark；目标花色小图改用字形。client 代码按用户要求全部由 astra 写 |
| 11-21 | 文档 | 规划变更 3 处：2.1 节 `client/**` 实现方改为 astra（opus 只负责规划和美术把关）；9.4 节删去 `glow_target/hover/win/lose.png` 和 `gui/card_game_hud.png`，注明发光边框和倒计时条由代码绘制、贴图由 astra 用生图模型制作、opus 审核；9.6 节写明倒计时条代码绘制规格（1px 天鹅绒描边、牌背墨底、金→筹码红渐变填充） |
| 11-21 | 文档 | 按后端修复 `849221f` 同步「命运伤害结算」：触发时在原来那次命中里直接把伤害类型换成命运伤害，只走一次原版伤害结算，死亡、不死图腾、击杀归属各结算一次，不再「取消后重打」。改动如下：6.1 改写触发描述；7.1 表中 `bypasses_shield`、`bypasses_cooldown` 的说明改掉「重打」理由；7.2 按现实现重写为 5 步（`hurt()` 入口判定 → 铁魔法投射物闪避复用判定 → 包住 `actuallyHurt` 算应有伤害 → Hurt / Damage 两阶段分发后恢复应有伤害，最终伤害 > 0 才消耗孤注一掷并发 FX），取代上一条里「命中 FX 在 `LivingDamageEvent`（LOWEST）发送」的说法；5.3 叠加顺序按现实现改写（倍率不变，最高 ×4）；12 节第 4 条「两次受击声和两次击退」改为 Mixin 兼容风险。两份玩家说明同步删掉「取消后重打」和「两次受击」。ID、数值和网络包都未改 |
| 11-21 | 文档 | 按 Tommyxu 的分工要求改分工描述：文首信息表的 opus 一行改为「设计、规划、审美，不写代码」，astra 一行改为「全部代码（前端 + 后端，含修复和构建配置）」；2.1 节末加注，说明「前端 / 后端」只指客户端 / 服务端，不指实现者。归属表、ID、数值和接口都未改 |
| 11-21 | 文档 | 按 opus 认可的第④步实现差异改 9.7 两处：「渲染」里发光边框由 `RenderType.lightning()` 加法混合改为普通透明混合（当前实现 `RenderType.gui()`，见 `4594ce3`），原因是加法混合会把亮背景上的灰紫边框冲成白色；框宽、颜色、优先级和呼吸节奏都不变。「贴图硬要求」里 `fate_spark_*`、`forsaken_mote_*` 的主色由象牙白 `#FFF4E0` 改为「浅中性灰到白」，与现有贴图一致。`card_shard` 的配色未改 |
| 11-21 | 文档 | 按用户要求「卡牌保持原本的样子，不用加贴图和多余东西」收窄范围（opus 定取舍）：卡牌沿用现有样子，贴图不再返工，9.4 清单里的 22 张贴图按 `pixelsky/flow-b455f0-c` 上的 `234b9cf` 定稿（11-21 opus 二审通过，三个法术图标、裂牌和 Joker 正面已按首审意见重画）。`f4804f0` 是另一套未审过的贴图，不用。9.5 的「每次爆发 ≤ 60 粒子」「不在渲染帧里分配对象」改为尽量做到、不作硬性验收，9.5 加注，9.7 两处引用同步。审查 `4594ce3` 时退回的 3 处按现状接受：全画质猜中同一 tick 66 个粒子（`FortuneFx.java:89、138`）、查询 `RenderType` 构造临时键（`FateCardRenderer.java:142、157`）、HUD 每帧量文字宽度（`CardGameHud.java:132`）。最后 60 tick 倒计时不红闪（`CardGameHud.java:106`）属于功能问题，仍要修，不在接受之列 |
| 11-21 | 文档 | 审查复核 849221f 后，3 处按现状接受（opus 定），4 处交给 astra 修。接受的 3 处写进第 12 节第 7～9 条：魔法飞弹未触发命运时可能吃掉两层闪避（铁魔法 3.16.4 自身问题）、其它模组转移伤害时孤注一掷可能不生效、命运伤害无视焰护之戒这类按伤害类型的免疫。7.2「结论」后补一段，说明「不无视 Attack 阶段免疫」只指不看伤害类型的那一类，和第 9 条对齐。交给 astra 的 4 处（切割者之骰增伤被覆盖、止心术重复扣血、喷溅药水借命运绕过闪避、深渊庇佑下箭跳过命运）修完后，如果改变了 7.2 的结算步骤再同步 |
| 11-21 | 文档 | 11-21 审查复审 `b0f60e3`：①②④通过，③主路径通过，两条无伤害碰撞路径按现状接受（opus 定），第①步后端收尾。接受的两条写进第 12 节第 10、11 条 |
| 11-21 | 文档 | 按 `b0f60e3` 同步 7.2 结算步骤和 12 节第 4 条（Mixin 由 4 个变为 7 个，增伤改为在 setAmount 时保留）。具体：7.2 标题改为 7 个 Mixin，并加表列出各自的目标和职责；第 2 步补上深渊庇佑，以及投射物的碰撞伤害预判和已知例外；第 4 步改为 `FortuneEventAmountMixin` 只接受上调、黄心照常先扣；第 5 步注明只注入 `onLivingDamage`，最终伤害 > 0 才消耗孤注一掷、发特效不变；新增第 6 步止心术（`IronHeartstopMixin`）；结论段补「增伤照常生效、无视止心术延后结算」。12 节第 4 条按 Mixin 逐条列兼容风险，删掉 `onLivingHurt`。顺手把第 11 条的笔误「祈福」改为「射手身上的「被命运选中」」 |
| 11-21 | 文档 | 为发布 0.1.0 准备文档：新增 `docs/发布说明.md`（安装清单、第⑤步验收结果、已知问题）；12 节第 3 条按第⑤步实测改写（火焰箭、冰锥丢附效，地狱浮现、地震保留）。README 状态行和两份玩家说明同步：去掉「草稿 / 还没验收」，墨水等级表改为已实测，补上下线和死亡时「牌局作废」提示的实际表现。代码和规格未改 |
| 11-22 | 文档 | 新增第 13 节（第二轮 0.2.0），文头版本改为 v2.0、目录和信息表同步。依据 `需求记录-第二轮.md`、`build/reports/plan-v2/规划.md`，数值按 `main@a0f654e` 源码和 astra 验收记录。内容：三个学派及其掉落物 ID；超位稀有度用备用方案及原因（铁魔法 3.16.4 稀有度写死 5 档、配置限定 5 项）；超位墨水配方；8 个法术总表与行为；共用效果；敌人判定；美术和 opus 审稿结论；第二轮已知限制。骨翼下线和换维度后的表现等审查结论再补 |
| 11-22 | 文档 | 11-22 审查复核 0d2f009，4 处按现状接受（opus 定），虚化落点和亡鳄转向交 astra 修。接受的 4 处写进 13.8 第 9～12 条（世界边界缩进虚化中的玩家、止心术 + 黄心 + 灰心同时生效、网络积压超过 10 tick 时打断吟唱看不到碎裂动画、其它模组在施法事件里传送玩家白扣法力），第 8 条改为审查进展。交 astra 的 2 处在 13.4 虚化、亡鳄吞噬行内标「待修」并写明要求。13.4 骨翼补上重登和末地返回后保留（`0d2f009` 已修、实测通过）。顺手改掉笔误：第 13 节和两份玩家说明第 8 节的「吐唱」→「吟唱」、「亢自」→「亲自」，`需求记录-第二轮.md` 的「斜击」→「斩击」。两份玩家说明的骨翼「下线、换维度待实测」同步为重登和末地返回后保留 |
| 11-22 | 文档 | 11-22 审查复核 0ec3c90：亡鳄转向通过；虚化悬空误拉回交 astra 修，栅栏顶出口按现状接受（opus 定）。栅栏顶出口写进 13.8 第 13 条；13.4 亡鳄吞噬去掉「待修」，改为按竖阵冻结时的朝向咬合；13.4 虚化补一句悬空误拉回待修 |
| 11-22 | 文档 | 记录用户实测 0.2.1 后的要求（原话见 `需求记录-第二轮.md` 3.9），代码尚未改，文中标「待改」：13.1 钟鸣、腐沼、蔷薇卷轴改用铁魔法原版材质；13.2 超位卷轴改回铁魔法原版卷轴规则（无 1000 门槛、不扣法力、不进冷却），法术书仍要 1000；13.8 第 2 条同步。判定范围和释放特效的调整等 opus 定方案后再写 |
| 11-22 | 文档 | 按 0.2.2（`2c3ba5e`）源码同步第 13 节，并记下用户原则「除了我说的，其他尽可能遵循铁魔法原版」。卷轴：三学派卷轴改用铁魔法原版材质，超位卷轴按原版规则（无 1000 门槛、不扣法力、不进冷却），法力和冷却全部交给铁魔法原生流程；目标：13.6 重写，除施法者本人外几乎所有生物，玩家间按 PvP 和队伍友伤；范围：亡鳄 140° / 20 格、幽钟 24 格、柱半径 3.5、剑舞斩击 6 格 160°、火海 14 格、高度统一 ±8，公式不变；虚化：按用户「穿透所有方块，剩余的不用多管」重写，删掉悬浮、基岩 / 屏障 / 边界拦截和到期找落点。13.8 第 9、13 条标为不再适用，新增第 14、15 条 |
| 11-22 | astra | 0.2.3 只重做落日剑舞特效、骨翼加特效，其余不加。0.2.4 改为纯骨翼，剑舞起手吟唱、按施法手每 5 tick 挥刀，并记录书与卷轴的打断消耗。补入虚化六向穿透实测，在 13.6 区分已发布的宠物命中与 22:30 新要求。 |
| 11-22 | 审查 | 0.2.5 的四术释放展开十二面法阵。落日剑舞固定 8 秒，亡鳄持续牵引并分五次咬合，幽钟使用三维钟架和中空钟体。 |
| 11-22 | astra | 0.2.6（`b87fc32`）为四个超位法术加入十九面分段吟唱法阵，将亡鳄头颅放大至 3.25 倍。 |
| 11-23 | 审查 | 蔷薇怨灵改为先绘五芒星、再以迎战小怪的外观现身；跟随施法者连链守卫，架势结束才追击自爆。更新规划、实现与文档的分工。 |
| 11-23 | 审查 | 新增第 14 节，记录「觐见」的卷轴来源、十三个供位、仪式耗蓝与取消返还、成杖和首次命名。 |
| 11-23 | 审查 | 觐见卷轴改由四瓶超位墨水炼成，供品悬浮；记录法杖的四个 10 级超位法术、专属爆裂魔法与高空镜头。 |
| 11-23 | 审查 | 0.2.13 增加法杖自由视角、觐见吟唱绘阵、剑舞破绽和四个进度，记录爆裂九环的层间距与法杖称号的获得条件。 |
| 11-23 | 审查 | 0.2.14 增加法杖属性、末日终结与莱特兰恶意联动、极限死亡后的复活法阵，以及 Shift / Alt 银灰铭文；区分杖上攻击和盲眼赌局对首领阶段无敌的处理。 |
