package dev.kiddo.headguitest.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.kiddo.headguitest.HeadData;
import dev.kiddo.headguitest.HeadFactory;
import dev.kiddo.headguitest.HeadGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HeadguitestClient implements ClientModInitializer {

    private static final String MOD_CONFIG_FOLDER = "InGameShopDir";
    private static final String CONFIG_FILE_NAME = "heads.json";
    private static final String SERVER_URL = "https://example.com/heads.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static List<HeadData> cachedHeadData = null;
    private static boolean isLoading = false;
    private static String errorMessage = null;

    private static List<ItemStack> pendingItems = null;

    @Override
    public void onInitializeClient() {
        loadConfigAsync();

        // 1. REGISTER TICK LISTENER
        // This runs every single game tick (approx 20 times a second, or faster on client)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check if we have items waiting to be opened
            if (pendingItems != null) {
                // Check if the Chat Screen has finally closed (screen is null)
                if (client.currentScreen == null) {
                    // Open our GUI
                    HeadGui.openItems(pendingItems);

                    // Reset pending items so we don't open it infinitely
                    pendingItems = null;
                }
            }
        });

        // 2. COMMAND REGISTRATION
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("headgui")
                    .executes(context -> {
                        if (isLoading) {
                            context.getSource().sendFeedback(Text.literal("Still syncing config...").formatted(Formatting.YELLOW));
                            return 0;
                        }
                        if (errorMessage != null) {
                            context.getSource().sendError(Text.literal("Error: " + errorMessage));
                            return 0;
                        }
                        if (cachedHeadData == null || cachedHeadData.isEmpty()) {
                            context.getSource().sendError(Text.literal("No heads found."));
                            return 0;
                        }

                        try {
                            // 1. Prepare items
                            List<HeadData> randomizedData = new ArrayList<>(cachedHeadData);
                            Collections.shuffle(randomizedData);

                            List<ItemStack> items = new ArrayList<>();
                            for (HeadData data : randomizedData) {
                                items.add(HeadFactory.create(data));
                            }

                            // 2. Queue them up
                            pendingItems = items;

                            // 3. Force close the chat screen immediately
                            // This triggers the Tick Event logic to pick it up on the very next frame
                            context.getSource().getClient().setScreen(null);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return 1;
                    }));
        });
    }

    private void loadConfigAsync() {
        isLoading = true;
        errorMessage = null;
        CompletableFuture.runAsync(() -> {
            try {
                Path rootConfigDir = FabricLoader.getInstance().getConfigDir();
                Path modConfigDir = rootConfigDir.resolve(MOD_CONFIG_FOLDER);
                Files.createDirectories(modConfigDir);
                Path configFile = modConfigDir.resolve(CONFIG_FILE_NAME);

                if (!Files.exists(configFile)) {
                    downloadConfig(configFile);
                }

                try (Reader reader = Files.newBufferedReader(configFile)) {
                    HeadData[] parsedArray = GSON.fromJson(reader, HeadData[].class);
                    if (parsedArray != null) {
                        cachedHeadData = Arrays.asList(parsedArray);
                    } else {
                        cachedHeadData = new ArrayList<>();
                    }
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
            } finally {
                isLoading = false;
            }
        });
    }

    private boolean downloadConfig(Path destination) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Files.writeString(destination, response.body());
                return true;
            }
        } catch (Exception e) { /* handle */ }
        return false;
    }
}