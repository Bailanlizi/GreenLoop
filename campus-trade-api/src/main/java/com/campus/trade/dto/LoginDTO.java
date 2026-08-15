package com.campus.trade.dto;

import javax.validation.constraints.NotBlank;

public class LoginDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    // --- Getters ---
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // --- Setters ---
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
