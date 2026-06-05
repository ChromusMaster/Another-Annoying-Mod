# Annoying Cross RegistryKey Fix

Fixes Minecraft 1.21.11 startup crash:

Caused by: java.lang.NullPointerException: Block id not set

The custom block now sets RegistryKey on AbstractBlock.Settings before creating AnnoyingCrossBlock.
The block item also receives RegistryKey on Item.Settings.
