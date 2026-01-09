package dev.kiddo.headguitest;

import com.google.gson.annotations.SerializedName;

public record HeadData(
        @SerializedName("name_json") String name_json,
        @SerializedName("texture") String texture,
        @SerializedName("shop_name") String shopName, // Maps to JSON "shop_name"
        @SerializedName("owner") String owner,
        @SerializedName("coords") String coords,
        @SerializedName("note") String note
) {}