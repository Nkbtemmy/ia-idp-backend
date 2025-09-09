package com.urutare.sso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegisterRequest {
  @NotBlank @Email
  private String email;
  @NotBlank
  private String password;
  private String redirect;

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getRedirect() { return redirect; }
  public void setRedirect(String redirect) { this.redirect = redirect; }
}
