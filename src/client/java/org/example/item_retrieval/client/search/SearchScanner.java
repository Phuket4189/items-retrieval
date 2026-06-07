package org.example.item_retrieval.client.search;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.vehicle.AbstractChestBoatEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Field;

/**
 * 容器扫描器：
 * 在指定半径内遍历容器，统计目标物品命中情况，支持嵌套容器递归统计。
 */
public final class SearchScanner {

    /** 马/驴真实背包字段缓存（AbstractHorseEntity#items）。 */
    private static Field donkeyInventoryField;

    /** 检索半径（方块）。 */
    private volatile int searchRadiusBlocks;

    /** 单次最多记录多少个命中容器。 */
    private final int maxResultHits;

    /** 单次最多扫描方块容器数量，防止大范围卡顿。 */
    private final int maxBlockContainersScanned;

    /** 单次最多扫描实体容器数量，防止实体密集区卡顿。 */
    private final int maxEntityContainersScanned;

    /** 嵌套容器递归层级上限。 */
    private final int maxNestedContainerDepth;

    /**
     * @param searchRadiusBlocks 检索半径（方块）。
     * @param maxResultHits 单次最多记录的命中容器数。
     * @param maxBlockContainersScanned 单次最多扫描方块容器数。
     * @param maxEntityContainersScanned 单次最多扫描实体容器数。
     * @param maxNestedContainerDepth 嵌套容器递归深度上限。
     */
    public SearchScanner(
            int searchRadiusBlocks,
            int maxResultHits,
            int maxBlockContainersScanned,
            int maxEntityContainersScanned,
            int maxNestedContainerDepth
    ) {
        this.searchRadiusBlocks = Math.max(1, searchRadiusBlocks);
        this.maxResultHits = maxResultHits;
        this.maxBlockContainersScanned = maxBlockContainersScanned;
        this.maxEntityContainersScanned = maxEntityContainersScanned;
        this.maxNestedContainerDepth = maxNestedContainerDepth;
    }

    public int getSearchRadiusBlocks() {
        return searchRadiusBlocks;
    }

    public int setSearchRadiusBlocks(int searchRadiusBlocks) {
        int clamped = Math.max(1, searchRadiusBlocks);
        this.searchRadiusBlocks = clamped;
        return clamped;
    }

    /**
     * 扫描玩家附近方块容器并生成检索命中结果。
     *
     * @param blockEntityLookup 方块实体查询函数。
     * @param center 扫描中心坐标（通常为玩家坐标）。
     * @param targets 目标物品集合。
     * @return 包含命中列表、扫描容器数、匹配总数的计算结果。
     */
    public SearchComputation scanNearbyContainers(BlockEntityLookup blockEntityLookup, BlockPos center, Set<Item> targets) {
        return scanNearbyContainers(blockEntityLookup, searchBounds -> List.of(), center, targets);
    }

    /**
     * 扫描玩家附近"方块容器 + 实体容器"并生成检索命中结果。
     *
     * @param blockEntityLookup 方块实体查询函数。
     * @param nearbyEntityLookup 实体查询函数（传入球形半径外包盒）。
     * @param center 扫描中心坐标（通常为玩家坐标）。
     * @param targets 目标物品集合。
     * @return 包含命中列表、扫描容器数、匹配总数的计算结果。
     */
    public SearchComputation scanNearbyContainers(
            BlockEntityLookup blockEntityLookup,
            NearbyEntityLookup nearbyEntityLookup,
            BlockPos center,
            Set<Item> targets
    ) {
        int radius = searchRadiusBlocks;
        List<ContainerHit> hits = new ArrayList<>();
        int scannedBlockContainers = 0;
        int scannedEntityContainers = 0;
        int totalMatchedCount = 0;
        int radiusSq = radius * radius;

        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz > radiusSq) {
                continue;
            }

            BlockEntity blockEntity = blockEntityLookup.get(pos);
            if (!(blockEntity instanceof Inventory inventory)) {
                continue;
            }

            scannedBlockContainers++;
            if (scannedBlockContainers > maxBlockContainersScanned) {
                break;
            }

            InventoryMatch inventoryMatch = summarizeInventoryMatch(inventory, targets);
            if (inventoryMatch.totalCount() <= 0) {
                continue;
            }

            hits.add(new ContainerHit(
                    ContainerReference.forBlock(pos),
                    inventoryMatch.displayStack(),
                    inventoryMatch.totalCount(),
                    inventoryMatch.matchedTargetCounts()
            ));
            totalMatchedCount += inventoryMatch.totalCount();

