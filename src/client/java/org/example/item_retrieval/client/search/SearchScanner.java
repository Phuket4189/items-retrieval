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
 * 瀹瑰櫒鎵弿鍣細
 * 鍦ㄦ寚瀹氬崐寰勫唴閬嶅巻瀹瑰櫒锛岀粺璁＄洰鏍囩墿鍝佸懡涓儏鍐碉紝鏀寔宓屽瀹瑰櫒閫掑綊缁熻銆?
 */
public final class SearchScanner {

    /** 椹?楠＄湡瀹炶儗鍖呭瓧娈电紦瀛橈紙AbstractHorseEntity#items锛夈€?*/
    private static Field donkeyInventoryField;

    /** 妫€绱㈠崐寰勶紙鏂瑰潡锛夈€?*/
    private volatile int searchRadiusBlocks;

    /** 鍗曟鏈€澶氳褰曞灏戜釜鍛戒腑瀹瑰櫒銆?*/
    private final int maxResultHits;

    /** 鍗曟鏈€澶氭壂鎻忔柟鍧楀鍣ㄦ暟閲忥紝闃叉澶ц寖鍥村崱椤裤€?*/
    private final int maxBlockContainersScanned;

    /** 鍗曟鏈€澶氭壂鎻忓疄浣撳鍣ㄦ暟閲忥紝闃叉瀹炰綋瀵嗛泦鍖哄崱椤裤€?*/
    private final int maxEntityContainersScanned;

    /** 宓屽瀹瑰櫒閫掑綊灞傜骇涓婇檺銆?*/
    private final int maxNestedContainerDepth;

    /**
     * @param searchRadiusBlocks 妫€绱㈠崐寰勶紙鏂瑰潡锛夈€?
     * @param maxResultHits 鍗曟鏈€澶氳褰曠殑鍛戒腑瀹瑰櫒鏁般€?
     * @param maxBlockContainersScanned 鍗曟鏈€澶氭壂鎻忔柟鍧楀鍣ㄦ暟銆?
     * @param maxEntityContainersScanned 鍗曟鏈€澶氭壂鎻忓疄浣撳鍣ㄦ暟銆?
     * @param maxNestedContainerDepth 宓屽瀹瑰櫒閫掑綊娣卞害涓婇檺銆?
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
     * 鎵弿鐜╁闄勮繎鏂瑰潡瀹瑰櫒骞剁敓鎴愭绱㈠懡涓粨鏋溿€?
     *
     * @param blockEntityLookup 鏂瑰潡瀹炰綋鏌ヨ鍑芥暟銆?
     * @param center 鎵弿涓績鍧愭爣锛堥€氬父涓虹帺瀹跺潗鏍囷級銆?
     * @param targets 鐩爣鐗╁搧闆嗗悎銆?
     * @return 鍖呭惈鍛戒腑鍒楄〃銆佹壂鎻忓鍣ㄦ暟銆佸尮閰嶆€绘暟鐨勮绠楃粨鏋溿€?
     */
    public SearchComputation scanNearbyContainers(BlockEntityLookup blockEntityLookup, BlockPos center, Set<Item> targets) {
        return scanNearbyContainers(blockEntityLookup, searchBounds -> List.of(), center, targets);
    }

    /**
     * 鎵弿鐜╁闄勮繎鈥滄柟鍧楀鍣?+ 瀹炰綋瀹瑰櫒鈥濆苟鐢熸垚妫€绱㈠懡涓粨鏋溿€?
     *
     * @param blockEntityLookup 鏂瑰潡瀹炰綋鏌ヨ鍑芥暟銆?
     * @param nearbyEntityLookup 瀹炰綋鏌ヨ鍑芥暟锛堜紶鍏ョ悆褰㈠鍖呯洅锛夈€?
     * @param center 鎵弿涓績鍧愭爣锛堥€氬父涓虹帺瀹跺潗鏍囷級銆?
     * @param targets 鐩爣鐗╁搧闆嗗悎銆?
     * @return 鍖呭惈鍛戒腑鍒楄〃銆佹壂鎻忓鍣ㄦ暟銆佸尮閰嶆€绘暟鐨勮绠楃粨鏋溿€?
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
     * 瀵瑰懡涓粨鏋滃仛娣辨嫹璐濓紝閬垮厤璺ㄧ嚎绋嬩紶閫掓椂寮曠敤鍏变韩銆?
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

    /** 姹囨€诲崟涓鍣ㄤ腑鐩爣鐗╁搧鍖归厤鎯呭喌銆?*/
    private InventoryMatch summarizeInventoryMatch(Inventory inventory, Set<Item> targets) {
        return summarizeContainerMatch(inventory.size(), inventory::getStack, targets);
    }

