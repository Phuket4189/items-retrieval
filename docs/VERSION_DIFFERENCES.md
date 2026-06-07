# 版本差异文档 (Version Differences)

本文档记录 Items Retrieval 模组在不同 Minecraft 版本间的 API 差异与适配方案。

---

## 支持版本一览

| 分支 | Minecraft | Fabric Loader | Fabric API | Yarn Mappings | Java |
|------|-----------|---------------|------------|---------------|------|
| [`mc/1.21.10`](../../tree/mc/1.21.10) | 1.21.10 | 0.18.1 | 0.138.4+1.21.10 | 1.21.10+build.3 | 21 |
| [`mc/1.21.4`](../../tree/mc/1.21.4) | 1.21.4 | 0.16.10 | 0.118.5+1.21.4 | 1.21.4+build.8 | 21 |
| [`mc/1.21`](../../tree/mc/1.21) | 1.21 | 0.15.11 | 0.100.8+1.21 | 1.21+build.9 | 21 |
| [`mc/1.20.6`](../../tree/mc/1.20.6) | 1.20.6 | 0.15.11 | 0.97.8+1.20.6 | 1.20.6+build.3 | 21 |

---

## API 差异矩阵

### 1. 输入处理 (Input Handling)

| API | 1.21.10 | 1.21.4 | 1.21 | 1.20.6 |
|-----|---------|--------|------|--------|
| 鼠标事件参数 | `Click` record | `(double, double, int)` | `(double, double, int)` | `(double, double, int)` |
| 键盘事件参数 | `KeyInput` record | `(int, int, int)` | `(int, int, int)` | `(int, int, int)` |
| `mouseClicked` | `(Click, boolean)` | `(double, double, int)` | `(double, double, int)` | `(double, double, int)` |
| `mouseDragged` | `(Click, double, double)` | `(double, double, int, double, double)` | `(double, double, int, double, double)` | `(double, double, int, double, double)` |
| `mouseReleased` | `(Click)` | `(double, double, int)` | `(double, double, int)` | `(double, double, int)` |
| `keyPressed` | `(KeyInput)` | `(int, int, int)` | `(int, int, int)` | `(int, int, int)` |

> **说明**：`Click` 和 `KeyInput` record 类是 1.21.10 引入的新 API。老旧版本使用原始参数形式。
>
> **影响文件**：`SearchScreen.java`

### 2. 渲染管线 (Render Pipeline)

| API | 1.21.10 | 1.21.4 / 1.21 / 1.20.6 |
|-----|---------|--------------------------|
| 管线构建 | `RenderPipeline.builder(...)` | ❌ 不存在 |
| 混合模式 | `BlendFunction.TRANSLUCENT` | ❌ 不存在 |
| 深度测试 | `DepthTestFunction.NO_DEPTH_TEST` | ❌ 不存在 |
| 管线常量 | `RenderPipelines.RENDERTYPE_LINES_SNIPPET` | ❌ 不存在 |
| 自定义 RenderLayer | `RenderLayer.of(name, size, pipeline, phases)` | `RenderLayer.of(name, format, mode, size, ...)` |

> **适配方案**：使用 `RenderLayer.getLines()` 直接获取原版线条渲染层，替代自定义管线。
>
> **影响文件**：`SearchHighlightRenderer.java`

### 3. 渲染绘制 (Vertex Drawing)

| API | 1.21.10 | 1.21.4 / 1.21 | 1.20.6 |
|-----|---------|----------------|--------|
| 方块绘制 | `VertexRendering.drawBox()` | `VertexRendering.drawBox()` | ❌ 不存在 |
| 向量绘制 | `VertexRendering.drawVector()` | `VertexRendering.drawVector()` | ❌ 不存在 |
| 适配方案 | — | — | `WorldRenderer.drawBox()` + 手写 `drawLine()` |

> **说明**：`VertexRendering` 工具类在 1.21 引入，1.20.6 中不存在，需使用 `WorldRenderer` 静态方法。
>
> **法线注意**：`RenderLayer.getLines()` 要求顶点包含法线数据（POSITION_COLOR_NORMAL 格式），手动写入顶点时必须调用 `.normal(entry, x, y, z)`。
>
> **影响文件**：`SearchHighlightRenderer.java`

### 4. Fabric API 渲染事件

| API | 1.21.10 | 1.21.4 / 1.21 | 1.20.6 |
|-----|---------|----------------|--------|
| 包路径 | `rendering.v1.world` | `rendering.v1` | `rendering.v1` |
| 事件常量 | `WorldRenderEvents.END_MAIN` | `WorldRenderEvents.LAST` | `WorldRenderEvents.LAST` |
| Context 方法 | `context.matrices()` | `context.matrixStack()` | `context.matrixStack()` |

> **说明**：`.v1.world` 子包和 `END_MAIN` 事件是较新版本引入的。老旧版本使用 `.v1` 包和 `LAST` 事件。
>
> **影响文件**：`ItemRetrievalModClient.java`, `SearchHighlightRenderer.java`

### 5. RenderPhase 常量

