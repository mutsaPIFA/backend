package com.mutsapifa.mcmmuse.profile.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 계약 §5-1 */
public record NicknameUpdateRequest(
    @NotBlank(message = "닉네임은 1~20자여야 합니다") @Size(max = 20, message = "닉네임은 1~20자여야 합니다")
        String nickname) {}
