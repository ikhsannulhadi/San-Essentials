package me.sandreamin.mods.san_essentials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class SanHomeMenu extends ChestMenu {
    private final SanHomeMenuContent content;

    private SanHomeMenu(int containerId, Inventory inventory, ServerPlayer owner, int requestedPage) {
        this(containerId, inventory, new SimpleContainer(SanHomeMenuContent.MENU_SIZE), owner, requestedPage);
    }

    private SanHomeMenu(
            int containerId,
            Inventory inventory,
            SimpleContainer container,
            ServerPlayer owner,
            int requestedPage
    ) {
        super(MenuType.GENERIC_9x6, containerId, inventory, container, 6);
        this.content = new SanHomeMenuContent(container, owner, requestedPage);
    }

    static void open(ServerPlayer player, int requestedPage) {
        int page = SanHomeMenuContent.currentPage(player, requestedPage);
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new SanHomeMenu(containerId, inventory, player, page),
                SanHomeMenuContent.title(player, page)
        );
        player.openMenu(provider);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (player != content.owner()) {
            return;
        }
        if (slotId < 0
                || slotId >= SanHomeMenuContent.MENU_SIZE
                || clickType != ClickType.PICKUP
                || !content.click(slotId)) {
            broadcastFullState();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return false;
    }
}
