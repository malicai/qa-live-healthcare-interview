package com.leansofx.qaserviceuser;

import com.leansofx.qaserviceuser.model.Patient;
import com.leansofx.qaserviceuser.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PatientApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
    }

    @Test
    void shouldRegisterPatientWhenRequestIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "patient-li-si",
                                  "password": "123456",
                                  "name": "李四",
                                  "birthday": "1992-04-18",
                                  "phone": "13800000000",
                                  "gender": "男"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patient.id").isNotEmpty())
                .andExpect(jsonPath("$.patient.username").value("patient-li-si"))
                .andExpect(jsonPath("$.patient.birthday").value("1992-04-18"))
                .andExpect(jsonPath("$.patient.password").doesNotExist())
                .andExpect(jsonPath("$.patient.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturnConflictWhenUsernameExists() throws Exception {
        savePatient();

        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("patient-zhao-ming", "123456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void shouldReturnValidationErrorWhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("patient-li-si", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldLoginWhenCredentialsAreCorrect() throws Exception {
        savePatient();

        mockMvc.perform(post("/api/v1/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "patient-zhao-ming", "password": "123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.id").value("patient001"))
                .andExpect(jsonPath("$.patient.name").value("赵明"))
                .andExpect(jsonPath("$.patient.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        savePatient();

        mockMvc.perform(post("/api/v1/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "patient-zhao-ming", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnUnauthorizedWhenUsernameDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/patients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "patient-nobody", "password": "123456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private void savePatient() {
        Patient patient = new Patient();
        patient.setId("patient001");
        patient.setUsername("patient-zhao-ming");
        patient.setPasswordHash(passwordEncoder.encode("123456"));
        patient.setName("赵明");
        patient.setBirthday(LocalDate.of(1985, 3, 15));
        patient.setPhone("138****1234");
        patient.setGender("男");
        patientRepository.save(patient);
    }

    private String registerBody(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s",
                  "name": "测试患者",
                  "birthday": "1990-01-01"
                }
                """.formatted(username, password);
    }
}
