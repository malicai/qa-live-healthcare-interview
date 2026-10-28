package com.leansofx.qaserviceuser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[a-zA-Z0-9_-]{4,32}", message = "用户名需为 4-32 位字母、数字、下划线或连字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名长度不能超过 64 位")
        String name,

        @NotNull(message = "生日不能为空")
        LocalDate birthday,

        @Size(max = 32, message = "手机号长度不能超过 32 位")
        String phone,

        @Size(max = 16, message = "性别长度不能超过 16 位")
        String gender) {
}
