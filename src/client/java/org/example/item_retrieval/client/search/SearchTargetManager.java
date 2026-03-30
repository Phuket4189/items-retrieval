package org.example.item_retrieval.client.search;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 妫€绱㈢洰鏍囩鐞嗗櫒锛?
 * 缁存姢鐩爣姹犮€佸惎鐢ㄧ姸鎬併€侀鑹查厤缃€佹绱㈢粨鏋滃垎椤典笌灞曠ず搴撳瓨銆?
 */
public final class SearchTargetManager {

    public static final int NO_COLOR = -1;
    private static final int RESULT_COLUMNS = 9;

    private static final int[] TARGET_HIGHLIGHT_COLORS = {
            0xFFE76F51,
            0xFFF4A261,
            0xFFE9C46A,
            0xFF2A9D8F,
            0xFF4D96FF,
            0xFF90BE6D,
            0xFFF28482,
            0xFFC77DFF
    };

    /** 宸︿晶鐩爣姹犲簱瀛樸€?*/
    private final SimpleInventory searchOptions;

    /** 涓儴缁撴灉灞曠ず搴撳瓨銆?*/
    private final SimpleInventory searchResults;

    /** 姣忎釜鐩爣鐗╁搧瀵瑰簲鐨勯珮浜鑹层€?*/
    private final Map<Item, Integer> targetItemColors = new HashMap<>();

    /** 姣忎釜鐩爣鐗╁搧鏄惁鍚敤妫€绱€?*/
    private final Map<Item, Boolean> targetItemEnabled = new HashMap<>();

    /** 鏈€鏂颁竴娆℃绱㈠懡涓垪琛紙鐢ㄤ簬鍒嗛〉鏄剧ず锛夈€?*/
    private List<SearchScanner.ContainerHit> latestSearchHits = List.of();

    /** 寰呮坊鍔犵洰鏍囩殑榛樿棰滆壊銆?*/
    private int pendingTargetColor = TARGET_HIGHLIGHT_COLORS[0];

    /** 涓嬩竴娆″彇鑹叉澘棰滆壊鐨勭储寮曘€?*/
    private int nextPaletteColorIndex = 1;

    /** 褰撳墠缁撴灉鍒嗛〉鍋忕Щ锛堟寜鍒楀榻愶紝鍗曚綅锛氭Ы浣嶏級銆?*/
    private int searchResultRowOffset = 0;

    /**
     * @param optionSlotCount 鐩爣姹犲閲忋€?
     * @param resultSlotCount 缁撴灉灞曠ず妲戒綅鏁伴噺銆?
     */
    public SearchTargetManager(int optionSlotCount, int resultSlotCount) {
        this.searchOptions = new SimpleInventory(optionSlotCount);
        this.searchResults = new SimpleInventory(resultSlotCount);
    }

    public Inventory getSearchOptionsInventory() {
        return searchOptions;
    }

    public Inventory getSearchResultsInventory() {
        return searchResults;
    }

    /** 浣跨敤褰撳墠寰呮坊鍔犻鑹叉柊澧炵洰鏍囥€?*/
    public boolean addSearchTarget(ItemStack sourceStack) {
        return addSearchTarget(sourceStack, pendingTargetColor);
    }

    /**
     * 鏂板鐩爣骞舵寚瀹氶鑹层€?
     *
     * @param sourceStack 鏉ユ簮鍫嗗彔锛堝彧鍙?item锛屾暟閲忕粺涓€涓?1锛夈€?
     * @param colorArgb 鎸囧畾楂樹寒棰滆壊銆?
     */
    public boolean addSearchTarget(ItemStack sourceStack, int colorArgb) {
        if (sourceStack.isEmpty()) {
            return false;
        }

        Item targetItem = sourceStack.getItem();
        for (int i = 0; i < searchOptions.size(); i++) {
            ItemStack existing = searchOptions.getStack(i);
            if (!existing.isEmpty() && existing.getItem() == targetItem) {
                return false;
            }
        }

        for (int i = 0; i < searchOptions.size(); i++) {
            if (searchOptions.getStack(i).isEmpty()) {
                searchOptions.setStack(i, sourceStack.copyWithCount(1));
                targetItemColors.put(targetItem, normalizeOpaqueColor(colorArgb));
                targetItemEnabled.put(targetItem, true);
                pendingTargetColor = pickPaletteColor(nextPaletteColorIndex++);
                searchOptions.markDirty();
                return true;
            }
        }

        return false;
    }

