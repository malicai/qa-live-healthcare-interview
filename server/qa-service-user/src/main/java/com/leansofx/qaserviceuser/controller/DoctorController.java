package com.leansofx.qaserviceuser.controller;

import com.leansofx.qaserviceuser.dto.DoctorLoginRequest;
import com.leansofx.qaserviceuser.dto.DoctorResponse;
import com.leansofx.qaserviceuser.dto.LoginResponse;
import com.leansofx.qaserviceuser.dto.PageResponse;
import com.leansofx.qaserviceuser.service.DoctorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public PageResponse<DoctorResponse> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于等于 1") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size 必须大于等于 1")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) Boolean isActive) {
        return doctorService.list(page, size, isActive);
    }

    @GetMapping("/{id}")
    public DoctorResponse getById(@PathVariable String id) {
        return doctorService.getById(id);
    }

    @GetMapping("/username/{username}")
    public DoctorResponse getByUsername(@PathVariable String username) {
        return doctorService.getByUsername(username);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody DoctorLoginRequest request) {
        return new LoginResponse(doctorService.login(request.username(), request.password()));
    }
}
