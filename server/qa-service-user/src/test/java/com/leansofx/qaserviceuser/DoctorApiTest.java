package com.leansofx.qaserviceuser;

import com.leansofx.qaserviceuser.model.Doctor;
import com.leansofx.qaserviceuser.model.Specialty;
import com.leansofx.qaserviceuser.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DoctorApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
        doctorRepository.save(buildDoctor());
    }

    @Test
    void shouldReturnActiveDoctorsWhenFilterProvided() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].id").value("doc001"))
                .andExpect(jsonPath("$.items[0].isActive").value(true))
                .andExpect(jsonPath("$.items[0].specialties[0]").value("高血压"))
                .andExpect(jsonPath("$.items[0].specialtiesEn[1]").value("Coronary Heart Disease"))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist());
    }

    @Test
    void shouldReturnDoctorWhenGetById() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/doc001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doc001"))
                .andExpect(jsonPath("$.nameEn").value("Dr. Zhang Wei"))
                .andExpect(jsonPath("$.departmentEn").value("Cardiology"));
    }

    @Test
    void shouldReturnDoctorWhenGetByUsername() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/username/dr-zhang-wei"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dr-zhang-wei"))
                .andExpect(jsonPath("$.title").value("主任医师"));
    }

    @Test
    void shouldReturnDoctorWhenLoginSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/doctors/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dr-zhang-wei\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctor.id").value("doc001"))
                .andExpect(jsonPath("$.doctor.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        mockMvc.perform(post("/api/v1/doctors/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dr-zhang-wei\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnNotFoundWhenDoctorDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/doc999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnValidationErrorWhenPageIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/doctors").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldMatchSeededPasswordHash() {
        String seedHash = "$2b$10$7mqxpuBApPozlWYfXhenueWCadMtfSTwJ2MTWipraA8sRmT1pbqSu";
        assertTrue(passwordEncoder.matches("123456", seedHash));
    }

    private Doctor buildDoctor() {
        Doctor doctor = new Doctor();
        doctor.setId("doc001");
        doctor.setUsername("dr-zhang-wei");
        doctor.setPasswordHash(passwordEncoder.encode("123456"));
        doctor.setName("张伟医生");
        doctor.setNameEn("Dr. Zhang Wei");
        doctor.setTitle("主任医师");
        doctor.setTitleEn("Chief Physician");
        doctor.setDepartment("心内科");
        doctor.setDepartmentEn("Cardiology");
        doctor.setAvatar("https://example.com/avatar.jpg");
        doctor.setExperience("15年临床经验");
        doctor.setExperienceEn("15 years of clinical experience");
        doctor.setActive(true);
        doctor.setSpecialties(List.of(
                new Specialty("高血压", "Hypertension", 1),
                new Specialty("冠心病", "Coronary Heart Disease", 2)));
        return doctor;
    }
}
