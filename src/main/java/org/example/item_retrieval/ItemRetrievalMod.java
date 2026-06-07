package org.example.item_retrieval;

import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.example.item_retrieval.screen.SearchScreenHandler;

/**
 * 模组主入口（服务端/通用初始化）。
 */
public class ItemRetrievalMod implements ModInitializer {

    public static final String MOD_ID = "items-retrieval";

    /** ScreenHandler 注册 ID。 */
    private static final Identifier SEARCH_SCREEN_ID =
        Identifier.of(MOD_ID, "search_screen");

    /** 客户端检索面板使用的 ScreenHandler 类型。 */
    public static final ScreenHandlerType<SearchScreenHandler> SEARCH_SCREEN_HANDLER =
            new ScreenHandlerType<>(SearchScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

    @Override
    public void onInitialize() {
    // 注册 GUI 对应的 ScreenHandler，供客户端创建检索面板容器。 
        Registry.register(Registries.SCREEN_HANDLER,
        SEARCH_SCREEN_ID,
                SEARCH_SCREEN_HANDLER);
    }
}
