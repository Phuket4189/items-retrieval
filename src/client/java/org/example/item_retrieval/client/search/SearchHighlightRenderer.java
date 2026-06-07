package org.example.item_retrieval.client.search;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.example.item_retrieval.client.config.SearchRuntimeConfig;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * 检索命中高亮渲染器：
 * 绘制容器命中框、颜色标记和方向引导线。
 * 1.20.6 版本使用 WorldRenderer 绘制 + 简化的 RenderLayer。
 */
public final class SearchHighlightRenderer {

    private static final int NO_COLOR = SearchTargetManager.NO_COLOR;

    /** 直接使用原版无深度的线条渲染层。 */
    private static final RenderLayer HIGHLIGHT_LINES_NO_DEPTH = RenderLayer.getLines();

    /** 非持续模式下高亮默认持续时间（毫秒）。 */
    private final long highlightDurationMs;

    /** 同时显示的方向引导线数量上限。 */
    private final int maxDirectionGuideLines;

    /** 当前激活高亮集合。 */
    private final List<ActiveHighlight> activeHighlights = new ArrayList<>();

    private boolean highlightRenderDisabled = false;
    private boolean persistentHighlights = false;

    /**
     * @param highlightDurationMs 非持续模式下高亮持续时长（毫秒）。
     * @param maxDirectionGuideLines 方向引导线数量上限。
     */
    public SearchHighlightRenderer(long highlightDurationMs, int maxDirectionGuideLines) {
        this.highlightDurationMs = highlightDurationMs;
        this.maxDirectionGuideLines = maxDirectionGuideLines;
    }

    /** 清空当前所有高亮。 */
    public void clear() {
        activeHighlights.clear();
    }

    /**
     * 设置是否启用"持续高亮模式"。
     * 持续模式下不按 expiresAt 自动移除，而是等下一轮检索结果覆盖。
     */
    public void setPersistentHighlights(boolean persistentHighlights) {
        this.persistentHighlights = persistentHighlights;
    }

    /**
     * 根据检索命中结果刷新高亮列表。
     *
     * @param hits 命中容器列表。
     * @param expiresAtMs 该批高亮的过期时间。
     * @param colorResolver 目标物品到颜色的解析函数。
     */
    public void applyHighlights(List<SearchScanner.ContainerHit> hits, long expiresAtMs, ToIntFunction<Item> colorResolver) {
        highlightRenderDisabled = false;
        activeHighlights.clear();

        for (SearchScanner.ContainerHit hit : hits) {
            int primaryColor = selectPrimaryColor(hit.matchedTargetCounts(), colorResolver);
            int[] accentColors = selectAccentColors(hit.matchedTargetCounts(), primaryColor, colorResolver);

            activeHighlights.add(new ActiveHighlight(
                    hit.reference(),
                    new HighlightRenderInfo(
                            expiresAtMs,
                            primaryColor,
                            accentColors[0],
                            accentColors[1]
                    )
            ));
        }
    }

    /** 在世界渲染阶段绘制高亮。 */
    public void render(WorldRenderContext context) {
        if (highlightRenderDisabled || activeHighlights.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            activeHighlights.clear();
            return;
        }

        if (context.matrixStack() == null || context.consumers() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!persistentHighlights) {
            activeHighlights.removeIf(entry -> entry.renderInfo().expiresAtMs() <= now);
        }
        if (activeHighlights.isEmpty()) {
            return;
        }

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        List<ResolvedHighlight> resolvedHighlights = new ArrayList<>(activeHighlights.size());
        for (ActiveHighlight activeHighlight : activeHighlights) {
            ResolvedRenderTarget resolvedRenderTarget = resolveRenderTarget(activeHighlight.reference(), client);
            resolvedHighlights.add(new ResolvedHighlight(resolvedRenderTarget, activeHighlight.renderInfo()));
        }

        resolvedHighlights.sort(Comparator.comparingDouble(entry -> squaredDistanceToCamera(entry.renderTarget().guideTarget(), cameraPos)));

        try {
            for (int index = 0; index < resolvedHighlights.size(); index++) {
                ResolvedHighlight entry = resolvedHighlights.get(index);
                HighlightRenderInfo renderInfo = entry.renderInfo();
                long remainingMs = Math.max(0L, renderInfo.expiresAtMs() - now);
                float alpha = Math.max(0.25F, Math.min(0.92F, (float) remainingMs / (float) highlightDurationMs));

                int primaryColor = renderInfo.primaryColorArgb();
                float red = channelToFloat(primaryColor, 16);
                float green = channelToFloat(primaryColor, 8);
                float blue = channelToFloat(primaryColor, 0);

                Box localBox = entry.renderTarget().worldBox()
                        .offset(-cameraPos.x, -cameraPos.y, -cameraPos.z)
                        .expand(0.003D);

                // Re-acquire consumers for each layer use.
                // Custom layers are not always retained as fixed buffers, and reusing a stale
                // consumer after switching layers can throw "Not building".
                VertexConsumer throughWallLineConsumer = context.consumers().getBuffer(HIGHLIGHT_LINES_NO_DEPTH);
                drawBox(context.matrixStack(), throughWallLineConsumer, localBox, red, green, blue, Math.max(0.38F, alpha));
                drawAccentColorMarker(context, throughWallLineConsumer, localBox, renderInfo.secondaryColorArgb(), 0, alpha);
                drawAccentColorMarker(context, throughWallLineConsumer, localBox, renderInfo.tertiaryColorArgb(), 1, alpha);

                if (index < maxDirectionGuideLines) {
                    drawDirectionGuideLine(context, throughWallLineConsumer, cameraPos, entry.renderTarget().guideTarget(), primaryColor, alpha);
                }

                VertexConsumer depthLineConsumer = context.consumers().getBuffer(RenderLayer.getLines());
                drawBox(context.matrixStack(), depthLineConsumer, localBox.expand(0.001D), red, green, blue, Math.max(0.22F, alpha * 0.85F));
            }
        } catch (IllegalStateException renderError) {
            highlightRenderDisabled = true;
            activeHighlights.clear();
            System.err.println("[item-retrieval] Highlight rendering disabled due to render state error: " + renderError.getMessage());
        }
    }

