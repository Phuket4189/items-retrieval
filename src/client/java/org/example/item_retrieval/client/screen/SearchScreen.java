package org.example.item_retrieval.client.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.example.item_retrieval.client.ItemRetrievalModClient;
import org.example.item_retrieval.client.config.SearchRuntimeConfig;
import org.example.item_retrieval.screen.SearchScreenHandler;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchScreen extends HandledScreen<SearchScreenHandler> {

    // Core panel palette:
    // base gray #c6c6c6, outline #000000, highlight #ffffff, shadow #555555.
    private static final int PANEL_TEXT_COLOR = 0xFFEFEFEF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB7B7B7;
    private static final int SLOT_SELECTION_COLOR = 0xFFE3E3E3;

    private static final int FRAME_FILL_COLOR = 0xF0C6C6C6;
    private static final int FRAME_OUTLINE_EDGE = 0xFF000000;
    private static final int FRAME_LIGHT_EDGE = 0xFFFFFFFF;
    private static final int FRAME_DARK_EDGE = 0xFF555555;
    private static final int SUB_PANEL_FILL = 0xD89A9A9A;

    private static final int SEARCH_EDITABLE_TEXT_COLOR = 0xFFF5F5F5;
    private static final int SEARCH_UNEDITABLE_TEXT_COLOR = 0xFF8F8F8F;

    private static final int TARGET_LIST_X = 8;
    private static final int TARGET_LIST_Y = 28;
    private static final int TARGET_LIST_WIDTH = 120;
    private static final int TARGET_LIST_HEIGHT = 154;
    private static final int TARGET_ROW_HEIGHT = 18;
    private static final int TARGET_VISIBLE_ROWS = TARGET_LIST_HEIGHT / TARGET_ROW_HEIGHT;
    private static final int TARGET_SCROLLBAR_X = TARGET_LIST_X + TARGET_LIST_WIDTH - 7;
    private static final int TARGET_SCROLLBAR_Y = TARGET_LIST_Y + 2;
    private static final int TARGET_SCROLLBAR_WIDTH = 5;
    private static final int TARGET_SCROLLBAR_HEIGHT = TARGET_LIST_HEIGHT - 4;

    private static final int RESULT_COLUMNS = 9;
    private static final int RESULT_ROWS = 7;
    private static final int RESULT_PAGE_SIZE = RESULT_COLUMNS * RESULT_ROWS;
    private static final int RESULT_SLOT_START_X = 136;
    private static final int RESULT_SLOT_START_Y = 42;
    private static final int RESULT_SLOT_SPACING = 18;
    private static final int RESULT_SCROLLBAR_X = RESULT_SLOT_START_X + RESULT_COLUMNS * RESULT_SLOT_SPACING + 2;
    private static final int RESULT_SCROLLBAR_Y = RESULT_SLOT_START_Y;
    private static final int RESULT_SCROLLBAR_WIDTH = 6;
    private static final int RESULT_SCROLLBAR_HEIGHT = RESULT_ROWS * RESULT_SLOT_SPACING;

    private static final int TOOL_PANEL_X = 308;
    private static final int TOOL_PANEL_Y = 28;
    private static final int TOOL_PANEL_WIDTH = 62;
    private static final int TOOL_PANEL_HEIGHT = 154;
    private static final int TOOL_BUTTON_WIDTH = 58;
    private static final int RADIUS_CONTROL_Y = TOOL_PANEL_Y + 122;
    private static final int RADIUS_CONTROL_HEIGHT = 14;
    private static final int RADIUS_BUTTON_WIDTH = 12;
    private static final int RADIUS_INPUT_WIDTH = TOOL_BUTTON_WIDTH - (RADIUS_BUTTON_WIDTH * 2) - 2;
    private static final int RADIUS_MIN_VALUE = SearchRuntimeConfig.SEARCH_RADIUS_MIN_BLOCKS;
    private static final int RADIUS_MAX_VALUE = SearchRuntimeConfig.SEARCH_RADIUS_MAX_BLOCKS;

    private static final String SEARCH_PLACEHOLDER_TEXT = "名称 / minecraft:id / 模糊词";
    private static final int SEARCH_PLACEHOLDER_COLOR = 0x99999999;

    private static final int TARGET_ROW_BG_EVEN = 0x66464646;
    private static final int TARGET_ROW_BG_ODD = 0x66525252;
    private static final int TARGET_ROW_SELECTED_BG = 0x557F7F7F;
    private static final int TARGET_ROW_HOVER_BG = 0x336A6A6A;
    private static final int TARGET_ROW_DISABLED_OVERLAY = 0x99000000;
    private static final int TARGET_STATUS_PILL_ENABLED_BG = 0xCC5F5F5F;
    private static final int TARGET_STATUS_PILL_DISABLED_BG = 0xCC2F2F2F;
    private static final int TARGET_STATUS_ENABLED_TEXT = 0xFFF0F0F0;
    private static final int TARGET_STATUS_DISABLED_TEXT = 0xFFA8A8A8;
    private static final int TARGET_NAME_DISABLED_TEXT = 0xFF8A8A8A;

    private static final int SCROLLBAR_TRACK_COLOR = 0xCC2B2B2B;
    private static final int SCROLLBAR_IDLE_FILL_COLOR = 0x66555555;
    private static final int SCROLLBAR_THUMB_COLOR = 0xFFC6C6C6;
    private static final int SCROLLBAR_THUMB_DRAGGING_COLOR = 0xFFF0F0F0;

    private static final int CONTINUOUS_ON_TEXT_COLOR = 0xFFE5E5E5;

    private ButtonWidget findButton;
    private ButtonWidget addButton;
    private ButtonWidget removeButton;
    private ButtonWidget searchButton;
    private ButtonWidget retrieveButton;
    private ButtonWidget colorButton;
    private ButtonWidget radiusDecreaseButton;
    private ButtonWidget radiusIncreaseButton;
    private TextFieldWidget searchField;
    private TextFieldWidget radiusField;

    private boolean updatingRadiusFieldText = false;

    private int selectedOptionIndex = -1;
    private int selectedCatalogResultGlobalIndex = -1;
    private int catalogResultRowOffset = 0;
    private int targetListRowOffset = 0;

    private boolean showingCatalogResults = false;
    private boolean draggingResultScrollbar = false;
    private boolean draggingTargetScrollbar = false;

    private final List<ItemStack> catalogResults = new ArrayList<>();

    public SearchScreen(SearchScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 380;
        this.backgroundHeight = 194;
    }

    @Override
    protected void init() {
        super.init();

        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        this.searchField = new TextFieldWidget(
                textRenderer,
                guiLeft + RESULT_SLOT_START_X,
                guiTop + 11,
                162,
                14,
                Text.literal("搜索物品")
        );
        this.searchField.setMaxLength(50);
        this.searchField.setDrawsBackground(true);
        this.searchField.setVisible(true);
        this.searchField.setEditable(true);
        this.searchField.setEditableColor(SEARCH_EDITABLE_TEXT_COLOR);
        this.searchField.setUneditableColor(SEARCH_UNEDITABLE_TEXT_COLOR);
        this.searchField.setText("");
        this.addDrawableChild(searchField);
        this.setInitialFocus(searchField);
        this.searchField.setFocused(true);

        this.findButton = ButtonWidget.builder(
                Text.literal("搜索"),
                button -> runCatalogItemSearch(true)
        ).dimensions(guiLeft + TOOL_PANEL_X + 2, guiTop + 11, TOOL_BUTTON_WIDTH, 14).build();
        this.addDrawableChild(findButton);

        this.addButton = ButtonWidget.builder(
                Text.literal("+添"),
                button -> {
                    if (client == null || client.player == null) {
                        return;
                    }

                    ItemStack selected = getSelectedCatalogStack();
                    if (selected.isEmpty()) {
                        client.player.sendMessage(Text.literal("请先搜索并选中一个物品后再添加。"), true);
                        return;
                    }

                    boolean added = ItemRetrievalModClient.addSearchTarget(selected);
                    if (!added) {
                        client.player.sendMessage(Text.literal("添加失败：目标已存在或目标列表已满。"), true);
                    }
                }
        ).dimensions(guiLeft + TOOL_PANEL_X + 2, guiTop + TOOL_PANEL_Y + 12, TOOL_BUTTON_WIDTH, 18).build();
        this.addDrawableChild(addButton);

        this.removeButton = ButtonWidget.builder(
                Text.literal("-删"),
                button -> {
                    if (client == null || client.player == null) {
                        return;
                    }

                    int targetIndex = selectedOptionIndex >= 0 ? selectedOptionIndex : findLastFilledOptionSlot();
                    if (targetIndex < 0) {
                        client.player.sendMessage(Text.literal("目标列表为空。"), true);
                        return;
                    }

                    boolean removed = ItemRetrievalModClient.removeSearchTarget(targetIndex);
                    if (removed) {
                        Inventory options = handler.getSearchOptions();
                        if (selectedOptionIndex >= 0
                                && (selectedOptionIndex >= options.size() || options.getStack(selectedOptionIndex).isEmpty())) {
                            selectedOptionIndex = -1;
                        }
                        clampTargetListRowOffset();
                    }
                }
        ).dimensions(guiLeft + TOOL_PANEL_X + 2, guiTop + TOOL_PANEL_Y + 34, TOOL_BUTTON_WIDTH, 18).build();
        this.addDrawableChild(removeButton);

        this.searchButton = ButtonWidget.builder(
                Text.literal("检索"),
                button -> {
                    showingCatalogResults = false;
                    selectedCatalogResultGlobalIndex = -1;
                    draggingResultScrollbar = false;
                    clearResultDisplayInventory();
                    ItemRetrievalModClient.runNearbySearch(client, true);
                }
        ).dimensions(guiLeft + TOOL_PANEL_X + 2, guiTop + TOOL_PANEL_Y + 56, TOOL_BUTTON_WIDTH, 18).build();
        this.addDrawableChild(searchButton);

        this.retrieveButton = ButtonWidget.builder(
                Text.literal("清空"),
                button -> {
                    catalogResults.clear();
                    catalogResultRowOffset = 0;
                    selectedCatalogResultGlobalIndex = -1;
                    showingCatalogResults = false;
                    draggingResultScrollbar = false;
                    clearResultDisplayInventory();
                    ItemRetrievalModClient.clearSearchResultsAndHighlights();
                    if (client != null && client.player != null) {
                        client.player.sendMessage(Text.literal("已清空检索结果与容器框记。"), true);
                    }
                }
        ).dimensions(guiLeft + TOOL_PANEL_X + 2, guiTop + TOOL_PANEL_Y + 78, TOOL_BUTTON_WIDTH, 18).build();
        this.addDrawableChild(retrieveButton);

        this.colorButton = ButtonWidget.builder(
                Text.literal("换色"),
                button -> {
                    if (client == null || client.player == null) {
                        return;
                    }

                    if (selectedOptionIndex >= 0) {
                        int updatedColor = ItemRetrievalModClient.cycleSearchTargetColor(selectedOptionIndex);
                        if (updatedColor != -1) {
                            client.player.sendMessage(Text.literal(
                                    "已切换目标颜色: #" + ItemRetrievalModClient.formatColorHex(updatedColor)
                            ), true);
                            return;
                        }
                    }

                    int pendingColor = ItemRetrievalModClient.cyclePendingTargetColor();
                    client.player.sendMessage(Text.literal(
                            "下一个添加目标颜色: #" + ItemRetrievalModClient.formatColorHex(pendingColor)
                    ), true);
                }
        ).dimensions(guiLeft + TOOL_PANEL_X + 2, guiTop + TOOL_PANEL_Y + 100, TOOL_BUTTON_WIDTH, 18).build();
        this.addDrawableChild(colorButton);

        int radiusControlX = guiLeft + TOOL_PANEL_X + 2;
        int radiusControlY = guiTop + RADIUS_CONTROL_Y;
        int radiusInputX = radiusControlX + RADIUS_BUTTON_WIDTH + 1;
        int radiusIncreaseX = radiusInputX + RADIUS_INPUT_WIDTH + 1;

        this.radiusDecreaseButton = ButtonWidget.builder(
                Text.literal("<"),
                button -> setSearchRadiusAndSyncField(ItemRetrievalModClient.adjustSearchRadiusBlocks(-1))
        ).dimensions(radiusControlX, radiusControlY, RADIUS_BUTTON_WIDTH, RADIUS_CONTROL_HEIGHT).build();
        this.addDrawableChild(radiusDecreaseButton);

        this.radiusField = new TextFieldWidget(
                textRenderer,
                radiusInputX,
                radiusControlY,
                RADIUS_INPUT_WIDTH,
                RADIUS_CONTROL_HEIGHT,
                Text.literal("检索半径")
        );
        this.radiusField.setMaxLength(3);
        this.radiusField.setDrawsBackground(true);
        this.radiusField.setEditable(true);
        this.radiusField.setEditableColor(SEARCH_EDITABLE_TEXT_COLOR);
        this.radiusField.setUneditableColor(SEARCH_UNEDITABLE_TEXT_COLOR);
        this.radiusField.setText(Integer.toString(ItemRetrievalModClient.getSearchRadiusBlocks()));
        this.radiusField.setChangedListener(this::onRadiusFieldChanged);
        this.addDrawableChild(radiusField);

        this.radiusIncreaseButton = ButtonWidget.builder(
                Text.literal(">"),
                button -> setSearchRadiusAndSyncField(ItemRetrievalModClient.adjustSearchRadiusBlocks(1))
        ).dimensions(radiusIncreaseX, radiusControlY, RADIUS_BUTTON_WIDTH, RADIUS_CONTROL_HEIGHT).build();
        this.addDrawableChild(radiusIncreaseButton);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        drawFramedPanel(context, guiLeft, guiTop, backgroundWidth, backgroundHeight, FRAME_FILL_COLOR);
        drawFramedPanel(context, guiLeft + TARGET_LIST_X, guiTop + TARGET_LIST_Y, TARGET_LIST_WIDTH, TARGET_LIST_HEIGHT, SUB_PANEL_FILL);
        drawFramedPanel(context, guiLeft + RESULT_SLOT_START_X - 6, guiTop + 28, 170, 154, SUB_PANEL_FILL);
        drawFramedPanel(context, guiLeft + TOOL_PANEL_X, guiTop + TOOL_PANEL_Y, TOOL_PANEL_WIDTH, TOOL_PANEL_HEIGHT, SUB_PANEL_FILL);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, this.title, 8, 8, PANEL_TEXT_COLOR, false);

        int totalTargets = ItemRetrievalModClient.getSearchTargetCount();
        int enabledTargets = ItemRetrievalModClient.getEnabledSearchTargetCount();
        context.drawText(
                textRenderer,
                Text.literal("目标 " + enabledTargets + "/" + totalTargets),
                TARGET_LIST_X + 4,
                17,
                SECONDARY_TEXT_COLOR,
                false
        );

        if (showingCatalogResults) {
            int total = catalogResults.size();
            context.drawText(
                    textRenderer,
                    Text.literal("词条结果: " + total + "（选中后点 +添）"),
                    RESULT_SLOT_START_X,
                    30,
                    SECONDARY_TEXT_COLOR,
                    false
            );
        } else {
            int total = ItemRetrievalModClient.getSearchResultCount();
            context.drawText(
                    textRenderer,
                    Text.literal("检索结果: " + total + "（容器命中）"),
                    RESULT_SLOT_START_X,
                    30,
                    SECONDARY_TEXT_COLOR,
                    false
            );
        }

        context.drawText(textRenderer, Text.literal("工具"), TOOL_PANEL_X + 20, TOOL_PANEL_Y + 3, SECONDARY_TEXT_COLOR, false);

        boolean continuousEnabled = ItemRetrievalModClient.isContinuousSearchEnabled();
        int continuousTextColor = continuousEnabled ? CONTINUOUS_ON_TEXT_COLOR : SECONDARY_TEXT_COLOR;
        context.drawText(
            textRenderer,
            Text.literal(continuousEnabled ? "O:持续检索 [ON]" : "O:持续检索 [OFF]"),
            RESULT_SLOT_START_X,
            164,
            continuousTextColor,
            false
        );
        context.drawText(textRenderer, Text.literal("面板[检索]=单次，右键目标可换色"), RESULT_SLOT_START_X, 174, SECONDARY_TEXT_COLOR, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        ensureSelectedOptionStillValid();
        clampTargetListRowOffset();

        drawTargetRows(context, guiLeft, guiTop, mouseX, mouseY);
        drawTargetScrollbar(context, guiLeft, guiTop);

        if (showingCatalogResults && selectedCatalogResultGlobalIndex >= 0) {
            int localSlotIndex = selectedCatalogResultGlobalIndex - catalogResultRowOffset;
            if (localSlotIndex >= 0 && localSlotIndex < RESULT_PAGE_SIZE) {
                drawSelectedResultSlotFrame(context, guiLeft, guiTop, localSlotIndex);
            }
        }

        drawResultScrollbar(context, guiLeft, guiTop);
        syncRadiusFieldWithRuntimeValue();
        drawSearchPlaceholder(context);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && isMouseOverTargetScrollbar(click.x(), click.y())
                && hasScrollableTargetRows()) {
            draggingTargetScrollbar = true;
            setTargetListOffsetFromMouse(click.y());
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && isMouseOverResultScrollbar(click.x(), click.y())
                && hasCurrentScrollableResults()) {
            draggingResultScrollbar = true;
            setCurrentResultOffsetFromMouse(click.y());
            return true;
        }

        int clickedTargetSlot = getTargetSlotAt(click.x(), click.y());
        if (clickedTargetSlot >= 0) {
            selectedOptionIndex = clickedTargetSlot;

            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                ItemRetrievalModClient.cycleSearchTargetColor(clickedTargetSlot);
                return true;
            }

            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && isMouseOverTargetToggleZone(click.x(), click.y())) {
                ItemRetrievalModClient.toggleSearchTargetEnabled(clickedTargetSlot);
                return true;
            }

            return true;
        }

        Slot clickedSlot = findHoveredSlot(click.x(), click.y());
        if (clickedSlot != null && clickedSlot.inventory == handler.getSearchResults()) {
            if (showingCatalogResults) {
                int globalIndex = catalogResultRowOffset + clickedSlot.getIndex();
                if (globalIndex >= 0 && globalIndex < catalogResults.size()) {
                    selectedCatalogResultGlobalIndex = globalIndex;
                } else {
                    selectedCatalogResultGlobalIndex = -1;
                }
            }
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingTargetScrollbar && hasScrollableTargetRows()) {
            setTargetListOffsetFromMouse(click.y());
            return true;
        }

        if (draggingResultScrollbar && hasCurrentScrollableResults()) {
            setCurrentResultOffsetFromMouse(click.y());
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean consumed = false;
        if (draggingTargetScrollbar) {
            draggingTargetScrollbar = false;
            consumed = true;
        }

        if (draggingResultScrollbar) {
            draggingResultScrollbar = false;
            consumed = true;
        }

        if (consumed) {
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if ((isMouseOverTargetArea(mouseX, mouseY) || isMouseOverTargetScrollbar(mouseX, mouseY))
                && hasScrollableTargetRows()
                && verticalAmount != 0.0D) {
            int rowDelta = verticalAmount < 0.0D ? 1 : -1;
            if (scrollTargetListByRows(rowDelta)) {
                return true;
            }
        }

        if ((isMouseOverResultArea(mouseX, mouseY) || isMouseOverResultScrollbar(mouseX, mouseY))
                && hasCurrentScrollableResults()
                && verticalAmount != 0.0D) {
            int rowDelta = verticalAmount < 0.0D ? 1 : -1;
            if (scrollCurrentResultsByRows(rowDelta)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (searchField != null
                && searchField.isFocused()
                && (keyInput.key() == GLFW.GLFW_KEY_ENTER || keyInput.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            runCatalogItemSearch(true);
            return true;
        }

        if (radiusField != null
                && radiusField.isFocused()
                && (keyInput.key() == GLFW.GLFW_KEY_ENTER || keyInput.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            commitRadiusFieldInput();
            return true;
        }

        return super.keyPressed(keyInput);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        // 纯客户端界面，不向服务器容器发送 slot click。
    }

    private void ensureSelectedOptionStillValid() {
        if (selectedOptionIndex < 0) {
            return;
        }

        Inventory options = handler.getSearchOptions();
        if (selectedOptionIndex >= options.size() || options.getStack(selectedOptionIndex).isEmpty()) {
            selectedOptionIndex = -1;
        }
    }

    private Slot findHoveredSlot(double mouseX, double mouseY) {
        for (Slot slot : handler.slots) {
            if (!slot.isEnabled()) {
                continue;
            }

            if (isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }

        return null;
    }

    private List<Integer> collectFilledTargetSlots() {
        Inventory options = handler.getSearchOptions();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            if (!options.getStack(i).isEmpty()) {
                indices.add(i);
            }
        }
        return indices;
    }

    private int getTargetListMaxRowOffset() {
        int total = ItemRetrievalModClient.getSearchTargetCount();
        return Math.max(0, total - TARGET_VISIBLE_ROWS);
    }

    private boolean hasScrollableTargetRows() {
        return getTargetListMaxRowOffset() > 0;
    }

    private void clampTargetListRowOffset() {
        int maxOffset = getTargetListMaxRowOffset();
        if (targetListRowOffset > maxOffset) {
            targetListRowOffset = maxOffset;
        }
        if (targetListRowOffset < 0) {
            targetListRowOffset = 0;
        }
    }

    private boolean scrollTargetListByRows(int rowDelta) {
        if (rowDelta == 0) {
            return false;
        }

        return setTargetListRowOffset(targetListRowOffset + rowDelta);
    }

    private boolean setTargetListRowOffset(int requestedOffset) {
        int maxOffset = getTargetListMaxRowOffset();
        int clampedOffset = Math.max(0, Math.min(maxOffset, requestedOffset));
        if (clampedOffset == targetListRowOffset) {
            return false;
        }

        targetListRowOffset = clampedOffset;
        return true;
    }

    private void setTargetListOffsetFromMouse(double mouseY) {
        int maxOffset = getTargetListMaxRowOffset();
        if (maxOffset <= 0) {
            return;
        }

        int guiTop = (height - backgroundHeight) / 2;
        int trackY = guiTop + TARGET_SCROLLBAR_Y;
        int thumbHeight = getTargetScrollbarThumbHeight(ItemRetrievalModClient.getSearchTargetCount());
        int movableHeight = TARGET_SCROLLBAR_HEIGHT - thumbHeight;
        if (movableHeight <= 0) {
            setTargetListRowOffset(0);
            return;
        }

        double normalized = (mouseY - trackY - (thumbHeight / 2.0D)) / (double) movableHeight;
        double clamped = Math.max(0.0D, Math.min(1.0D, normalized));
        int targetOffset = (int) Math.round(clamped * maxOffset);
        setTargetListRowOffset(targetOffset);
    }

    private int getTargetScrollbarThumbHeight(int totalEntries) {
        if (totalEntries <= TARGET_VISIBLE_ROWS) {
            return TARGET_SCROLLBAR_HEIGHT;
        }

        return Math.max(14, Math.round((TARGET_VISIBLE_ROWS / (float) totalEntries) * TARGET_SCROLLBAR_HEIGHT));
    }

    private void drawTargetRows(DrawContext context, int guiLeft, int guiTop, int mouseX, int mouseY) {
        List<Integer> targetSlots = collectFilledTargetSlots();

        int rowX = guiLeft + TARGET_LIST_X + 2;
        int rowYStart = guiTop + TARGET_LIST_Y + 2;
        int rowWidth = TARGET_LIST_WIDTH - TARGET_SCROLLBAR_WIDTH - 4;

        for (int row = 0; row < TARGET_VISIBLE_ROWS; row++) {
            int rowY = rowYStart + row * TARGET_ROW_HEIGHT;
            int globalRow = targetListRowOffset + row;

            int rowBg = ((row & 1) == 0) ? TARGET_ROW_BG_EVEN : TARGET_ROW_BG_ODD;
            context.fill(rowX, rowY, rowX + rowWidth, rowY + TARGET_ROW_HEIGHT - 1, rowBg);

            if (globalRow >= targetSlots.size()) {
                continue;
            }

            int slotIndex = targetSlots.get(globalRow);
            ItemStack stack = handler.getSearchOptions().getStack(slotIndex);
            if (stack.isEmpty()) {
                continue;
            }

            boolean hovered = mouseX >= rowX
                    && mouseX < rowX + rowWidth
                    && mouseY >= rowY
                    && mouseY < rowY + TARGET_ROW_HEIGHT - 1;
            boolean selected = selectedOptionIndex == slotIndex;
            boolean enabled = ItemRetrievalModClient.isSearchTargetEnabled(slotIndex);

            if (selected) {
                context.fill(rowX, rowY, rowX + rowWidth, rowY + TARGET_ROW_HEIGHT - 1, TARGET_ROW_SELECTED_BG);
                context.fill(rowX, rowY, rowX + rowWidth, rowY + 1, SLOT_SELECTION_COLOR);
                context.fill(rowX, rowY + TARGET_ROW_HEIGHT - 2, rowX + rowWidth, rowY + TARGET_ROW_HEIGHT - 1, SLOT_SELECTION_COLOR);
                context.fill(rowX, rowY, rowX + 1, rowY + TARGET_ROW_HEIGHT - 1, SLOT_SELECTION_COLOR);
                context.fill(rowX + rowWidth - 1, rowY, rowX + rowWidth, rowY + TARGET_ROW_HEIGHT - 1, SLOT_SELECTION_COLOR);
            } else if (hovered) {
                context.fill(rowX, rowY, rowX + rowWidth, rowY + TARGET_ROW_HEIGHT - 1, TARGET_ROW_HOVER_BG);
            }

            context.drawItem(stack, rowX + 2, rowY + 1);
            if (!enabled) {
                context.fill(rowX + 2, rowY + 1, rowX + 18, rowY + 17, TARGET_ROW_DISABLED_OVERLAY);
            }

            // Status label (right-aligned, dynamic width)
            String status = enabled ? "Enabled" : "Disabled";
            int statusTextWidth = textRenderer.getWidth(status);
            int statusPillRight = rowX + rowWidth - 13; // 13px margin for color swatch
            int statusPillLeft = statusPillRight - statusTextWidth - 4;
            int statusPillBg = enabled ? TARGET_STATUS_PILL_ENABLED_BG : TARGET_STATUS_PILL_DISABLED_BG;
            context.fill(statusPillLeft, rowY + 3, statusPillRight, rowY + TARGET_ROW_HEIGHT - 3, statusPillBg);
            int statusColor = enabled ? TARGET_STATUS_ENABLED_TEXT : TARGET_STATUS_DISABLED_TEXT;
            context.drawText(textRenderer, status, statusPillLeft + 2, rowY + 5, statusColor, false);

            // Item name — max width limited so it never overlaps with the status pill
            int nameMaxWidth = statusPillLeft - (rowX + 22) - 3;
            if (nameMaxWidth < 1) nameMaxWidth = 1;
            String displayedName = textRenderer.trimToWidth(stack.getName().getString(), nameMaxWidth);
            int nameColor = enabled ? PANEL_TEXT_COLOR : TARGET_NAME_DISABLED_TEXT;
            context.drawText(textRenderer, displayedName, rowX + 22, rowY + 5, nameColor, false);

            int slotColor = ItemRetrievalModClient.getSearchTargetColor(slotIndex);
            if (slotColor != -1) {
                int swatchX = rowX + rowWidth - 12;
                int swatchY = rowY + 5;
                int color = 0xFF000000 | (slotColor & 0x00FFFFFF);
                context.fill(swatchX, swatchY, swatchX + 8, swatchY + 8, 0xFF000000);
                context.fill(swatchX + 1, swatchY + 1, swatchX + 7, swatchY + 7, color);
            }
        }
    }

    private void drawTargetScrollbar(DrawContext context, int guiLeft, int guiTop) {
        int trackX = guiLeft + TARGET_SCROLLBAR_X;
        int trackY = guiTop + TARGET_SCROLLBAR_Y;

        context.fill(trackX, trackY, trackX + TARGET_SCROLLBAR_WIDTH, trackY + TARGET_SCROLLBAR_HEIGHT, SCROLLBAR_TRACK_COLOR);

        int totalEntries = ItemRetrievalModClient.getSearchTargetCount();
        int maxOffset = getTargetListMaxRowOffset();
        if (maxOffset <= 0) {
            context.fill(trackX + 1, trackY + 1, trackX + TARGET_SCROLLBAR_WIDTH - 1, trackY + TARGET_SCROLLBAR_HEIGHT - 1, SCROLLBAR_IDLE_FILL_COLOR);
            return;
        }

        int thumbHeight = getTargetScrollbarThumbHeight(totalEntries);
        int movableHeight = TARGET_SCROLLBAR_HEIGHT - thumbHeight;
        float offsetProgress = (float) targetListRowOffset / (float) maxOffset;
        int thumbY = trackY + Math.round(offsetProgress * movableHeight);

        int thumbColor = draggingTargetScrollbar ? SCROLLBAR_THUMB_DRAGGING_COLOR : SCROLLBAR_THUMB_COLOR;
        context.fill(trackX + 1, thumbY, trackX + TARGET_SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, thumbColor);
    }

    private boolean isMouseOverTargetArea(double mouseX, double mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        int minX = guiLeft + TARGET_LIST_X + 2;
        int minY = guiTop + TARGET_LIST_Y + 2;
        int maxX = minX + TARGET_LIST_WIDTH - TARGET_SCROLLBAR_WIDTH - 4;
        int maxY = minY + TARGET_VISIBLE_ROWS * TARGET_ROW_HEIGHT;

        return mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY;
    }

    private boolean isMouseOverTargetScrollbar(double mouseX, double mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        int minX = guiLeft + TARGET_SCROLLBAR_X;
        int minY = guiTop + TARGET_SCROLLBAR_Y;
        int maxX = minX + TARGET_SCROLLBAR_WIDTH;
        int maxY = minY + TARGET_SCROLLBAR_HEIGHT;

        return mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY;
    }

    private int getTargetSlotAt(double mouseX, double mouseY) {
        int localRow = getTargetLocalRow(mouseX, mouseY);
        if (localRow < 0) {
            return -1;
        }

        List<Integer> targetSlots = collectFilledTargetSlots();
        int globalRow = targetListRowOffset + localRow;
        if (globalRow < 0 || globalRow >= targetSlots.size()) {
            return -1;
        }

        return targetSlots.get(globalRow);
    }

    private int getTargetLocalRow(double mouseX, double mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        int minX = guiLeft + TARGET_LIST_X + 2;
        int minY = guiTop + TARGET_LIST_Y + 2;
        int maxX = minX + TARGET_LIST_WIDTH - TARGET_SCROLLBAR_WIDTH - 4;
        int maxY = minY + TARGET_VISIBLE_ROWS * TARGET_ROW_HEIGHT;

        if (mouseX < minX || mouseX >= maxX || mouseY < minY || mouseY >= maxY) {
            return -1;
        }

        int localRow = (int) ((mouseY - minY) / TARGET_ROW_HEIGHT);
        if (localRow < 0 || localRow >= TARGET_VISIBLE_ROWS) {
            return -1;
        }
        return localRow;
    }

    private boolean isMouseOverTargetToggleZone(double mouseX, double mouseY) {
        int localRow = getTargetLocalRow(mouseX, mouseY);
        if (localRow < 0) {
            return false;
        }

        int guiLeft = (width - backgroundWidth) / 2;
        int rowX = guiLeft + TARGET_LIST_X + 2;
        int rowWidth = TARGET_LIST_WIDTH - TARGET_SCROLLBAR_WIDTH - 4;
        int toggleMinX = rowX + rowWidth - 60;
        int toggleMaxX = rowX + rowWidth - 13;

        return mouseX >= toggleMinX && mouseX < toggleMaxX;
    }

    private int findLastFilledOptionSlot() {
        Inventory searchOptions = handler.getSearchOptions();
        for (int i = searchOptions.size() - 1; i >= 0; i--) {
            if (!searchOptions.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack getSelectedCatalogStack() {
        if (!showingCatalogResults || selectedCatalogResultGlobalIndex < 0) {
            return ItemStack.EMPTY;
        }

        if (selectedCatalogResultGlobalIndex >= catalogResults.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack selected = catalogResults.get(selectedCatalogResultGlobalIndex);
        if (selected.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return selected.copyWithCount(1);
    }

    private void drawSelectedResultSlotFrame(DrawContext context, int guiLeft, int guiTop, int resultSlotIndex) {
        int row = resultSlotIndex / RESULT_COLUMNS;
        int col = resultSlotIndex % RESULT_COLUMNS;
        int slotX = guiLeft + RESULT_SLOT_START_X + col * RESULT_SLOT_SPACING;
        int slotY = guiTop + RESULT_SLOT_START_Y + row * RESULT_SLOT_SPACING;
        drawSelectedSlotFrame(context, slotX, slotY);
    }

    private void syncRadiusFieldWithRuntimeValue() {
        if (radiusField == null || radiusField.isFocused()) {
            return;
        }

        String runtimeValue = Integer.toString(ItemRetrievalModClient.getSearchRadiusBlocks());
        if (!runtimeValue.equals(radiusField.getText())) {
            setRadiusFieldText(runtimeValue);
        }
    }

    private void onRadiusFieldChanged(String rawValue) {
        if (updatingRadiusFieldText) {
            return;
        }

        if (rawValue == null) {
            return;
        }

        String digitsOnly = rawValue.replaceAll("[^0-9]", "");
        if (!digitsOnly.equals(rawValue)) {
            setRadiusFieldText(digitsOnly);
            return;
        }

        if (digitsOnly.isEmpty()) {
            return;
        }

        try {
            int parsedValue = Integer.parseInt(digitsOnly);
            setSearchRadiusAndSyncField(parsedValue);
        } catch (NumberFormatException ignored) {
            setRadiusFieldText(Integer.toString(ItemRetrievalModClient.getSearchRadiusBlocks()));
        }
    }

    private void commitRadiusFieldInput() {
        if (radiusField == null) {
            return;
        }

        String text = radiusField.getText();
        if (text == null || text.isEmpty()) {
            setRadiusFieldText(Integer.toString(ItemRetrievalModClient.getSearchRadiusBlocks()));
            return;
        }

        try {
            setSearchRadiusAndSyncField(Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
            setRadiusFieldText(Integer.toString(ItemRetrievalModClient.getSearchRadiusBlocks()));
        }
    }

    private void setSearchRadiusAndSyncField(int requestedRadius) {
        int clampedRequested = clampRadius(requestedRadius);
        int appliedRadius = ItemRetrievalModClient.setSearchRadiusBlocks(clampedRequested);
        setRadiusFieldText(Integer.toString(appliedRadius));
    }

    private static int clampRadius(int value) {
        return Math.max(RADIUS_MIN_VALUE, Math.min(RADIUS_MAX_VALUE, value));
    }

    private void setRadiusFieldText(String value) {
        if (radiusField == null) {
            return;
        }

        updatingRadiusFieldText = true;
        try {
            radiusField.setText(value);
        } finally {
            updatingRadiusFieldText = false;
        }
    }

    private void runCatalogItemSearch(boolean notifyPlayer) {
        if (searchField == null) {
            return;
        }

        String query = normalizeQuery(searchField.getText());
        if (query.isEmpty()) {
            showingCatalogResults = false;
            catalogResults.clear();
            catalogResultRowOffset = 0;
            selectedCatalogResultGlobalIndex = -1;
            clearResultDisplayInventory();
            if (notifyPlayer && client != null && client.player != null) {
                client.player.sendMessage(Text.literal("请输入要搜索的物品名称或 minecraft:id。"), true);
            }
            return;
        }

        List<CatalogMatch> matches = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }

            Identifier id = Registries.ITEM.getId(item);
            if (id == null) {
                continue;
            }

            String idText = id.toString();
            String nameText = item.getName().getString();
            int score = computeMatchScore(query, idText, nameText);
            if (score != Integer.MAX_VALUE) {
                matches.add(new CatalogMatch(item, idText, score));
            }
        }

        matches.sort(Comparator
                .comparingInt(CatalogMatch::score)
                .thenComparing(CatalogMatch::idText));

        catalogResults.clear();
        for (CatalogMatch match : matches) {
            catalogResults.add(new ItemStack(match.item()));
        }

        showingCatalogResults = true;
        catalogResultRowOffset = 0;
        selectedCatalogResultGlobalIndex = catalogResults.isEmpty() ? -1 : 0;
        refreshCatalogResultsPage();

        if (notifyPlayer && client != null && client.player != null) {
            if (matches.isEmpty()) {
                client.player.sendMessage(Text.literal("没有匹配结果，试试更短关键词或模糊词。"), true);
            } else {
                int shownCount = Math.min(RESULT_PAGE_SIZE, matches.size());
                client.player.sendMessage(Text.literal(
                        "已找到 " + matches.size() + " 个匹配，当前展示前 " + shownCount + " 个。"
                ), true);
            }
        }
    }

    private void refreshCatalogResultsPage() {
        Inventory searchResults = handler.getSearchResults();
        for (int slotIndex = 0; slotIndex < searchResults.size(); slotIndex++) {
            int globalIndex = catalogResultRowOffset + slotIndex;
            if (globalIndex >= 0 && globalIndex < catalogResults.size()) {
                searchResults.setStack(slotIndex, catalogResults.get(globalIndex).copyWithCount(1));
            } else {
                searchResults.setStack(slotIndex, ItemStack.EMPTY);
            }
        }
        searchResults.markDirty();
    }

    private void clearResultDisplayInventory() {
        Inventory searchResults = handler.getSearchResults();
        for (int i = 0; i < searchResults.size(); i++) {
            searchResults.setStack(i, ItemStack.EMPTY);
        }
        searchResults.markDirty();
    }

    private int getCurrentResultTotalCount() {
        if (showingCatalogResults) {
            return catalogResults.size();
        }

        return ItemRetrievalModClient.getSearchResultCount();
    }

    private int getCurrentResultRowOffset() {
        if (showingCatalogResults) {
            return catalogResultRowOffset;
        }

        return ItemRetrievalModClient.getSearchResultRowOffset();
    }

    private int getCurrentResultMaxRowOffset() {
        if (showingCatalogResults) {
            return computeMaxRowOffset(catalogResults.size());
        }

        return ItemRetrievalModClient.getSearchResultMaxRowOffset();
    }

    private boolean hasCurrentScrollableResults() {
        return getCurrentResultMaxRowOffset() > 0;
    }

    private boolean scrollCurrentResultsByRows(int rowDelta) {
        if (rowDelta == 0) {
            return false;
        }

        return setCurrentResultRowOffset(getCurrentResultRowOffset() + rowDelta * RESULT_COLUMNS);
    }

    private boolean setCurrentResultRowOffset(int requestedOffset) {
        if (showingCatalogResults) {
            int maxOffset = computeMaxRowOffset(catalogResults.size());
            int snappedOffset = snapOffsetToRow(requestedOffset);
            int clampedOffset = Math.max(0, Math.min(maxOffset, snappedOffset));

            if (clampedOffset == catalogResultRowOffset) {
                return false;
            }

            catalogResultRowOffset = clampedOffset;
            refreshCatalogResultsPage();
            return true;
        }

        return ItemRetrievalModClient.setSearchResultRowOffset(requestedOffset);
    }

    private void setCurrentResultOffsetFromMouse(double mouseY) {
        int maxOffset = getCurrentResultMaxRowOffset();
        if (maxOffset <= 0) {
            return;
        }

        int guiTop = (height - backgroundHeight) / 2;
        int trackY = guiTop + RESULT_SCROLLBAR_Y;
        int thumbHeight = getResultScrollbarThumbHeight(getCurrentResultTotalCount());
        int movableHeight = RESULT_SCROLLBAR_HEIGHT - thumbHeight;
        if (movableHeight <= 0) {
            setCurrentResultRowOffset(0);
            return;
        }

        double normalized = (mouseY - trackY - (thumbHeight / 2.0D)) / (double) movableHeight;
        double clamped = Math.max(0.0D, Math.min(1.0D, normalized));
        int targetOffset = (int) Math.round(clamped * maxOffset);
        setCurrentResultRowOffset(targetOffset);
    }

    private int getResultScrollbarThumbHeight(int totalEntries) {
        if (totalEntries <= RESULT_PAGE_SIZE) {
            return RESULT_SCROLLBAR_HEIGHT;
        }

        return Math.max(10, Math.round((RESULT_PAGE_SIZE / (float) totalEntries) * RESULT_SCROLLBAR_HEIGHT));
    }

    private void drawResultScrollbar(DrawContext context, int guiLeft, int guiTop) {
        int trackX = guiLeft + RESULT_SCROLLBAR_X;
        int trackY = guiTop + RESULT_SCROLLBAR_Y;

        context.fill(trackX, trackY, trackX + RESULT_SCROLLBAR_WIDTH, trackY + RESULT_SCROLLBAR_HEIGHT, SCROLLBAR_TRACK_COLOR);

        int totalEntries = getCurrentResultTotalCount();
        int maxOffset = getCurrentResultMaxRowOffset();
        if (maxOffset <= 0) {
            context.fill(trackX + 1, trackY + 1, trackX + RESULT_SCROLLBAR_WIDTH - 1, trackY + RESULT_SCROLLBAR_HEIGHT - 1, SCROLLBAR_IDLE_FILL_COLOR);
            return;
        }

        int thumbHeight = getResultScrollbarThumbHeight(totalEntries);
        int movableHeight = RESULT_SCROLLBAR_HEIGHT - thumbHeight;
        float offsetProgress = (float) getCurrentResultRowOffset() / (float) maxOffset;
        int thumbY = trackY + Math.round(offsetProgress * movableHeight);

        int thumbColor = draggingResultScrollbar ? SCROLLBAR_THUMB_DRAGGING_COLOR : SCROLLBAR_THUMB_COLOR;
        context.fill(trackX + 1, thumbY, trackX + RESULT_SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, thumbColor);
    }

    private boolean isMouseOverResultArea(double mouseX, double mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        int minX = guiLeft + RESULT_SLOT_START_X;
        int minY = guiTop + RESULT_SLOT_START_Y;
        int maxX = minX + RESULT_COLUMNS * RESULT_SLOT_SPACING;
        int maxY = minY + RESULT_ROWS * RESULT_SLOT_SPACING;

        return mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY;
    }

    private boolean isMouseOverResultScrollbar(double mouseX, double mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop = (height - backgroundHeight) / 2;

        int minX = guiLeft + RESULT_SCROLLBAR_X;
        int minY = guiTop + RESULT_SCROLLBAR_Y;
        int maxX = minX + RESULT_SCROLLBAR_WIDTH;
        int maxY = minY + RESULT_SCROLLBAR_HEIGHT;

        return mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY;
    }

    private static int computeMaxRowOffset(int totalEntries) {
        if (totalEntries <= 0) {
            return 0;
        }

        int totalRows = (totalEntries + RESULT_COLUMNS - 1) / RESULT_COLUMNS;
        int overflowRows = Math.max(0, totalRows - RESULT_ROWS);
        return overflowRows * RESULT_COLUMNS;
    }

    private static int snapOffsetToRow(int offset) {
        return Math.floorDiv(offset, RESULT_COLUMNS) * RESULT_COLUMNS;
    }

    private void drawSearchPlaceholder(DrawContext context) {
        if (searchField == null || searchField.isFocused() || !searchField.getText().isEmpty()) {
            return;
        }

        context.drawText(
                textRenderer,
                SEARCH_PLACEHOLDER_TEXT,
                searchField.getX() + 4,
                searchField.getY() + 3,
                SEARCH_PLACEHOLDER_COLOR,
                false
        );
    }

    private static int computeMatchScore(String query, String itemId, String itemName) {
        String idLower = itemId.toLowerCase(Locale.ROOT);
        String pathLower = idLower;
        int colonIndex = idLower.indexOf(':');
        if (colonIndex >= 0 && colonIndex + 1 < idLower.length()) {
            pathLower = idLower.substring(colonIndex + 1);
        }

        String nameLower = itemName.toLowerCase(Locale.ROOT);
        String[] tokens = query.split(" ");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (!matchesToken(token, idLower, pathLower, nameLower)) {
                return Integer.MAX_VALUE;
            }
        }

        if (idLower.equals(query) || pathLower.equals(query)) {
            return 0;
        }
        if (nameLower.equals(query)) {
            return 1;
        }
        if (idLower.startsWith(query) || pathLower.startsWith(query)) {
            return 2;
        }
        if (nameLower.startsWith(query)) {
            return 3;
        }
        if (idLower.contains(query) || pathLower.contains(query)) {
            return 4;
        }
        if (nameLower.contains(query)) {
            return 5;
        }
        return 6;
    }

    private static boolean matchesToken(String token, String idLower, String pathLower, String nameLower) {
        return idLower.contains(token)
                || pathLower.contains(token)
                || nameLower.contains(token)
                || isSubsequence(token, idLower)
                || isSubsequence(token, pathLower)
                || isSubsequence(token, nameLower);
    }

    private static boolean isSubsequence(String token, String content) {
        int tokenIndex = 0;
        for (int i = 0; i < content.length() && tokenIndex < token.length(); i++) {
            if (content.charAt(i) == token.charAt(tokenIndex)) {
                tokenIndex++;
            }
        }
        return tokenIndex == token.length();
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        String normalized = query.toLowerCase(Locale.ROOT).trim();
        if (normalized.isEmpty()) {
            return "";
        }

        return normalized.replaceAll("\\s+", " ");
    }

    private void drawSelectedSlotFrame(DrawContext context, int slotX, int slotY) {
        context.fill(slotX - 2, slotY - 2, slotX + 18, slotY - 1, SLOT_SELECTION_COLOR);
        context.fill(slotX - 2, slotY + 17, slotX + 18, slotY + 18, SLOT_SELECTION_COLOR);
        context.fill(slotX - 2, slotY - 1, slotX - 1, slotY + 17, SLOT_SELECTION_COLOR);
        context.fill(slotX + 17, slotY - 1, slotX + 18, slotY + 17, SLOT_SELECTION_COLOR);
        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x18000000);
    }

    private void drawFramedPanel(DrawContext context, int x, int y, int panelWidth, int panelHeight, int fillColor) {
        int x2 = x + panelWidth;
        int y2 = y + panelHeight;

        // 1px black outer outline.
        context.fill(x, y, x2, y2, FRAME_OUTLINE_EDGE);

        // Inner panel fill.
        context.fill(x + 1, y + 1, x2 - 1, y2 - 1, fillColor);

        // 1px bevel on inner edge: top/left light, bottom/right dark.
        context.fill(x + 1, y + 1, x2 - 1, y + 2, FRAME_LIGHT_EDGE);
        context.fill(x + 1, y + 1, x + 2, y2 - 1, FRAME_LIGHT_EDGE);
        context.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, FRAME_DARK_EDGE);
        context.fill(x2 - 2, y + 1, x2 - 1, y2 - 1, FRAME_DARK_EDGE);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }

    public void setSearchResults(ItemStack result) {
        // reserved
    }

    private record CatalogMatch(Item item, String idText, int score) {
    }
}