package com.leansofx.qaserviceuser.service;

import com.leansofx.qaserviceuser.dto.DoctorResponse;
import com.leansofx.qaserviceuser.dto.PageResponse;
import com.leansofx.qaserviceuser.exception.ApiException;
import com.leansofx.qaserviceuser.model.Doctor;
import com.leansofx.qaserviceuser.repository.DoctorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository, PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<DoctorResponse> list(int page, int size, Boolean isActive) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Doctor> result = isActive == null
                ? doctorRepository.findAll(pageable)
                : doctorRepository.findByActive(isActive, pageable);

        List<DoctorResponse> items = result.getContent().stream()
                .map(DoctorResponse::from)
                .toList();
        return new PageResponse<>(items, result.getTotalElements(), page, size);
    }

    public DoctorResponse getById(String id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "医生不存在"));
        return DoctorResponse.from(doctor);
    }

    public DoctorResponse getByUsername(String username) {
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "医生不存在"));
        return DoctorResponse.from(doctor);
    }

    public DoctorResponse login(String username, String password) {
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "用户名或密码错误"));

        if (!passwordEncoder.matches(password, doctor.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "用户名或密码错误");
        }

        return DoctorResponse.from(doctor);
    }
}
