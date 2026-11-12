package me.sandreamin.mods.san_essentials;

/**
 * Prints the startup banner shared by the loader entrypoints.
 */
final class SanStartupBanner {
    private static final String ASCII_ART = String.join("\n",
            "╭───────╮                       ╭───────╮                                           ╭─╮     ╭─╮             ╭─╮",
            "│ ╭─────╯ ╭───────╮   ╭───────╮ │ ╭─────╯   ╭─────╮   ╭─────╮   ╭───╮   ╭───────╮ ╭─╯ ╰─╮   ╰─╯ ╭───────╮   │ │   ╭─────╮",
            "│ ╰─────╮ │ ╭───╮ │   │ ╭───╮ │ │ ╰───╮   ╭─╯   ╭─╯ ╭─╯   ╭─╯ ╭─╯   ╰─╮ │ ╭───╮ │ ╰─╮ ╭─╯   ╭─╮ │ ╭───╮ │   │ │ ╭─╯   ╭─╯",
            "╰─────╮ │ │ │   │ │   │ │   │ │ │ ╭───╯   ╰───╮ ╰─╮ ╰───╮ ╰─╮ │ ╭─────╯ │ │   │ │   │ │     │ │ │ │   │ │   │ │ ╰───╮ ╰─╮",
            "╭─────╯ │ │ ╰───╯ ╰─╮ │ │   │ │ │ ╰─────╮ ╭───╯ ╭─╯ ╭───╯ ╭─╯ │ ╰─────╮ │ │   │ │   │ ╰───╮ │ │ │ ╰───╯ ╰─╮ │ │ ╭───╯ ╭─╯",
            "╰───────╯ ╰─────────╯ ╰─╯   ╰─╯ ╰───────╯ ╰─────╯   ╰─────╯   ╰───────╯ ╰─╯   ╰─╯   ╰─────╯ ╰─╯ ╰─────────╯ ╰─╯ ╰─────╯"
    );

    private SanStartupBanner() {
    }

    static void logLoading() {
        SanEssentials.LOGGER.info("\n{}\nFork of PPE Essentials by Purple_East", ASCII_ART);
    }

    static void logLoaded() {
        SanEssentials.LOGGER.info("San Essentials loaded");
    }
}
