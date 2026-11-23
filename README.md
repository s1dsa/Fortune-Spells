# 命运法术 Fortune Spells

命运法术把铁魔法（Iron's Spells 'n Spellbooks）和迎战（Meet Your Fight）联动起来。**0.2.16** 适用于 Minecraft 1.20.1 / Forge 47.4.0。13 个法术分属本模组的四个学派，以及铁魔法的神圣、火焰学派。[下载 0.2.16](https://github.com/s1dsa/Fortune-Spells/releases/download/v0.2.16/fortune_spells-0.2.16.jar) · [发布说明](docs/发布说明.md)

超位墨水采用「星轨天球」瓶形。一道星轨斜穿球形玻璃瓶，瓶身中央点亮一颗四芒星。整瓶的流彩动画共 40 帧，约两秒循环一次。物品栏格子外缘有一圈 1 像素的金边，四角呈浅金色。[查看墨水动图](https://github.com/s1dsa/Fortune-Spells/releases/download/v0.2.16/rainbow-ink.gif)

| 学派 | 法术 |
|---|---|
| 命运 | 命运之选、盲眼赌局、孤注一掷 |
| 钟鸣 | 虚化、金钟冲击、超位·幽钟齐鸣 |
| 腐沼 | 地遁、超位·亡鳄吞噬、超位·骨翼 |
| 蔷薇 | 蔷薇怨灵、超位·落日剑舞 |
| 神圣（铁魔法） | 觐见 |
| 火焰（铁魔法） | 爆裂魔法（觐见法杖专属） |

把四瓶超位墨水倒入炼金釜，再投入任意一件迎战四大首领的掉落物，制成「觐见」卷轴。基础吟唱的 35 秒内，地面和天空的阵纹逐层绘出。阵成后，十三座五芒星开放供奉。外圈放八瓶超位墨水，内圈放四位首领各自的掉落物，阵心放入死者之王的猩红神杖，即可铸造一把由玩家亲自命名的法杖。[供奉位置与操作](docs/玩家说明.md#audience)

法杖轮盘自带四个 10 级超位法术及专属的「爆裂魔法」。爆裂吟唱 30 秒，在千格主阵上空展开九环光束。持杖施法时，按 **I** 可以自由移动镜头观察法阵，按 **U** 返回角色视角，法术继续进行。获得并命名法杖后，玩家名前会显示带杖名的称号。[法杖用法](docs/玩家说明.md#audience)

持觐见法杖时，铁魔法法术冷却归零，法力上限增加 1000 点，总法强翻倍。极限模式的角色死亡时，背包里的法杖可在 3 秒后将其复活。在物品栏指向法杖，按住 Shift 或 Alt 可阅读不同的铭文。

四种超位法术在吟唱期间逐层绘出十九面法阵。地面主阵展开到约 96 格宽，周围十二面副阵依次立起，最高一道光环升至约 56 格。释放时另外展开十二面阵盘。落日剑舞在 8 秒内缓挥 8～12 刀，亡鳄持续将目标拉向巨口，合拢的鳄吻伸至前方约 19.6 格，幽钟召出立体悬钟，骨翼在法阵中逐节展开。四个超位法术从法术书施放时，默认需要 1000 点当前法力；直接使用卷轴，零法力也能施放。卷轴配方和各法术用法见[玩家说明](docs/PLAYER_GUIDE.md)，开发规格见[设计文档](docs/DESIGN.md)。

模组 ID 为 `fortune_spells`，许可为 All Rights Reserved，仅供自用或放进整合包。必须与铁魔法、迎战及其前置一起安装。

---

## 依赖与版本

客户端和服务端都要装下面全部依赖，版本以这张表为准（DESIGN 第 1 节）。

| 依赖 | 版本 | 说明 |
|---|---|---|
| Minecraft | 1.20.1 | |
| Forge | 47.4.0 或更高（1.20.1） | 铁魔法 3.16.x 要求 `[47.4.0,)` |
| Java | 17 | 推荐 Temurin 17 |
| Iron's Spells 'n Spellbooks（铁魔法） | 1.20.1-3.16.4 | 代码基于这个版本编写 |
| Iron's Lib | 1.20.1-2.1.0 | 铁魔法 3.16.4 的前置，要求 `[1.20.1-2,1.20.1-3)` |
| Meet Your Fight（迎战） | 1.20.1-1.6.2 | |
| Curios | 5.14.1+1.20.1 | |
| GeckoLib | 4.8.2（geckolib-forge-1.20.1） | |
| Player Animator | 1.0.2-rc1+1.20 | 铁魔法运行时必需 |

本模组在 `mods.toml` 里声明的最低版本：`forge [47.4.0,)`、`minecraft [1.20.1,1.20.2)`、`irons_spellbooks [1.20.1-3.16.4,)`、`meetyourfight [1.5.0,)`、`curios [5.14.1,)`、`geckolib [4.8.2,)`。

联机时，服务器和每个客户端使用同一版模组 JAR。

---

## 在 Windows 上构建和运行

### 1. 准备 JDK 17

1. 安装 JDK 17（如 Eclipse Temurin 17）。
2. 把环境变量 `JAVA_HOME` 设为 JDK 17 的安装目录，例如 `C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot`。
3. 打开新的命令行窗口，运行 `java -version`，确认输出的是 17。

### 2. 构建

在工程根目录（就是 `gradlew.bat` 所在的目录）打开命令行：

```bat
gradlew.bat build
```

PowerShell 里要写成 `.\gradlew.bat build`。

- 构建 0.2.16 的产物在 `build\libs\fortune_spells-0.2.16.jar`。
- 首次构建会下载依赖、反编译 Minecraft，需要几分钟，之后会快很多。
- 依赖下载失败（尤其是 Modrinth 上的迎战）时，请在频道 #111 里报，后端会换成 cursemaven 坐标（DESIGN 第 12 节第 5 条）。

### 3. 启动开发客户端

```bat
gradlew.bat runClient
```

启动的开发客户端会自动带上全部依赖。游戏内的验收步骤见 DESIGN 第 11.2 节。

### 4. 启动开发专用服务器

```bat
gradlew.bat runServer
```

用来验证专用服务器能正常启动不崩溃，也就是代码没有在服务端引用客户端类（DESIGN 11.2 第 10 条）。

首次启动专服时，请阅读运行目录 `run` 中的 `eula.txt`，同意 EULA 后将 `eula=false` 改为 `eula=true` 再启动。

---

## 装进整合包

1. 整合包实例先装 **Forge 1.20.1-47.4.0**（或更高的 47.x）。
2. 把下面这些 jar 放进实例的 `mods\` 文件夹，客户端和服务端都要放：

| jar | 版本 |
|---|---|
| 本模组 [fortune_spells-0.2.16.jar](https://github.com/s1dsa/Fortune-Spells/releases/download/v0.2.16/fortune_spells-0.2.16.jar) | 0.2.16，升级时替换旧版 JAR |
| Iron's Spells 'n Spellbooks | 1.20.1-3.16.4 |
| Iron's Lib | 1.20.1-2.1.0 |
| Meet Your Fight | 1.20.1-1.6.2 |
| Curios | 5.14.1+1.20.1 |
| GeckoLib（Forge 1.20.1） | 4.8.2 |
| Player Animator（Forge） | 1.0.2-rc1+1.20 |

3. 进游戏检查：铁魔法的卷轴合成台能做出「命运」学派法术（学派名显示为红色）。具体操作见 [玩家说明](docs/PLAYER_GUIDE.md)。

---

## 版权说明

- 迎战的素材由原作者保留全部权利。本模组只在运行时按路径引用迎战的贴图和声音，不复制、不描摹、不打包这些文件。
- 命运卷轴使用本模组的原创外观；钟鸣、腐沼、蔷薇卷轴在游戏中使用铁魔法提供的原版卷轴材质。
- 详见 DESIGN 第 9.1 节。
