package org.example.item_retrieval.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.example.item_retrieval.client.config.SearchRuntimeConfig;
import org.example.item_retrieval.client.runtime.ContinuousSearchScheduler;
import org.example.item_retrieval.client.screen.SearchScreen;
import org.example.item_retrieval.client.search.SearchHighlightRenderer;
import org.example.item_retrieval.client.search.SearchScanner;
import org.example.item_retrieval.client.search.SearchTargetManager;
import org.example.item_retrieval.client.search.service.SearchRuntimeService;
import org.example.item_retrieval.screen.SearchScreenHandler;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端入口：
 * 负责按键注册、界面打开、持续检索开关以及对 UI 提供静态调用门面。
 */
public class ItemRetrievalModClient implements ClientModInitializer {

    // 目标与结果状态管理（目标池、启用状态、颜色、结果分页等）。
    private static final SearchTargetManager SEARCH_TARGETS = new SearchTargetManager(
        SearchRuntimeConfig.SEARCH_OPTION_CAPACITY,
        SearchRuntimeConfig.RESULT_DISPLAY_SLOTS
    );

    // 检索运行服务：负责扫描、冷却、结果落地与高亮更新。
    private static final SearchRuntimeService SEARCH_RUNTIME = new SearchRuntimeService(
        SEARCH_TARGETS,
        new SearchScanner(
            SearchRuntimeConfig.SEARCH_RADIUS_BLOCKS,
            SearchRuntimeConfig.MAX_RESULT_HITS,
            SearchRuntimeConfig.MAX_CONTAINERS_SCANNED,
            SearchRuntimeConfig.MAX_ENTITY_CONTAINERS_SCANNED,
            SearchRuntimeConfig.MAX_NESTED_CONTAINER_DEPTH
        ),
        new SearchHighlightRenderer(
            SearchRuntimeConfig.HIGHLIGHT_DURATION_MS,
            SearchRuntimeConfig.MAX_DIRECTION_GUIDE_LINES
        ),
        SearchRuntimeConfig.SEARCH_COOLDOWN_MS,
        SearchRuntimeConfig.HIGHLIGHT_DURATION_MS
    );

    // 持续检索调度器：只负责轮询节奏。
    private static final ContinuousSearchScheduler CONTINUOUS_SEARCH_SCHEDULER =
        new ContinuousSearchScheduler(SearchRuntimeConfig.CONTINUOUS_SEARCH_INTERVAL_MS);

    public static final Category CUSTOM_CATEGORY = new Category(Identifier.of(SearchRuntimeConfig.MOD_ID, "general"));

    private static KeyBinding openGuiKey;
    private static KeyBinding runSearchKey;

