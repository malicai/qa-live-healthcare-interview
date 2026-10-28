package com.leansofx.qaserviceuser.repository;

import com.leansofx.qaserviceuser.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, String> {

    Optional<Patient> findByUsername(String username);

    boolean existsByUsername(String username);
}
