package me.sandreamin.mods.san_essentials;

/**
 * Prints the startup banner shared by the Fabric and NeoForge entrypoints.
 */
final class SanStartupBanner {
    private static final String ASCII_ART = String.join("\n",
            "mmmmm                        mmmmm                                                ##",
            " m#\"\"\"\"#                       ##\"\"\"##                                              \"\"",
            " ##m        m#####m  ##m####m  ##    ##   ##m####   m####m    m#####m  ####m##m   ####     ##m####m",
            "  \"####m    \" mmm##  ##\"   ##  ##    ##   ##\"      ##mmmm##   \" mmm##  ## ## ##     ##     ##\"   ##",
            "      \"##  m##\"\"\"##  ##    ##  ##    ##   ##       ##\"\"\"\"\"\"  m##\"\"\"##  ## ## ##     ##     ##    ##",
            " #mmmmm#\"  ##mmm###  ##    ##  ##mmm##    ##       \"##mmmm#  ##mmm###  ## ## ##  mmm##mmm  ##    ##",
            "  \"\"\"\"\"     \"\"\"\" \"\"  \"\"    \"\"  \"\"\"\"\"      \"\"         \"\"\"\"\"    \"\"\"\" \"\"  \"\" \"\" \"\"  \"\"\"\"\"\"\"\"  \"\"    \"\""
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
