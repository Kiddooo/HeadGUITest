package dev.kiddo.headguitest;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HeadFactory {

    public static ItemStack create(HeadData data) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);

        // 1. Set Name
        if (data.name_json() != null) {
            Text text = parseJsonText(data.name_json());
            stack.set(DataComponentTypes.CUSTOM_NAME, text);
        }

        // 2. Set Texture
        if (data.texture() != null && !data.texture().isEmpty()) {
            Multimap<String, Property> propertiesMap = HashMultimap.create();
            propertiesMap.put("textures", new Property("textures", data.texture()));
            PropertyMap properties = new PropertyMap(propertiesMap);
            GameProfile profile = new GameProfile(UUID.randomUUID(), "CustomHead", properties);
            ProfileComponent component = ProfileComponent.ofStatic(profile);
            stack.set(DataComponentTypes.PROFILE, component);
        }

        // 3. Set Lore (Shop Info)
        List<Text> loreLines = new ArrayList<>();

        if (data.shopName() != null && !data.shopName().isEmpty()) {
            loreLines.add(Text.literal("Shop Name: " + data.shopName())
                    .formatted(Formatting.GOLD)
                    .styled(s -> s.withItalic(false))); // FIX: Disable Italics
        }

        if (data.owner() != null && !data.owner().isEmpty()) {
            loreLines.add(Text.literal("Owner(s): " + data.owner())
                    .formatted(Formatting.GRAY)
                    .styled(s -> s.withItalic(false))); // FIX: Disable Italics
        }

        if (data.coords() != null && !data.coords().isEmpty()) {
            loreLines.add(Text.literal("Shop Coords: " + data.coords())
                    .formatted(Formatting.AQUA)
                    .styled(s -> s.withItalic(false))); // FIX: Disable Italics
        }

        if (data.note() != null && !data.note().isEmpty()) {
            loreLines.add(Text.literal("Note: " + data.note())
                    .formatted(Formatting.GREEN)
                    .styled(s -> s.withItalic(false))); // FIX: Disable Italics
        }

        if (!loreLines.isEmpty()) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        }


        return stack;
    }

    private static Text parseJsonText(String json) {
        try {
            ClientPlayNetworkHandler netHandler = MinecraftClient.getInstance().getNetworkHandler();
            if (netHandler != null) {
                RegistryWrapper.WrapperLookup registry = netHandler.getRegistryManager();
                var jsonElement = JsonParser.parseString(json);
                var ops = RegistryOps.of(JsonOps.INSTANCE, registry);

                return TextCodecs.CODEC
                        .decode(ops, jsonElement)
                        .result()
                        .map(Pair::getFirst)
                        .orElse(Text.literal(json).styled(s -> s.withItalic(false)));
            }
        } catch (Exception e) {
            // Fall through
        }
        return Text.literal(json).styled(s -> s.withItalic(false));
    }
}