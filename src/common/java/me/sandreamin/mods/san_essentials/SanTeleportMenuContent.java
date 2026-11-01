package me.sandreamin.mods.san_essentials;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds and updates the shared inventory contents for paginated teleport menus.
 */
final class SanTeleportMenuContent {
    static final int MENU_SIZE = 54;
    static final int PLAYERS_PER_PAGE = 36;

    private static final int PREVIOUS_SLOT = 45;
    private static final int AUTO_ACCEPT_SLOT = 47;
    private static final int CLOSE_SLOT = 49;
    private static final int REFRESH_SLOT = 51;
    private static final int NEXT_SLOT = 53;

    private final SimpleContainer container;
    private final ServerPlayer owner;
    private final boolean here;
    private int page;
    private int totalPages;
    private List<UUID> playerSlots = List.of();

    SanTeleportMenuContent(SimpleContainer container, ServerPlayer owner, boolean here, int requestedPage) {
        this.container = container;
        this.owner = owner;
        this.here = here;
        refresh(requestedPage);
    }

    static Component title(ServerPlayer player, boolean here, int requestedPage) {
        int totalPages = pageCount(SanCommands.teleportMenuTargets(player).size());
        int page = clampPage(requestedPage, totalPages);
        String titleKey = here
                ? "san_essentials.teleport_menu.tpahere.title"
                : "san_essentials.teleport_menu.tpa.title";
        return SanLang.component(player, titleKey, page + 1, totalPages);
    }

    static int currentPage(ServerPlayer player, int requestedPage) {
        return clampPage(requestedPage, pageCount(SanCommands.teleportMenuTargets(player).size()));
    }

    ServerPlayer owner() {
        return owner;
    }

    boolean click(int slot) {
        if (slot < PLAYERS_PER_PAGE) {
            return clickPlayer(slot);
        }
        if (slot == PREVIOUS_SLOT) {
            return changePage(previousPage());
        }
        if (slot == AUTO_ACCEPT_SLOT && SanCommands.canToggleTpaAutoFromMenu(owner)) {
            SanCommands.toggleTpaAutoFromMenu(owner);
            return refreshCurrentPage();
        }
        if (slot == CLOSE_SLOT) {
            owner.closeContainer();
            return true;
        }
        if (slot == REFRESH_SLOT) {
            return refreshCurrentPage();
        }
        if (slot == NEXT_SLOT) {
            return changePage(nextPage());
        }
        return false;
    }

    private void refresh(int requestedPage) {
        List<ServerPlayer> targets = SanCommands.teleportMenuTargets(owner);
        totalPages = pageCount(targets.size());
        page = clampPage(requestedPage, totalPages);
        container.clearContent();
        playerSlots = populate(targets);
    }

    private boolean refreshCurrentPage() {
        List<ServerPlayer> targets = SanCommands.teleportMenuTargets(owner);
        int refreshedTotalPages = pageCount(targets.size());
        int refreshedPage = clampPage(page, refreshedTotalPages);
        if (refreshedPage != page || refreshedTotalPages != totalPages) {
            reopen(refreshedPage);
            return true;
        }

        container.clearContent();
        playerSlots = populate(targets);
        return false;
    }

    private List<UUID> populate(List<ServerPlayer> targets) {
        List<UUID> visiblePlayers = new ArrayList<>(PLAYERS_PER_PAGE);
        int first = page * PLAYERS_PER_PAGE;
        int last = Math.min(first + PLAYERS_PER_PAGE, targets.size());
        for (int i = first; i < last; i++) {
            ServerPlayer target = targets.get(i);
            visiblePlayers.add(target.getUUID());
            container.setItem(i - first, playerHead(target));
        }

        ItemStack divider = namedItem(Items.GRAY_STAINED_GLASS_PANE, "san_essentials.teleport_menu.blank");
        for (int slot = PLAYERS_PER_PAGE; slot < MENU_SIZE; slot++) {
            container.setItem(slot, divider.copy());
        }
        container.setItem(PREVIOUS_SLOT, buttonItem(
                Items.ARROW,
                "san_essentials.teleport_menu.previous.name",
                List.of(text("san_essentials.teleport_menu.page", previousPage() + 1))
        ));
        if (SanCommands.canToggleTpaAutoFromMenu(owner)) {
            boolean enabled = SanCommands.isTpaAutoEnabled(owner);
            container.setItem(AUTO_ACCEPT_SLOT, buttonItem(
                    Items.OAK_PRESSURE_PLATE,
                    enabled
                            ? "san_essentials.teleport_menu.auto.enabled.name"
                            : "san_essentials.teleport_menu.auto.disabled.name",
                    List.of(
                            text("san_essentials.teleport_menu.auto.description"),
                            Component.empty(),
                            text(enabled
                                    ? "san_essentials.teleport_menu.auto.disable"
                                    : "san_essentials.teleport_menu.auto.enable")
                    )
            ));
        }
        container.setItem(CLOSE_SLOT, namedItem(Items.BARRIER, "san_essentials.teleport_menu.close"));
        container.setItem(REFRESH_SLOT, buttonItem(
                Items.CLOCK,
                "san_essentials.teleport_menu.refresh.name",
                List.of(text("san_essentials.teleport_menu.refresh.description"))
        ));
        container.setItem(NEXT_SLOT, buttonItem(
                Items.ARROW,
                "san_essentials.teleport_menu.next.name",
                List.of(text("san_essentials.teleport_menu.page", nextPage() + 1))
        ));
        return List.copyOf(visiblePlayers);
    }

    private ItemStack playerHead(ServerPlayer target) {
        ItemStack head = namedItem(
                Items.PLAYER_HEAD,
                "san_essentials.teleport_menu.player.name",
                SanCompat.profileName(target)
        );
        List<Component> lore = new ArrayList<>();
        if (!here && SanCommands.isTpaAutoEnabled(target)) {
            lore.add(text("san_essentials.teleport_menu.player.auto"));
            lore.add(Component.empty());
        }
        lore.add(text(here
                ? "san_essentials.teleport_menu.player.tpahere"
                : "san_essentials.teleport_menu.player.tpa"));
        head.set(DataComponents.LORE, new ItemLore(lore));
        SanCompat.setPlayerHeadProfile(head, target);
        return head;
    }

    private ItemStack namedItem(Item item, String nameKey, Object... nameArgs) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, text(nameKey, nameArgs));
        return stack;
    }

    private ItemStack buttonItem(Item item, String nameKey, List<Component> lore) {
        ItemStack stack = namedItem(item, nameKey);
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private Component text(String key, Object... args) {
        return SanLang.component(owner, key, args).withStyle(style -> style.withItalic(false));
    }

    private boolean clickPlayer(int slot) {
        if (slot >= playerSlots.size()) {
            return false;
        }
        if (!SanCommands.requestTeleportFromMenu(owner, playerSlots.get(slot), here)) {
            return false;
        }
        owner.closeContainer();
        return true;
    }

    private boolean changePage(int targetPage) {
        if (targetPage == page) {
            return refreshCurrentPage();
        }
        reopen(targetPage);
        return true;
    }

    private void reopen(int targetPage) {
        SanCompat.openTeleportMenu(owner, here, targetPage);
    }

    private int previousPage() {
        return (page + totalPages - 1) % totalPages;
    }

    private int nextPage() {
        return (page + 1) % totalPages;
    }

    private static int pageCount(int players) {
        return Math.max(1, (players + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
    }

    private static int clampPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }
}