    @Override
    public void onInitializeClient() {
        // U：打开检索面板
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.items-retrieval.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                CUSTOM_CATEGORY
        ));

        // O：切换持续检索模式
        runSearchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.items-retrieval.run_search",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CUSTOM_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                openSearchScreen(client);
            }

            while (runSearchKey.wasPressed()) {
                toggleContinuousSearch(client);
            }

            if (CONTINUOUS_SEARCH_SCHEDULER.isEnabled()) {
                tickContinuousSearch(client);
            }
        });

        WorldRenderEvents.END_MAIN.register(SEARCH_RUNTIME.getHighlightRenderer()::render);

        System.out.println("items-retrieval 客户端初始化完成！");
    }

    /**
     * 打开检索主界面。
     * @param client 客户端实例。
     */
    private static void openSearchScreen(MinecraftClient client) {
        if (client.player == null || client.currentScreen instanceof SearchScreen) {
            return;
        }

        client.setScreen(new SearchScreen(
                new SearchScreenHandler(
                        0,
                        client.player.getInventory(),
                        SEARCH_TARGETS.getSearchOptionsInventory(),
                        SEARCH_TARGETS.getSearchResultsInventory()
                ),
                client.player.getInventory(),
                Text.literal("物品检索面板")
        ));
    }

    /**
     * 单次检索入口（面板按钮与外部调用复用）。
     * @param client 客户端实例。
     * @param notifyPlayer 是否提示玩家消息。
     */
    public static void runNearbySearch(MinecraftClient client, boolean notifyPlayer) {
        SEARCH_RUNTIME.runNearbySearch(client, notifyPlayer);
    }

    /**
     * 切换持续检索状态，并同步高亮是否持久显示。
     * @param client 客户端实例。
     */
    private static void toggleContinuousSearch(MinecraftClient client) {
        boolean enabled = CONTINUOUS_SEARCH_SCHEDULER.toggle();
        SEARCH_RUNTIME.getHighlightRenderer().setPersistentHighlights(enabled);

        if (enabled) {
            CONTINUOUS_SEARCH_SCHEDULER.triggerImmediateNextTick();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("持续检索开启（每10秒自动刷新）"), true);
            }
            return;
        }

        if (client.player != null) {
            client.player.sendMessage(Text.literal("持续检索关闭"), true);
        }
    }

    /**
     * 客户端每 tick 调用：若达到轮询时间则执行一次静默检索。
     * @param client 客户端实例。
     */
    private static void tickContinuousSearch(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!CONTINUOUS_SEARCH_SCHEDULER.shouldRunNow(now)) {
            return;
        }

        CONTINUOUS_SEARCH_SCHEDULER.markExecuted(now);
        SEARCH_RUNTIME.runNearbySearchWithoutCooldown(client, false);
    }

    // ===== 结果与高亮控制 =====

    public static void clearSearchResultsAndHighlights() {
        SEARCH_RUNTIME.clearSearchResultsAndHighlights();
    }

    // ===== 目标池操作 =====

    public static boolean addSearchTarget(ItemStack sourceStack) {
        return SEARCH_TARGETS.addSearchTarget(sourceStack);
    }

    public static boolean addSearchTarget(ItemStack sourceStack, int colorArgb) {
        return SEARCH_TARGETS.addSearchTarget(sourceStack, colorArgb);
    }

    public static boolean removeSearchTarget(int index) {
        return SEARCH_TARGETS.removeSearchTarget(index);
    }

    public static int getSearchTargetColor(int slotIndex) {
        return SEARCH_TARGETS.getSearchTargetColor(slotIndex);
    }

    public static int getPendingTargetColor() {
        return SEARCH_TARGETS.getPendingTargetColor();
    }

    public static int cyclePendingTargetColor() {
        return SEARCH_TARGETS.cyclePendingTargetColor();
    }

    public static int cycleSearchTargetColor(int slotIndex) {
        return SEARCH_TARGETS.cycleSearchTargetColor(slotIndex);
    }

    public static boolean isSearchTargetEnabled(int slotIndex) {
        return SEARCH_TARGETS.isSearchTargetEnabled(slotIndex);
    }

    public static boolean toggleSearchTargetEnabled(int slotIndex) {
        return SEARCH_TARGETS.toggleSearchTargetEnabled(slotIndex);
    }

    public static int getSearchTargetCount() {
        return SEARCH_TARGETS.getSearchTargetCount();
    }

    public static boolean isContinuousSearchEnabled() {
        return CONTINUOUS_SEARCH_SCHEDULER.isEnabled();
    }

    public static int getEnabledSearchTargetCount() {
        return SEARCH_TARGETS.getEnabledSearchTargetCount();
    }

    // ===== 结果分页读取 =====

    public static int getSearchOptionCapacity() {
        return SearchRuntimeConfig.SEARCH_OPTION_CAPACITY;
    }

    public static String formatColorHex(int colorArgb) {
        return SEARCH_TARGETS.formatColorHex(colorArgb);
    }

    public static Inventory getSearchOptionsInventory() {
        return SEARCH_TARGETS.getSearchOptionsInventory();
    }

    public static int getSearchResultCount() {
        return SEARCH_TARGETS.getSearchResultCount();
    }

    public static int getSearchResultRowOffset() {
        return SEARCH_TARGETS.getSearchResultRowOffset();
    }

    public static int getSearchResultMaxRowOffset() {
        return SEARCH_TARGETS.getSearchResultMaxRowOffset();
    }

    public static boolean scrollSearchResultsByRows(int rowDelta) {
        return SEARCH_TARGETS.scrollSearchResultsByRows(rowDelta);
    }

    public static boolean setSearchResultRowOffset(int rowOffset) {
        return SEARCH_TARGETS.setSearchResultRowOffset(rowOffset);
    }

    private static int clampSearchRadiusBlocks(int requestedRadius) {
        return Math.max(
                SearchRuntimeConfig.SEARCH_RADIUS_MIN_BLOCKS,
                Math.min(SearchRuntimeConfig.SEARCH_RADIUS_MAX_BLOCKS, requestedRadius)
        );
    }

    public static int getSearchRadiusBlocks() {
        return SEARCH_RUNTIME.getSearchRadiusBlocks();
    }

    public static int setSearchRadiusBlocks(int requestedRadius) {
        return SEARCH_RUNTIME.setSearchRadiusBlocks(clampSearchRadiusBlocks(requestedRadius));
    }

    public static int adjustSearchRadiusBlocks(int delta) {
        return setSearchRadiusBlocks(getSearchRadiusBlocks() + delta);
    }
}
