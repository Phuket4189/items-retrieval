package org.example.item_retrieval.client.config;

import org.example.item_retrieval.ItemRetrievalMod;

/**
 * 检索系统运行参数。
 * 统一放在该类，便于后续调参和排查。
 */
public final class SearchRuntimeConfig {

    /** 模组 ID（用于按键分类、资源路径等标识）。 */
    public static final String MOD_ID = ItemRetrievalMod.MOD_ID;

    /** 检索半径最小值（方块）。 */
    public static final int SEARCH_RADIUS_MIN_BLOCKS = 1;

    /** 检索半径最大值（方块）。 */
    public static final int SEARCH_RADIUS_MAX_BLOCKS = 128;

    /** 默认检索半径（方块）。 */
    public static final int SEARCH_RADIUS_BLOCKS = 12;

    /** 左侧目标池容量（可添加多少目标物品）。 */
    public static final int SEARCH_OPTION_CAPACITY = 128;

    /** 中央结果展示槽位数量（7 x 9 = 63）。 */
    public static final int RESULT_DISPLAY_SLOTS = 63;

    /** 单次扫描最多记录的命中容器数。 */
    public static final int MAX_RESULT_HITS = 2_048;

    /** 单次扫描最多遍历的容器数，避免极端卡顿。 */
    public static final int MAX_CONTAINERS_SCANNED = 768;

    /** 单次扫描最多遍历的实体容器数，避免实体密集区卡顿。 */
    public static final int MAX_ENTITY_CONTAINERS_SCANNED = 192;

    /** 嵌套容器递归深度上限（例如潜影盒套娃）。 */
    public static final int MAX_NESTED_CONTAINER_DEPTH = 6;

    /** 手动检索冷却（毫秒）。 */
    public static final long SEARCH_COOLDOWN_MS = 400L;

    /** 持续检索轮询间隔（毫秒）。 */
    public static final long CONTINUOUS_SEARCH_INTERVAL_MS = 10_000L;

    /** 非持续模式下，高亮框默认存活时间（毫秒）。 */
    public static final long HIGHLIGHT_DURATION_MS = 8_000L;

    /** 同时绘制的方向引导线数量上限。 */
    public static final int MAX_DIRECTION_GUIDE_LINES = 6;

    private SearchRuntimeConfig() {
        // 配置类不应被实例化。
    }
}
