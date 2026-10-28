package com.leansofx.qaserviceuser.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leansofx.qaserviceuser.model.Doctor;
import com.leansofx.qaserviceuser.model.Specialty;

import java.util.List;

public record DoctorResponse(
        String id,
        String username,
        String name,
        String nameEn,
        String title,
        String titleEn,
        String department,
        String departmentEn,
        String avatar,
        String experience,
        String experienceEn,
        List<String> specialties,
        List<String> specialtiesEn,
        @JsonProperty("isActive") boolean isActive) {

    public static DoctorResponse from(Doctor doctor) {
        List<String> specialties = doctor.getSpecialties().stream()
                .map(Specialty::getName)
                .toList();
        List<String> specialtiesEn = doctor.getSpecialties().stream()
                .map(Specialty::getNameEn)
                .toList();

        return new DoctorResponse(
                doctor.getId(),
                doctor.getUsername(),
                doctor.getName(),
                doctor.getNameEn(),
                doctor.getTitle(),
                doctor.getTitleEn(),
                doctor.getDepartment(),
                doctor.getDepartmentEn(),
                doctor.getAvatar(),
                doctor.getExperience(),
                doctor.getExperienceEn(),
                specialties,
                specialtiesEn,
                doctor.isActive());
    }
}
