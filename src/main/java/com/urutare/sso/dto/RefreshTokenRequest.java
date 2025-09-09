package com.urutare.sso.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
    private String clientId;
    private String clientSecret;
}