            if (hits.size() >= maxResultHits) {
                break;
            }
        }

        if (hits.size() < maxResultHits) {
            Box searchBounds = new Box(center).expand(radius + 1.5D);
            Vec3d centerVec = Vec3d.ofCenter(center);
            double entityRadiusSq = (double) radius * (double) radius;

            for (Entity entity : nearbyEntityLookup.get(searchBounds)) {
                if (!isSupportedEntityContainer(entity)) {
                    continue;
                }

                if (entity.squaredDistanceTo(centerVec) > entityRadiusSq) {
                    continue;
                }

                scannedEntityContainers++;
                if (scannedEntityContainers > maxEntityContainersScanned) {
                    break;
                }

                InventoryMatch inventoryMatch = summarizeEntityInventoryMatch(entity, targets);
                if (inventoryMatch.totalCount() <= 0) {
                    continue;
                }

                hits.add(new ContainerHit(
                        ContainerReference.forEntity(entity.getId(), entity.getBlockPos()),
                        inventoryMatch.displayStack(),
                        inventoryMatch.totalCount(),
                        inventoryMatch.matchedTargetCounts()
                ));
                totalMatchedCount += inventoryMatch.totalCount();

                if (hits.size() >= maxResultHits) {
                    break;
                }
            }
        }

        return new SearchComputation(hits, scannedBlockContainers, scannedEntityContainers, totalMatchedCount);
    }

    /**
     * 对命中结果做深拷贝，避免跨线程传递时引用共享。
     */
    public List<ContainerHit> copyHits(List<ContainerHit> hits) {
        List<ContainerHit> copied = new ArrayList<>(hits.size());
        for (ContainerHit hit : hits) {
            ItemStack stackCopy = hit.displayStack().isEmpty() ? ItemStack.EMPTY : hit.displayStack().copy();
            ContainerReference copiedReference = new ContainerReference(
                    hit.reference().anchorPos().toImmutable(),
                    hit.reference().entityId()
            );
            copied.add(new ContainerHit(
                    copiedReference,
                    stackCopy,
                    hit.totalMatchedCount(),
                    Map.copyOf(hit.matchedTargetCounts())
            ));
        }
        return copied;
    }

    /** 汇总单个容器中目标物品匹配情况。 */
    private InventoryMatch summarizeInventoryMatch(Inventory inventory, Set<Item> targets) {
        return summarizeContainerMatch(inventory.size(), inventory::getStack, targets);
    }

    /** 汇总实体容器中目标物品匹配情况。 */
    private InventoryMatch summarizeEntityInventoryMatch(Entity entity, Set<Item> targets) {
        if (entity instanceof Inventory inventory && (entity instanceof StorageMinecartEntity || entity instanceof AbstractChestBoatEntity)) {
            return summarizeInventoryMatch(inventory, targets);
        }

        if (entity instanceof AbstractDonkeyEntity donkeyEntity && donkeyEntity.hasChest()) {
            Inventory donkeyInventory = resolveDonkeyInventory(donkeyEntity);
            if (donkeyInventory != null) {
                return summarizeInventoryMatch(donkeyInventory, targets);
            }

            // 兜底：若反射失败则回退到映射槽位读取（可能覆盖不完整）。
            return summarizeContainerMatch(
                    donkeyEntity.getInventorySize(),
                    slot -> donkeyEntity.getStackReference(slot).get(),
                    targets
            );
        }

        return InventoryMatch.EMPTY;
    }

    /**
     * 读取马/驴真实库存（AbstractHorseEntity#items）。
     * 1.21.10 中 getStackReference 使用映射槽位，不等于真实库存下标。
     */
    private static Inventory resolveDonkeyInventory(AbstractDonkeyEntity donkeyEntity) {
        Field cachedField = donkeyInventoryField;
        if (cachedField != null) {
            Inventory inventory = readInventoryField(cachedField, donkeyEntity);
            if (inventory != null) {
                return inventory;
            }
        }

        Field resolvedField = findDonkeyInventoryField(donkeyEntity.getClass());
        if (resolvedField == null) {
            return null;
        }

        try {
            resolvedField.setAccessible(true);
        } catch (RuntimeException setAccessibleError) {
            return null;
        }

        Inventory resolvedInventory = readInventoryField(resolvedField, donkeyEntity);
        if (resolvedInventory != null) {
            donkeyInventoryField = resolvedField;
        }
        return resolvedInventory;
    }

    private static Inventory readInventoryField(Field field, AbstractDonkeyEntity donkeyEntity) {
        try {
            Object value = field.get(donkeyEntity);
            return value instanceof Inventory inventory ? inventory : null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static Field findDonkeyInventoryField(Class<?> startType) {
        Class<?> currentType = startType;
        Field fallbackField = null;

        while (currentType != null) {
            for (Field field : currentType.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (SimpleInventory.class.isAssignableFrom(fieldType)) {
                    return field;
                }

                if (fallbackField == null && Inventory.class.isAssignableFrom(fieldType)) {
                    fallbackField = field;
                }
            }
            currentType = currentType.getSuperclass();
        }

        return fallbackField;
    }

    /** 按"槽位读取函数"汇总容器匹配情况。 */
    private InventoryMatch summarizeContainerMatch(int slotCount, StackLookup stackLookup, Set<Item> targets) {
        int total = 0;
        ItemStack display = ItemStack.EMPTY;
        Map<Item, Integer> matchedTargetCounts = new HashMap<>();

        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = stackLookup.get(slot);
            StackMatch stackMatch = summarizeStackMatch(stack, targets, 0);
            total += stackMatch.totalCount();
            mergeMatchedCounts(matchedTargetCounts, stackMatch.matchedTargetCounts());

            if (display.isEmpty() && !stackMatch.firstMatch().isEmpty()) {
                display = stackMatch.firstMatch();
            }
        }

        return new InventoryMatch(display, total, Map.copyOf(matchedTargetCounts));
    }

    /** 当前版本支持的实体容器白名单。 */
    private static boolean isSupportedEntityContainer(Entity entity) {
        if (entity instanceof StorageMinecartEntity || entity instanceof AbstractChestBoatEntity) {
            return true;
        }

        return entity instanceof AbstractDonkeyEntity donkeyEntity && donkeyEntity.hasChest();
    }

    /**
     * 汇总单个 ItemStack（含嵌套容器）匹配情况。
     *
     * @param stack 当前堆叠。
     * @param targets 目标物品集合。
     * @param depth 当前递归深度。
     */
    private StackMatch summarizeStackMatch(ItemStack stack, Set<Item> targets, int depth) {
        if (stack.isEmpty()) {
            return StackMatch.EMPTY;
        }

        int total = 0;
        ItemStack first = ItemStack.EMPTY;
        Map<Item, Integer> matchedTargetCounts = new HashMap<>();

        if (targets.contains(stack.getItem())) {
            total += stack.getCount();
            first = stack.copyWithCount(1);
            matchedTargetCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        if (depth < maxNestedContainerDepth) {
            ContainerComponent nestedContainer = stack.get(DataComponentTypes.CONTAINER);
            if (nestedContainer != null) {
                for (ItemStack nestedStack : nestedContainer.iterateNonEmptyCopy()) {
                    StackMatch nestedMatch = summarizeStackMatch(nestedStack, targets, depth + 1);
                    total += nestedMatch.totalCount();
                    mergeMatchedCounts(matchedTargetCounts, nestedMatch.matchedTargetCounts());

                    if (first.isEmpty() && !nestedMatch.firstMatch().isEmpty()) {
                        first = nestedMatch.firstMatch();
                    }
                }
            }
        }

        return new StackMatch(total, first, matchedTargetCounts);
    }

    private static void mergeMatchedCounts(Map<Item, Integer> target, Map<Item, Integer> source) {
        for (Map.Entry<Item, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    @FunctionalInterface
    public interface BlockEntityLookup {
        /**
         * @param pos 目标方块位置。
         * @return 该位置的方块实体；无则返回 null。
         */
        BlockEntity get(BlockPos pos);
    }

    @FunctionalInterface
    public interface NearbyEntityLookup {
        /**
         * @param searchBounds 球形半径外包盒，用于先做一次 AABB 过滤。
         * @return 外包盒内实体列表（调用方可返回可迭代集合）。
         */
        Iterable<Entity> get(Box searchBounds);
    }

    public record ContainerReference(BlockPos anchorPos, int entityId) {
        private static final int NO_ENTITY = -1;

        public static ContainerReference forBlock(BlockPos pos) {
            return new ContainerReference(pos.toImmutable(), NO_ENTITY);
        }

        public static ContainerReference forEntity(int entityId, BlockPos fallbackPos) {
            return new ContainerReference(fallbackPos.toImmutable(), entityId);
        }

        public boolean isEntity() {
            return entityId != NO_ENTITY;
        }
    }

    public record ContainerHit(ContainerReference reference, ItemStack displayStack, int totalMatchedCount, Map<Item, Integer> matchedTargetCounts) {
    }

    public record SearchComputation(
            List<ContainerHit> hits,
            int scannedBlockContainerCount,
            int scannedEntityContainerCount,
            int totalMatchedCount
    ) {
        public int scannedContainerCount() {
            return scannedBlockContainerCount + scannedEntityContainerCount;
        }
    }

    private record InventoryMatch(ItemStack displayStack, int totalCount, Map<Item, Integer> matchedTargetCounts) {
        private static final InventoryMatch EMPTY = new InventoryMatch(ItemStack.EMPTY, 0, Map.of());
    }

    private record StackMatch(int totalCount, ItemStack firstMatch, Map<Item, Integer> matchedTargetCounts) {
        private static final StackMatch EMPTY = new StackMatch(0, ItemStack.EMPTY, Map.of());
    }

    @FunctionalInterface
    private interface StackLookup {
        ItemStack get(int slot);
    }
}
