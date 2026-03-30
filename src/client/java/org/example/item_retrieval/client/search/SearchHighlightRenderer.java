package org.example.item_retrieval.client.search;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.example.item_retrieval.client.config.SearchRuntimeConfig;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.ToIntFunction;

/**
 * 妫€绱㈠懡涓珮浜覆鏌撳櫒锛?
 * 缁樺埗瀹瑰櫒鍛戒腑妗嗐€侀鑹叉爣璁板拰鏂瑰悜寮曞绾裤€?
 */
public final class SearchHighlightRenderer {

    private static final int NO_COLOR = SearchTargetManager.NO_COLOR;

    /**
     * 鍗曠嫭鏋勫缓涓€鏉℃棤娣卞害娴嬭瘯鐨勭嚎妗嗙绾匡紝璁╁鍣?瀹炰綋鏍囪鍙互闅斿鍙銆?
     * 淇濈暀鍘熺増 lines snippet 鐨勭嚎瀹姐€佹贩鍚堝拰椤剁偣鏍煎紡閰嶇疆锛屽彧瑕嗙洊娣卞害鐩稿叧鐘舵€併€?
     */
    private static final RenderPipeline HIGHLIGHT_LINES_NO_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
        .withLocation(Identifier.of(SearchRuntimeConfig.MOD_ID, "pipeline/search_highlight_lines_no_depth"))
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, com.mojang.blaze3d.vertex.VertexFormat.DrawMode.LINES)
        .build();

    private static final RenderLayer HIGHLIGHT_LINES_NO_DEPTH = RenderLayer.of(
        "search_highlight_lines_no_depth",
        1536,
        HIGHLIGHT_LINES_NO_DEPTH_PIPELINE,
        RenderLayer.MultiPhaseParameters.builder()
            .lineWidth(new RenderPhase.LineWidth(OptionalDouble.empty()))
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .target(RenderPhase.ITEM_ENTITY_TARGET)
            .build(false)
    );

    /** 闈炴寔缁ā寮忎笅楂樹寒榛樿鎸佺画鏃堕棿锛堟绉掞級銆?*/
    private final long highlightDurationMs;

    /** 鍚屾椂鏄剧ず鐨勬柟鍚戝紩瀵肩嚎鏁伴噺涓婇檺銆?*/
    private final int maxDirectionGuideLines;

    /** 褰撳墠婵€娲婚珮浜泦鍚堛€?*/
    private final List<ActiveHighlight> activeHighlights = new ArrayList<>();

    private boolean highlightRenderDisabled = false;
    private boolean persistentHighlights = false;

    /**
     * @param highlightDurationMs 闈炴寔缁ā寮忎笅楂樹寒鎸佺画鏃堕暱锛堟绉掞級銆?
     * @param maxDirectionGuideLines 鏂瑰悜寮曞绾挎暟閲忎笂闄愩€?
     */
    public SearchHighlightRenderer(long highlightDurationMs, int maxDirectionGuideLines) {
        this.highlightDurationMs = highlightDurationMs;
        this.maxDirectionGuideLines = maxDirectionGuideLines;
    }

    /** 娓呯┖褰撳墠鎵€鏈夐珮浜€?*/
    public void clear() {
        activeHighlights.clear();
    }

    /**
     * 璁剧疆鏄惁鍚敤鈥滄寔缁珮浜ā寮忊€濄€?
     * 鎸佺画妯″紡涓嬩笉鎸?expiresAt 鑷姩绉婚櫎锛岃€屾槸绛変笅涓€杞绱㈢粨鏋滆鐩栥€?
     */
    public void setPersistentHighlights(boolean persistentHighlights) {
        this.persistentHighlights = persistentHighlights;
    }

    /**
     * 鏍规嵁妫€绱㈠懡涓粨鏋滃埛鏂伴珮浜垪琛ㄣ€?
     *
     * @param hits 鍛戒腑瀹瑰櫒鍒楄〃銆?
     * @param expiresAtMs 璇ユ壒楂樹寒鐨勮繃鏈熸椂闂淬€?
     * @param colorResolver 鐩爣鐗╁搧鍒伴鑹茬殑瑙ｆ瀽鍑芥暟銆?
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

    /** 鍦ㄤ笘鐣屾覆鏌撻樁娈电粯鍒堕珮浜€?*/
    public void render(WorldRenderContext context) {
        if (highlightRenderDisabled || activeHighlights.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            activeHighlights.clear();
            return;
        }

        if (context.matrices() == null || context.consumers() == null) {
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
                VertexRendering.drawBox(context.matrices().peek(), throughWallLineConsumer, localBox, red, green, blue, Math.max(0.38F, alpha));
                drawAccentColorMarker(context, throughWallLineConsumer, localBox, renderInfo.secondaryColorArgb(), 0, alpha);
                drawAccentColorMarker(context, throughWallLineConsumer, localBox, renderInfo.tertiaryColorArgb(), 1, alpha);

                if (index < maxDirectionGuideLines) {
                    drawDirectionGuideLine(context, throughWallLineConsumer, cameraPos, entry.renderTarget().guideTarget(), primaryColor, alpha);
                }

                VertexConsumer depthLineConsumer = context.consumers().getBuffer(RenderLayer.getLines());
                VertexRendering.drawBox(context.matrices().peek(), depthLineConsumer, localBox.expand(0.001D), red, green, blue, Math.max(0.22F, alpha * 0.85F));
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
        VertexRendering.drawVector(context.matrices(), lineConsumer, new Vector3f(0.0F, 0.0F, 0.0F), direction, guideColor);
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
        VertexRendering.drawBox(
                context.matrices().peek(),
                lineConsumer,
                markerBox,
                channelToFloat(accentColorArgb, 16),
                channelToFloat(accentColorArgb, 8),
                channelToFloat(accentColorArgb, 0),
                Math.max(0.24F, alpha * 0.72F)
        );
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
