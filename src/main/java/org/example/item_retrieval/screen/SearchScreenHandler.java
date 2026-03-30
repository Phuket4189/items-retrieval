package org.example.item_retrieval.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.example.item_retrieval.ItemRetrievalMod;

public class SearchScreenHandler extends ScreenHandler {

    /** 结果栏列数。 */
    private static final int RESULT_COLUMNS = 9;

    /** 结果栏行数。 */
    private static final int RESULT_ROWS = 7;

    /** 默认目标池容量。 */
    private static final int DEFAULT_OPTION_CAPACITY = 128;

    /** 结果栏总槽位数（7 x 9）。 */
    private static final int RESULT_SLOT_COUNT = RESULT_ROWS * RESULT_COLUMNS;

    private final Inventory searchOptions;      // 左侧目标池（可滚动）
    private final Inventory searchResults;      // 中部结果栏（7x9）

    /**
     * 客户端默认构造：自动创建目标池与结果栏库存。
     *
     * @param syncId ScreenHandler 同步 ID。
     * @param playerInventory 玩家背包对象（用于生命周期绑定）。
     */
    public SearchScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory,
                new SimpleInventory(DEFAULT_OPTION_CAPACITY),
                new SimpleInventory(RESULT_SLOT_COUNT));
    }

    /**
     * 主构造函数。
     *
     * @param syncId ScreenHandler 同步 ID。
     * @param playerInventory 玩家背包对象。
     * @param searchOptions 左侧目标池 Inventory。
     * @param searchResults 中部检索结果 Inventory。
     */
    public SearchScreenHandler(int syncId, PlayerInventory playerInventory,
                               Inventory searchOptions, Inventory searchResults) {
        // ✅ 使用已注册的 ScreenHandlerType
        super(ItemRetrievalMod.SEARCH_SCREEN_HANDLER, syncId);
        this.searchOptions = searchOptions;
        this.searchResults = searchResults;

        // 确保Inventory有效
        searchOptions.onOpen(playerInventory.player);
        searchResults.onOpen(playerInventory.player);

        // ===== 搜索结果栏 (7x9 网格) =====
        for (int row = 0; row < RESULT_ROWS; row++) {
            for (int col = 0; col < RESULT_COLUMNS; col++) {
                this.addSlot(new Slot(searchResults, col + row * RESULT_COLUMNS,
                        136 + col * 18, 42 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false; // 搜索结果不能直接放入
                    }

                    @Override
                    public boolean canTakeItems(PlayerEntity playerEntity) {
                        return false; // 搜索结果仅展示，不允许直接取出
                    }
                });
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.searchOptions.canPlayerUse(player) &&
                this.searchResults.canPlayerUse(player);
    }

    public Inventory getSearchOptions() {
        return searchOptions;
    }

    public Inventory getSearchResults() {
        return searchResults;
    }
}
