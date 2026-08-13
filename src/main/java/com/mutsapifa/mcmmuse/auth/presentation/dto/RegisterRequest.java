package com.mutsapifa.mcmmuse.auth.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "이메일은 필수입니다") @Email(message = "이메일 형식이 아닙니다") String email,
    @NotBlank(message = "비밀번호는 필수입니다") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String password,
    @NotBlank(message = "닉네임은 필수입니다") String nickname) {}
