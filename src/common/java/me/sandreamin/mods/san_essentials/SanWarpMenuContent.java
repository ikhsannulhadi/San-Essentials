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

/**
 * Builds and updates the shared inventory contents for the warp menu.
 */
final class SanWarpMenuContent {
    static final int MENU_SIZE = 54;
    static final int WARPS_PER_PAGE = 45;

    private static final int PREVIOUS_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final SimpleContainer container;
    private final ServerPlayer owner;
    private int page;
    private int totalPages;
    private List<String> warpSlots = List.of();

    SanWarpMenuContent(SimpleContainer container, ServerPlayer owner, int requestedPage) {
        this.container = container;
        this.owner = owner;
        refresh(requestedPage);
    }

    static Component title(ServerPlayer player, int requestedPage) {
        int totalPages = pageCount(warpNames(player).size());
        int page = clampPage(requestedPage, totalPages);
        return SanLang.component(player, "san_essentials.warps.title", page + 1, totalPages);
    }

    static int currentPage(ServerPlayer player, int requestedPage) {
        return clampPage(requestedPage, pageCount(warpNames(player).size()));
    }

    ServerPlayer owner() {
        return owner;
    }

    boolean click(int slot) {
        if (slot < WARPS_PER_PAGE) {
            return clickWarp(slot);
        }
        if (slot == PREVIOUS_SLOT) {
            return changePage(previousPage());
        }
        if (slot == CLOSE_SLOT) {
            owner.closeContainer();
            return true;
        }
        if (slot == NEXT_SLOT) {
            return changePage(nextPage());
        }
        return false;
    }

    private void refresh(int requestedPage) {
        List<String> warps = warpNames(owner);
        totalPages = pageCount(warps.size());
        page = clampPage(requestedPage, totalPages);
        container.clearContent();
        warpSlots = populate(warps);
    }

    private List<String> populate(List<String> warps) {
        List<String> visibleWarps = new ArrayList<>(WARPS_PER_PAGE);
        int first = page * WARPS_PER_PAGE;
        int last = Math.min(first + WARPS_PER_PAGE, warps.size());
        for (int i = first; i < last; i++) {
            String warpName = warps.get(i);
            visibleWarps.add(warpName);
            container.setItem(i - first, warpItem(warpName));
        }

        ItemStack divider = namedItem(Items.GRAY_STAINED_GLASS_PANE, "san_essentials.teleport_menu.blank");
        for (int slot = WARPS_PER_PAGE; slot < MENU_SIZE; slot++) {
            container.setItem(slot, divider.copy());
        }
        container.setItem(PREVIOUS_SLOT, buttonItem(
                Items.ARROW,
                "san_essentials.warps.previous.name",
                List.of(text("san_essentials.warps.page", previousPage() + 1))
        ));
        container.setItem(CLOSE_SLOT, namedItem(Items.BARRIER, "san_essentials.warps.close"));
        container.setItem(NEXT_SLOT, buttonItem(
                Items.ARROW,
                "san_essentials.warps.next.name",
                List.of(text("san_essentials.warps.page", nextPage() + 1))
        ));
        return List.copyOf(visibleWarps);
    }

    private ItemStack warpItem(String warpName) {
        ItemStack item = namedItem(Items.ENDER_PEARL, "san_essentials.warps.entry", warpName);
        item.set(DataComponents.LORE, new ItemLore(List.of(
                text("san_essentials.warps.entry.tooltip")
        )));
        return item;
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

    private boolean clickWarp(int slot) {
        if (slot >= warpSlots.size()) {
            return false;
        }
        if (!SanCommands.requestWarpFromMenu(owner, warpSlots.get(slot))) {
            return false;
        }
        owner.closeContainer();
        return true;
    }

    private boolean changePage(int targetPage) {
        reopen(targetPage);
        return true;
    }

    private void reopen(int targetPage) {
        SanCompat.openWarpMenu(owner, targetPage);
    }

    private int previousPage() {
        return (page + totalPages - 1) % totalPages;
    }

    private int nextPage() {
        return (page + 1) % totalPages;
    }

    private static List<String> warpNames(ServerPlayer player) {
        return SanPlayerData.get(SanCompat.server(player)).warpNames().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static int pageCount(int warps) {
        return Math.max(1, (warps + WARPS_PER_PAGE - 1) / WARPS_PER_PAGE);
    }

    private static int clampPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }
}
