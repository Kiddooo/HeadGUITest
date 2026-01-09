package dev.kiddo.headguitest;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HeadDebugCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("headdebug")
                    .executes(HeadDebugCommand::run));
        });
    }

    private static int run(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;

        // 1. Get Item in Hand
        ItemStack stack = client.player.getMainHandStack();

        if (stack.isEmpty()) {
            client.player.sendMessage(Text.literal("Hold a head in your hand first!").formatted(Formatting.RED), false);
            return 0;
        }

        // 2. Convert the Item + Components to NBT String
        // We use the registry from the command context to ensure 1.21 compatibility
        RegistryWrapper.WrapperLookup registryLookup = context.getSource().getRegistryManager();

        // This converts the 1.21 Data Components back into an Old-School NBT Tag
        // so we can see exactly what the parser needs to read.
        ComponentMap nbtTag = stack.getComponents();
        String nbtString = nbtTag.toString();

        // 3. Print to Console (Best for copying large data)
        System.out.println("================= HEAD DEBUG START =================");
        System.out.println(nbtString);
        System.out.println("================= HEAD DEBUG END   =================");

        // 4. Print to Chat (Click to Copy)
        client.player.sendMessage(Text.literal("Head data dumped to console! "), false);

        return 1;
    }
}