    private static ResolvedRenderTarget resolveRenderTarget(SearchScanner.ContainerReference reference, MinecraftClient client) {
        if (reference.isEntity()) {
            Entity entity = client.world == null ? null : client.world.getEntityById(reference.entityId());
            if (entity != null) {
                Box entityBox = entity.getBoundingBox();
                return new ResolvedRenderTarget(entityBox, entityBox.getCenter());
            }
        }

        BlockPos blockPos = reference.anchorPos();
        return new ResolvedRenderTarget(new Box(blockPos), Vec3d.ofCenter(blockPos).add(0.0D, 0.35D, 0.0D));
    }

    private static void drawDirectionGuideLine(
            WorldRenderContext context,
            VertexConsumer lineConsumer,
            Vec3d cameraPos,
            Vec3d targetCenter,
            int colorArgb,
            float alpha
    ) {
        Vec3d direction = targetCenter.subtract(cameraPos);
        if (direction.lengthSquared() <= 0.04D) {
            return;
        }

        int guideColor = withAlpha(colorArgb, Math.max(56, Math.min(255, (int) (alpha * 205.0F))));
        drawLine(context.matrixStack(), lineConsumer, 0.0F, 0.0F, 0.0F, (float) direction.x, (float) direction.y, (float) direction.z, guideColor);
    }

    private static void drawAccentColorMarker(
            WorldRenderContext context,
            VertexConsumer lineConsumer,
            Box localBox,
            int accentColorArgb,
            int markerIndex,
            float alpha
    ) {
        if (accentColorArgb == NO_COLOR) {
            return;
        }

        double markerWidth = 0.20D;
        double markerHeight = 0.08D;
        double markerGap = 0.04D;
        double minX = localBox.minX + 0.08D + markerIndex * (markerWidth + markerGap);
        double minY = localBox.maxY + 0.02D;
        double minZ = localBox.minZ + 0.08D;

        Box markerBox = new Box(minX, minY, minZ, minX + markerWidth, minY + markerHeight, minZ + markerWidth);
        drawBox(
                context.matrixStack(),
                lineConsumer,
                markerBox,
                channelToFloat(accentColorArgb, 16),
                channelToFloat(accentColorArgb, 8),
                channelToFloat(accentColorArgb, 0),
                Math.max(0.24F, alpha * 0.72F)
        );
    }

    // ===== 1.20.6 通用绘制工具（替代 VertexRendering） =====

    private static void drawBox(MatrixStack matrices, VertexConsumer consumer, Box box, float r, float g, float b, float a) {
        WorldRenderer.drawBox(matrices, consumer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void drawLine(MatrixStack matrices, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        Matrix4f pos = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;
        consumer.vertex(pos, x1, y1, z1).color(r, g, b, a).normal(normal, 0.0F, 1.0F, 0.0F);
        consumer.vertex(pos, x2, y2, z2).color(r, g, b, a).normal(normal, 0.0F, 1.0F, 0.0F);
    }

    private static int selectPrimaryColor(Map<Item, Integer> matchedTargetCounts, ToIntFunction<Item> colorResolver) {
        Item primaryItem = null;
        int maxCount = -1;

        for (Map.Entry<Item, Integer> entry : matchedTargetCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                primaryItem = entry.getKey();
            }
        }

        return primaryItem == null ? NO_COLOR : colorResolver.applyAsInt(primaryItem);
    }

    private static int[] selectAccentColors(Map<Item, Integer> matchedTargetCounts, int primaryColor, ToIntFunction<Item> colorResolver) {
        List<Map.Entry<Item, Integer>> sortedMatches = new ArrayList<>(matchedTargetCounts.entrySet());
        sortedMatches.sort((left, right) -> Integer.compare(right.getValue(), left.getValue()));

        int secondary = NO_COLOR;
        int tertiary = NO_COLOR;

        for (Map.Entry<Item, Integer> entry : sortedMatches) {
            int candidate = colorResolver.applyAsInt(entry.getKey());
            if (candidate == primaryColor || candidate == secondary) {
                continue;
            }

            if (secondary == NO_COLOR) {
                secondary = candidate;
                continue;
            }

            tertiary = candidate;
            break;
        }

        return new int[]{secondary, tertiary};
    }

    private static float channelToFloat(int colorArgb, int shift) {
        return ((colorArgb >> shift) & 0xFF) / 255.0F;
    }

    private static int withAlpha(int colorArgb, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (clampedAlpha << 24) | (colorArgb & 0x00FFFFFF);
    }

    private static double squaredDistanceToCamera(Vec3d pos, Vec3d cameraPos) {
        double dx = pos.x - cameraPos.x;
        double dy = pos.y - cameraPos.y;
        double dz = pos.z - cameraPos.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private record ActiveHighlight(SearchScanner.ContainerReference reference, HighlightRenderInfo renderInfo) {
    }

    private record ResolvedRenderTarget(Box worldBox, Vec3d guideTarget) {
    }

    private record ResolvedHighlight(ResolvedRenderTarget renderTarget, HighlightRenderInfo renderInfo) {
    }

    private record HighlightRenderInfo(long expiresAtMs, int primaryColorArgb, int secondaryColorArgb, int tertiaryColorArgb) {
    }
}
