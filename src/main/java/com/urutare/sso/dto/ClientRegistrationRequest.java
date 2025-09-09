package com.urutare.sso.dto;

import lombok.Data;

@Data
public class ClientRegistrationRequest {
    private String name;
    private String description;
    private String redirectUri;
}
