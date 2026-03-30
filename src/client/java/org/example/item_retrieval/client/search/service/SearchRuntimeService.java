package org.example.item_retrieval.client.search.service;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.example.item_retrieval.client.search.SearchHighlightRenderer;
import org.example.item_retrieval.client.search.SearchScanner;
import org.example.item_retrieval.client.search.SearchTargetManager;

import java.util.List;
import java.util.Set;

/**
 * 检索运行服务：
 * 负责检索入口、冷却控制、单机服务端扫描切换、结果与高亮应用。
 */
public final class SearchRuntimeService {

    private final SearchTargetManager targetManager;
    private final SearchScanner scanner;
    private final SearchHighlightRenderer highlightRenderer;

    /** 手动检索冷却（毫秒）。 */
    private final long searchCooldownMs;

    /** 非持续模式下高亮存活时长（毫秒）。 */
    private final long highlightDurationMs;

    /** 上次检索时间（毫秒）。 */
    private long lastSearchTimeMs = 0L;

    /**
     * @param targetManager 目标管理器（维护目标池与结果槽内容）。
     * @param scanner 检索扫描器（执行容器扫描）。
     * @param highlightRenderer 高亮渲染器（绘制容器框与引导线）。
     * @param searchCooldownMs 手动检索冷却（毫秒）。
     * @param highlightDurationMs 高亮持续时长（毫秒）。
     */
    public SearchRuntimeService(
            SearchTargetManager targetManager,
            SearchScanner scanner,
            SearchHighlightRenderer highlightRenderer,
            long searchCooldownMs,
            long highlightDurationMs
    ) {
        this.targetManager = targetManager;
        this.scanner = scanner;
        this.highlightRenderer = highlightRenderer;
        this.searchCooldownMs = searchCooldownMs;
        this.highlightDurationMs = highlightDurationMs;
    }

    /**
     * 普通检索入口（带冷却）。
     */
    public void runNearbySearch(MinecraftClient client, boolean notifyPlayer) {
        runNearbySearchInternal(client, notifyPlayer, true);
    }

    /**
     * 轮询检索入口（不走手动冷却，供持续检索模式调用）。
     */
    public void runNearbySearchWithoutCooldown(MinecraftClient client, boolean notifyPlayer) {
        runNearbySearchInternal(client, notifyPlayer, false);
    }

    /**
     * 清空结果与高亮。
     */
    public void clearSearchResultsAndHighlights() {
        targetManager.clearSearchResults();
        highlightRenderer.clear();
    }

    public SearchHighlightRenderer getHighlightRenderer() {
        return highlightRenderer;
    }

    public int getSearchRadiusBlocks() {
        return scanner.getSearchRadiusBlocks();
    }

    public int setSearchRadiusBlocks(int searchRadiusBlocks) {
        return scanner.setSearchRadiusBlocks(searchRadiusBlocks);
    }

    private void runNearbySearchInternal(MinecraftClient client, boolean notifyPlayer, boolean applyCooldown) {
        if (client.player == null || client.world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (applyCooldown) {
            long cooldownLeftMs = searchCooldownMs - (now - lastSearchTimeMs);
            if (cooldownLeftMs > 0L) {
                if (notifyPlayer) {
                    client.player.sendMessage(Text.literal("检索冷却中：" + cooldownLeftMs + "ms"), true);
                }
                return;
            }
        }
        lastSearchTimeMs = now;

        Set<Item> targetItems = targetManager.collectTargetItems();
        if (targetItems.isEmpty()) {
            clearSearchResultsAndHighlights();
            if (notifyPlayer) {
                if (targetManager.getSearchTargetCount() > 0 && targetManager.getEnabledSearchTargetCount() == 0) {
                    client.player.sendMessage(Text.literal("当前目标全部为禁用状态，请先启用至少 1 个目标。"), true);
                } else {
                    client.player.sendMessage(Text.literal("请先添加至少 1 个检索目标物品。"), true);
                }
            }
            return;
        }

        IntegratedServer integratedServer = client.getServer();
        if (integratedServer != null) {
            runNearbySearchOnIntegratedServer(client, integratedServer, targetItems, notifyPlayer, now);
            return;
        }

        SearchScanner.SearchComputation computation = scanner.scanNearbyContainers(
                client.world::getBlockEntity,
            searchBounds -> client.world.getOtherEntities(null, searchBounds, entity -> true),
                client.player.getBlockPos(),
                targetItems
        );
        applySearchOutputs(computation.hits(), now + highlightDurationMs);

        if (notifyPlayer) {
            client.player.sendMessage(Text.literal(
                    "检索完成：扫描容器 " + computation.scannedContainerCount()
                            + "（方块 " + computation.scannedBlockContainerCount()
                            + "，实体 " + computation.scannedEntityContainerCount() + "）"
                            + " 个，命中 " + computation.hits().size()
                            + " 个，匹配物品 " + computation.totalMatchedCount() + " 个。"
            ), true);
            client.player.sendMessage(Text.literal("提示：当前使用客户端可见数据检索，多人服务器下结果可能不完整。"), true);
        }
    }

    private void runNearbySearchOnIntegratedServer(
            MinecraftClient client,
            IntegratedServer server,
            Set<Item> targetItems,
            boolean notifyPlayer,
            long searchStartedAtMs
    ) {
        if (client.player == null || client.world == null) {
            return;
        }

        BlockPos center = client.player.getBlockPos().toImmutable();
        Set<Item> targetSnapshot = Set.copyOf(targetItems);
        var worldKey = client.world.getRegistryKey();

        server.execute(() -> {
            ServerWorld serverWorld = server.getWorld(worldKey);
            if (serverWorld == null) {
                if (notifyPlayer) {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("检索失败：未找到服务端世界。"), true);
                        }
                    });
                }
                return;
            }

            SearchScanner.SearchComputation computation = scanner.scanNearbyContainers(
                    serverWorld::getBlockEntity,
                    searchBounds -> serverWorld.getOtherEntities(null, searchBounds, entity -> true),
                    center,
                    targetSnapshot
            );
            List<SearchScanner.ContainerHit> hitsForClient = scanner.copyHits(computation.hits());

            client.execute(() -> {
                if (client.player == null) {
                    return;
                }

                applySearchOutputs(hitsForClient, searchStartedAtMs + highlightDurationMs);

                if (notifyPlayer) {
                    client.player.sendMessage(Text.literal(
                            "检索完成：扫描容器 " + computation.scannedContainerCount()
                                    + "（方块 " + computation.scannedBlockContainerCount()
                                    + "，实体 " + computation.scannedEntityContainerCount() + "）"
                                    + " 个，命中 " + hitsForClient.size()
                                    + " 个，匹配物品 " + computation.totalMatchedCount() + " 个。"
                    ), true);
                }
            });
        });
    }

    private void applySearchOutputs(List<SearchScanner.ContainerHit> hits, long expiresAtMs) {
        targetManager.applySearchResults(hits);
        highlightRenderer.applyHighlights(hits, expiresAtMs, targetManager::resolveTargetColor);
    }
}