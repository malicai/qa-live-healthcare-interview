package com.leansofx.qaserviceuser.service;

import com.leansofx.qaserviceuser.dto.PatientRegisterRequest;
import com.leansofx.qaserviceuser.dto.PatientResponse;
import com.leansofx.qaserviceuser.exception.ApiException;
import com.leansofx.qaserviceuser.model.Patient;
import com.leansofx.qaserviceuser.repository.PatientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientRepository patientRepository, PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PatientResponse register(PatientRegisterRequest request) {
        String username = request.username().trim();

        if (patientRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "用户名已存在");
        }

        Patient patient = new Patient();
        patient.setId(generateId());
        patient.setUsername(username);
        patient.setPasswordHash(passwordEncoder.encode(request.password()));
        patient.setName(request.name().trim());
        patient.setBirthday(request.birthday());
        patient.setPhone(request.phone() == null ? "" : request.phone().trim());
        patient.setGender(request.gender() == null ? "" : request.gender().trim());

        try {
            patientRepository.save(patient);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "用户名已存在");
        }

        return PatientResponse.from(patient);
    }

    public PatientResponse login(String username, String password) {
        Patient patient = patientRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "用户名或密码错误"));

        if (!passwordEncoder.matches(password, patient.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "用户名或密码错误");
        }

        return PatientResponse.from(patient);
    }

    private String generateId() {
        return "patient" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
