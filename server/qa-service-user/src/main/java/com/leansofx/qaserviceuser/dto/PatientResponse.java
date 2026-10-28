package com.leansofx.qaserviceuser.dto;

import com.leansofx.qaserviceuser.model.Patient;

import java.time.LocalDate;

public record PatientResponse(
        String id,
        String username,
        String name,
        LocalDate birthday,
        String phone,
        String gender) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getUsername(),
                patient.getName(),
                patient.getBirthday(),
                patient.getPhone(),
                patient.getGender());
    }
}
