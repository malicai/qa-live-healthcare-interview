package com.leansofx.qaserviceuser.controller;

import com.leansofx.qaserviceuser.dto.PatientLoginRequest;
import com.leansofx.qaserviceuser.dto.PatientLoginResponse;
import com.leansofx.qaserviceuser.dto.PatientRegisterRequest;
import com.leansofx.qaserviceuser.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientLoginResponse register(@Valid @RequestBody PatientRegisterRequest request) {
        return new PatientLoginResponse(patientService.register(request));
    }

    @PostMapping("/login")
    public PatientLoginResponse login(@Valid @RequestBody PatientLoginRequest request) {
        return new PatientLoginResponse(patientService.login(request.username(), request.password()));
    }
}
