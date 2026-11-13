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
 * Builds and updates the shared inventory contents for the homes menu.
 */
final class SanHomeMenuContent {
    static final int MENU_SIZE = 54;
    static final int HOMES_PER_PAGE = 45;

    private static final int PREVIOUS_SLOT = 45;
    private static final int CLOSE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final SimpleContainer container;
    private final ServerPlayer owner;
    private int page;
    private int totalPages;
    private List<String> homeSlots = List.of();

    SanHomeMenuContent(SimpleContainer container, ServerPlayer owner, int requestedPage) {
        this.container = container;
        this.owner = owner;
        refresh(requestedPage);
    }

    static Component title(ServerPlayer player, int requestedPage) {
        List<String> homes = homeNames(player);
        int totalPages = pageCount(homes.size());
        int page = clampPage(requestedPage, totalPages);
        return SanLang.component(player, "san_essentials.homes.gui.title",
                page + 1, totalPages, homes.size(), SanConfig.maxHomes());
    }

    static int currentPage(ServerPlayer player, int requestedPage) {
        return clampPage(requestedPage, pageCount(homeNames(player).size()));
    }

    ServerPlayer owner() {
        return owner;
    }

    boolean click(int slot) {
        if (slot < HOMES_PER_PAGE) {
            return clickHome(slot);
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
        List<String> homes = homeNames(owner);
        totalPages = pageCount(homes.size());
        page = clampPage(requestedPage, totalPages);
        container.clearContent();
        homeSlots = populate(homes);
    }

    private List<String> populate(List<String> homes) {
        List<String> visibleHomes = new ArrayList<>(HOMES_PER_PAGE);
        int first = page * HOMES_PER_PAGE;
        int last = Math.min(first + HOMES_PER_PAGE, homes.size());
        for (int i = first; i < last; i++) {
            String homeName = homes.get(i);
            visibleHomes.add(homeName);
            container.setItem(i - first, homeItem(homeName));
        }

        ItemStack divider = namedItem(Items.GRAY_STAINED_GLASS_PANE, "san_essentials.teleport_menu.blank");
        for (int slot = HOMES_PER_PAGE; slot < MENU_SIZE; slot++) {
            container.setItem(slot, divider.copy());
        }
        container.setItem(PREVIOUS_SLOT, buttonItem(
                Items.ARROW,
                "san_essentials.homes.gui.previous",
                List.of(text("san_essentials.homes.gui.page", previousPage() + 1))
        ));
        container.setItem(CLOSE_SLOT, namedItem(Items.BARRIER, "san_essentials.homes.gui.close"));
        container.setItem(NEXT_SLOT, buttonItem(
                Items.ARROW,
                "san_essentials.homes.gui.next",
                List.of(text("san_essentials.homes.gui.page", nextPage() + 1))
        ));
        return List.copyOf(visibleHomes);
    }

    private ItemStack homeItem(String homeName) {
        ItemStack item = namedItem(Items.RED_BED, "san_essentials.homes.gui.entry", homeName);
        item.set(DataComponents.LORE, new ItemLore(List.of(
                text("san_essentials.homes.gui.entry.tooltip")
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

    private boolean clickHome(int slot) {
        if (slot >= homeSlots.size()) {
            return false;
        }
        if (!SanCommands.requestHomeFromMenu(owner, homeSlots.get(slot))) {
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
        SanCompat.openHomeMenu(owner, targetPage);
    }

    private int previousPage() {
        return (page + totalPages - 1) % totalPages;
    }

    private int nextPage() {
        return (page + 1) % totalPages;
    }

    private static List<String> homeNames(ServerPlayer player) {
        return SanPlayerData.get(SanCompat.server(player)).homeNames(player.getUUID()).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static int pageCount(int homes) {
        return Math.max(1, (homes + HOMES_PER_PAGE - 1) / HOMES_PER_PAGE);
    }

    private static int clampPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }
}