    /** 鎸夌储寮曠Щ闄ょ洰鏍囥€?*/
    public boolean removeSearchTarget(int index) {
        if (index < 0 || index >= searchOptions.size()) {
            return false;
        }

        ItemStack stack = searchOptions.getStack(index);
        if (stack.isEmpty()) {
            return false;
        }

        targetItemColors.remove(stack.getItem());
        targetItemEnabled.remove(stack.getItem());
        searchOptions.setStack(index, ItemStack.EMPTY);
        compactSearchTargets();
        searchOptions.markDirty();
        return true;
    }

    /** 鏌ヨ鐩爣妲戒綅鏄惁鍚敤銆?*/
    public boolean isSearchTargetEnabled(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= searchOptions.size()) {
            return false;
        }

        ItemStack stack = searchOptions.getStack(slotIndex);
        if (stack.isEmpty()) {
            return false;
        }

        return targetItemEnabled.getOrDefault(stack.getItem(), true);
    }

    /** 鍒囨崲鐩爣妲戒綅鐨勫惎鐢ㄧ姸鎬併€?*/
    public boolean toggleSearchTargetEnabled(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= searchOptions.size()) {
            return false;
        }

        ItemStack stack = searchOptions.getStack(slotIndex);
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        boolean nextEnabled = !targetItemEnabled.getOrDefault(item, true);
        targetItemEnabled.put(item, nextEnabled);
        searchOptions.markDirty();
        return true;
    }

    /** @return 褰撳墠鐩爣鎬绘暟锛堝惈鍚敤涓庣鐢級銆?*/
    public int getSearchTargetCount() {
        int count = 0;
        for (int i = 0; i < searchOptions.size(); i++) {
            if (!searchOptions.getStack(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** @return 褰撳墠鍚敤鐩爣鏁伴噺銆?*/
    public int getEnabledSearchTargetCount() {
        int count = 0;
        for (int i = 0; i < searchOptions.size(); i++) {
            ItemStack stack = searchOptions.getStack(i);
            if (!stack.isEmpty() && targetItemEnabled.getOrDefault(stack.getItem(), true)) {
                count++;
            }
        }
        return count;
    }

    public int getSearchTargetColor(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= searchOptions.size()) {
            return NO_COLOR;
        }

        ItemStack stack = searchOptions.getStack(slotIndex);
        if (stack.isEmpty()) {
            return NO_COLOR;
        }

        return resolveTargetColor(stack.getItem());
    }

    public int getPendingTargetColor() {
        return pendingTargetColor;
    }

    public int cyclePendingTargetColor() {
        pendingTargetColor = nextPaletteColor(pendingTargetColor);
        return pendingTargetColor;
    }

    public int cycleSearchTargetColor(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= searchOptions.size()) {
            return NO_COLOR;
        }

        ItemStack stack = searchOptions.getStack(slotIndex);
        if (stack.isEmpty()) {
            return NO_COLOR;
        }

        Item item = stack.getItem();
        int updatedColor = nextPaletteColor(resolveTargetColor(item));
        targetItemColors.put(item, updatedColor);
        return updatedColor;
    }

    public String formatColorHex(int colorArgb) {
        return String.format("%06X", colorArgb & 0x00FFFFFF);
    }

    /**
     * 鏀堕泦鈥滃惎鐢ㄧ姸鎬佲€濈殑鐩爣闆嗗悎锛岀敤浜庡疄闄呮绱€?
     */
    public Set<Item> collectTargetItems() {
        Set<Item> targets = new HashSet<>();
        for (int i = 0; i < searchOptions.size(); i++) {
            ItemStack stack = searchOptions.getStack(i);
            if (!stack.isEmpty() && targetItemEnabled.getOrDefault(stack.getItem(), true)) {
                targets.add(stack.getItem());
            }
        }
        return targets;
    }

    public int resolveTargetColor(Item item) {
        Integer configuredColor = targetItemColors.get(item);
        if (configuredColor != null) {
            return configuredColor;
        }

        int fallbackIndex = Math.floorMod(item.hashCode(), TARGET_HIGHLIGHT_COLORS.length);
        return TARGET_HIGHLIGHT_COLORS[fallbackIndex];
    }

    public void clearSearchResults() {
        latestSearchHits = List.of();
        searchResultRowOffset = 0;
        clearInventory(searchResults);
    }

    /**
     * 搴旂敤涓€娆℃柊鐨勬绱㈢粨鏋滐紝骞堕噸缃垎椤靛埌绗竴椤点€?
     */
    public void applySearchResults(List<SearchScanner.ContainerHit> hits) {
        latestSearchHits = List.copyOf(hits);
        searchResultRowOffset = 0;
        refreshSearchResultsPage();
    }

    public int getSearchResultCount() {
        return latestSearchHits.size();
    }

    public int getSearchResultRowOffset() {
        return searchResultRowOffset;
    }

    public int getSearchResultMaxRowOffset() {
        return computeMaxRowOffset(latestSearchHits.size(), RESULT_COLUMNS, searchResults.size());
    }

    public boolean scrollSearchResultsByRows(int rowDelta) {
        if (rowDelta == 0) {
            return false;
        }

        return setSearchResultRowOffset(searchResultRowOffset + rowDelta * RESULT_COLUMNS);
    }

    public boolean setSearchResultRowOffset(int requestedOffset) {
        int maxOffset = getSearchResultMaxRowOffset();
        int snappedOffset = snapOffsetToRow(requestedOffset);
        int clampedOffset = Math.max(0, Math.min(maxOffset, snappedOffset));

        if (clampedOffset == searchResultRowOffset) {
            return false;
        }

        searchResultRowOffset = clampedOffset;
        refreshSearchResultsPage();
        return true;
    }

    private void refreshSearchResultsPage() {
        for (int slotIndex = 0; slotIndex < searchResults.size(); slotIndex++) {
            int hitIndex = searchResultRowOffset + slotIndex;
            if (hitIndex >= latestSearchHits.size()) {
                searchResults.setStack(slotIndex, ItemStack.EMPTY);
                continue;
            }

            SearchScanner.ContainerHit hit = latestSearchHits.get(hitIndex);
            searchResults.setStack(slotIndex, createDisplayStack(hit));
        }

        searchResults.markDirty();
    }

    private static ItemStack createDisplayStack(SearchScanner.ContainerHit hit) {
        if (hit.displayStack().isEmpty()) {
            return ItemStack.EMPTY;
        }

        int displayCount = Math.min(hit.displayStack().getMaxCount(), Math.max(1, hit.totalMatchedCount()));
        return hit.displayStack().copyWithCount(displayCount);
    }

    private static int computeMaxRowOffset(int totalEntries, int columns, int visibleSlots) {
        if (totalEntries <= 0 || columns <= 0 || visibleSlots <= 0) {
            return 0;
        }

        int visibleRows = Math.max(1, (int) Math.ceil((double) visibleSlots / (double) columns));
        int totalRows = (totalEntries + columns - 1) / columns;
        int overflowRows = Math.max(0, totalRows - visibleRows);
        return overflowRows * columns;
    }

    private static int snapOffsetToRow(int offset) {
        return Math.floorDiv(offset, RESULT_COLUMNS) * RESULT_COLUMNS;
    }

    private int nextPaletteColor(int currentColorArgb) {
        int normalized = normalizeOpaqueColor(currentColorArgb);
        for (int i = 0; i < TARGET_HIGHLIGHT_COLORS.length; i++) {
            if (TARGET_HIGHLIGHT_COLORS[i] == normalized) {
                return TARGET_HIGHLIGHT_COLORS[(i + 1) % TARGET_HIGHLIGHT_COLORS.length];
            }
        }

        return pickPaletteColor(nextPaletteColorIndex++);
    }

    private int pickPaletteColor(int paletteIndex) {
        return TARGET_HIGHLIGHT_COLORS[Math.floorMod(paletteIndex, TARGET_HIGHLIGHT_COLORS.length)];
    }

    private static int normalizeOpaqueColor(int colorArgb) {
        return 0xFF000000 | (colorArgb & 0x00FFFFFF);
    }

    private void compactSearchTargets() {
        int writeIndex = 0;
        for (int readIndex = 0; readIndex < searchOptions.size(); readIndex++) {
            ItemStack stack = searchOptions.getStack(readIndex);
            if (stack.isEmpty()) {
                continue;
            }

            if (writeIndex != readIndex) {
                searchOptions.setStack(writeIndex, stack);
                searchOptions.setStack(readIndex, ItemStack.EMPTY);
            }
            writeIndex++;
        }

        for (int i = writeIndex; i < searchOptions.size(); i++) {
            searchOptions.setStack(i, ItemStack.EMPTY);
        }
    }

    private static void clearInventory(SimpleInventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }
        inventory.markDirty();
    }
}
