package com.stella.stellaathome;

import com.google.gson.annotations.SerializedName;

public class TokenInfo {
    @SerializedName("access_token")
    String AccessToken;
    @SerializedName("token_type")
    String TokenType;
}
