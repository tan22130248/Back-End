package com.example.Back_End.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Pattern(regexp = ".*@gmail\\..*", message = "Email phải có đuôi @gmail.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 7, message = "Mật khẩu phải từ 7 ký tự trở lên")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "Mật khẩu phải có ít nhất 1 chữ hoa và 1 số")
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
