package com.urutare.sso.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenRequest {
    private String subject;
    private String audience;
    private String issuer;
    private String clientId;
    private String clientSecret;
}
