package dev.kiddo.headguitest;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class HeadGui {

    private static final int ROWS = 6;
    private static final int SLOTS_PER_PAGE = 45;
    private static final int TOTAL_SLOTS = 54;

    private static final int SLOT_PREV = 45;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 53;

    // We store the heads globally so we can access them when reopening screens
    private static final List<ItemStack> cachedHeads = new ArrayList<>();

    public static void openItems(List<ItemStack> preMadeHeads) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        cachedHeads.clear();
        cachedHeads.addAll(preMadeHeads);

        // Start at page 0
        openPage(client, client.player, 0);
    }

    private static void openPage(MinecraftClient client, PlayerEntity player, int page) {
        SimpleInventory inventory = new SimpleInventory(TOTAL_SLOTS);

        // --- 1. Fill Inventory for this specific page ---
        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, cachedHeads.size());

        for (int i = start; i < end; i++) {
            inventory.setStack(i - start, cachedHeads.get(i));
        }

        // --- 2. Add Buttons ---
        if (page > 0) {
            inventory.setStack(SLOT_PREV, createButton(Items.ARROW, "Previous Page"));
        }
        inventory.setStack(SLOT_CLOSE, createButton(Items.BARRIER, "Close"));

        if (end < cachedHeads.size()) {
            inventory.setStack(SLOT_NEXT, createButton(Items.ARROW, "Next Page"));
        }

        // --- 3. Create Handler (Client Side Only) ---
        GenericContainerScreenHandler handler = new GenericContainerScreenHandler(
                ScreenHandlerType.GENERIC_9X6,
                -1, // SyncId -1 prevents server mismatch
                player.getInventory(),
                inventory,
                ROWS
        ) {
            @Override
            public boolean canUse(PlayerEntity player) {
                return true;
            }

            @Override
            public ItemStack quickMove(PlayerEntity player, int slot) {
                return ItemStack.EMPTY;
            }
        };

        // --- 4. Create Screen ---
        GenericContainerScreen screen = new GenericContainerScreen(
                handler,
                player.getInventory(),
                Text.literal("Shop Head Database").formatted(Formatting.DARK_GRAY, Formatting.BOLD)
        ) {
            @Override
            protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
                // If clicked outside
                if (slot == null) return;

                // Handle Navigation - REOPEN SCREEN with new page
                if (slotId == SLOT_PREV && page > 0) {
                    openPage(client, player, page - 1);
                } else if (slotId == SLOT_NEXT && (page + 1) * SLOTS_PER_PAGE < cachedHeads.size()) {
                    openPage(client, player, page + 1);
                } else if (slotId == SLOT_CLOSE) {
                    client.setScreen(null);
                }
            }
        };

        // Open the new screen
        client.setScreen(screen);
    }

    private static ItemStack createButton(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.YELLOW));
        return stack;
    }
}