    /** 姹囨€诲疄浣撳鍣ㄤ腑鐩爣鐗╁搧鍖归厤鎯呭喌銆?*/
    private InventoryMatch summarizeEntityInventoryMatch(Entity entity, Set<Item> targets) {
        if (entity instanceof Inventory inventory && (entity instanceof StorageMinecartEntity || entity instanceof AbstractChestBoatEntity)) {
            return summarizeInventoryMatch(inventory, targets);
        }

        if (entity instanceof AbstractDonkeyEntity donkeyEntity && donkeyEntity.hasChest()) {
            Inventory donkeyInventory = resolveDonkeyInventory(donkeyEntity);
            if (donkeyInventory != null) {
                return summarizeInventoryMatch(donkeyInventory, targets);
            }

            // 鍏滃簳锛氳嫢鍙嶅皠澶辫触鍒欏洖閫€鍒版槧灏勬Ы浣嶈鍙栵紙鍙兘瑕嗙洊涓嶅畬鏁达級銆?
            return summarizeContainerMatch(
                    donkeyEntity.getInventorySize(),
                    slot -> donkeyEntity.getStackReference(slot).get(),
                    targets
            );
        }

        return InventoryMatch.EMPTY;
    }

    /**
     * 璇诲彇椹?楠＄湡瀹炲簱瀛橈紙AbstractHorseEntity#items锛夈€?
     * 1.21.10 涓?getStackReference 浣跨敤鏄犲皠妲戒綅锛屼笉绛変簬鐪熷疄搴撳瓨涓嬫爣銆?
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

    /** 鎸夆€滄Ы浣嶈鍙栧嚱鏁扳€濇眹鎬诲鍣ㄥ尮閰嶆儏鍐点€?*/
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

    /** 褰撳墠鐗堟湰鏀寔鐨勫疄浣撳鍣ㄧ櫧鍚嶅崟銆?*/
    private static boolean isSupportedEntityContainer(Entity entity) {
        if (entity instanceof StorageMinecartEntity || entity instanceof AbstractChestBoatEntity) {
            return true;
        }

        return entity instanceof AbstractDonkeyEntity donkeyEntity && donkeyEntity.hasChest();
    }

    /**
     * 姹囨€诲崟涓?ItemStack锛堝惈宓屽瀹瑰櫒锛夊尮閰嶆儏鍐点€?
     *
     * @param stack 褰撳墠鍫嗗彔銆?
     * @param targets 鐩爣鐗╁搧闆嗗悎銆?
     * @param depth 褰撳墠閫掑綊娣卞害銆?
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
         * @param pos 鐩爣鏂瑰潡浣嶇疆銆?
         * @return 璇ヤ綅缃殑鏂瑰潡瀹炰綋锛涙棤鍒欒繑鍥?null銆?
         */
        BlockEntity get(BlockPos pos);
    }

    @FunctionalInterface
    public interface NearbyEntityLookup {
        /**
         * @param searchBounds 鐞冨舰鍗婂緞澶栧寘鐩掞紝鐢ㄤ簬鍏堝仛涓€娆?AABB 杩囨护銆?
         * @return 澶栧寘鐩掑唴瀹炰綋鍒楄〃锛堣皟鐢ㄦ柟鍙繑鍥炲彲杩唬闆嗗悎锛夈€?
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