| 1.21.10 | 1.21.4 / 1.21 / 1.20.6 |
|---------|--------------------------|
| `TRANSLUCENT` | `TRANSLUCENT_TRANSPARENCY` |
| `NO_DEPTH_TEST` | `ALWAYS_DEPTH_TEST` |
| `NO_CULL` | 移除 `.cull()` 调用 |
| `ALL_MASK` | `COLOR_MASK` |

> **说明**：RenderPhase 常量在 1.21.x 系列中被重构，名称发生变化。已通过统一使用 `RenderLayer.getLines()` 来避免此问题。
>
> **影响文件**：`SearchHighlightRenderer.java`

### 6. FeatureFlags / ScreenHandler

| API | 1.21.10 | 1.21.4 | 1.21 | 1.20.6 |
|-----|---------|--------|------|--------|
| 构造函数参数类型 | `FeatureSet` | `FeatureSet` | `FeatureSet` | `FeatureSet` |
| 正确常量 | `VANILLA_FEATURES` | `VANILLA_FEATURES` | `VANILLA_FEATURES` | `VANILLA_FEATURES` |

> **说明**：所有目标版本的 `ScreenHandlerType` 构造函数均接受 `FeatureSet` 类型参数，必须使用 `FeatureFlags.VANILLA_FEATURES`（切勿使用 `FeatureFlags.VANILLA`）。
>
> **影响文件**：`ItemRetrievalMod.java`

### 7. 键盘分类 (KeyBinding Category)

| API | 1.21.10 | 1.21.4 / 1.21 / 1.20.6 |
|-----|---------|--------------------------|
| 内部类 | `KeyBinding.Category` | ❌ 不存在 |
| 构造函数 | `new Category(Identifier)` | 直接使用 `String` 常量 |

> **说明**：`KeyBinding.Category` 内部类在 1.21.10 Yarn 映射中不存在，改用字符串分类键。
>
> **影响文件**：`ItemRetrievalModClient.java`

### 8. 实体容器类 (Entity Containers)

| 类名 | 1.21.10 / 1.21.4 / 1.21 | 1.20.6 |
|------|--------------------------|--------|
| 箱子船 | `AbstractChestBoatEntity` | `ChestBoatEntity` |
| 驴背包大小 | `getInventorySize()` public | `getInventorySize()` protected |

> **说明**：1.20.6 中 `AbstractChestBoatEntity` 未被抽象化，仍为 `ChestBoatEntity`。驴的 `getInventorySize()` 为 protected，兜底方案使用常数 15。
>
> **影响文件**：`SearchScanner.java`

### 9. 顶点格式 (Vertex Format)

| API | 1.21.10 | 1.21.4 / 1.21 / 1.20.6 |
|-----|---------|--------------------------|
| 格式常量 | `VertexFormats.POSITION_COLOR_NORMAL` | `VertexFormats.POSITION_COLOR` |

> **说明**：已通过统一使用 `RenderLayer.getLines()` 避免此差异。
>
> **影响文件**：`SearchHighlightRenderer.java`

---

## 适配文件汇总

每个适配分支相对于 `mc/1.21.10` 的变更文件：

```
mc/1.21.10 → mc/1.21.4  (6 files)
  ├── SearchScreen.java           # 输入降级
  ├── SearchHighlightRenderer.java # 渲染管线 + RenderPhase + Context
  ├── ItemRetrievalModClient.java  # Fabric API 包 + Category + WorldRenderEvents
  ├── ItemRetrievalMod.java        # FeatureFlags (已在 1.21.10 中正确)
  ├── SearchScanner.java           # 实体类差异 + 注释修复
  └── SearchTargetManager.java     # 注释修复

mc/1.21.10 → mc/1.21    (5 files)
  └── 同上（不含 SearchTargetManager）

mc/1.21.10 → mc/1.20.6  (5 files)
  └── 同上，额外：
      - SearchHighlightRenderer: VertexRendering → WorldRenderer + 手写 drawLine
      - SearchScanner: AbstractChestBoatEntity → ChestBoatEntity, getInventorySize → 15
```

---

## 新建适配版本指南

如需适配新的 Minecraft 版本，按以下步骤操作：

1. **创建分支**：`git checkout -b mc/X.Y.Z main`
2. **更新构建配置**：修改 `gradle.properties`、`settings.gradle`、`fabric.mod.json`
3. **尝试构建**：`./gradlew build`
4. **根据编译错误逐项修复**：
   - 检查是否缺少 `Click`/`KeyInput` → 降级为原始参数
   - 检查是否缺少 `RenderPipeline`/`BlendFunction` → 改用 `RenderLayer.getLines()` + `WorldRenderer.drawBox()`
   - 检查 Fabric API 包路径 → 改为 `.v1`
   - 检查 `FeatureFlags` → 使用 `VANILLA_FEATURES`
   - 检查 `VertexRendering` → 若不存在则手写
   - 检查实体类名 → 查阅目标版本的 Yarn 映射
5. **运行时测试**：`./gradlew runClient`，验证基本功能（面板打开、检索、高亮）

---

## 更新日志

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-06-07 | — | 初始文档，适配 1.21.4、1.21、1.20.6 |
