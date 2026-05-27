package com.clarix.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarix.domain.Hospital;
import com.clarix.domain.Role;
import com.clarix.domain.User;
import com.clarix.repo.HospitalRepository;
import com.clarix.repo.UserRepository;

@Service
public class AdminService {

    private final HospitalRepository hospitals;
    private final UserRepository users;

    public AdminService(HospitalRepository hospitals, UserRepository users) {
        this.hospitals = hospitals;
        this.users = users;
    }

    public record HospitalRow(
        UUID id, String name, String specialty, String address,
        boolean partnered, int doctorCount, int staffCount, int patientCount
    ) {}

    public List<HospitalRow> hospitalRows() {
        // 병원 목록 화면은 병원 자체 정보 + 소속 사용자 수를 같이 보여줘야 하므로 view 전용 row로 변환합니다.
        var all = hospitals.findAll();
        var allUsers = users.findAll();
        return all.stream().map(h -> {
            int doctors = (int) allUsers.stream()
                .filter(u -> matchesHospital(u, h.getId()) && u.getRole() == Role.DOCTOR).count();
            int staff = (int) allUsers.stream()
                .filter(u -> matchesHospital(u, h.getId())
                    && (u.getRole() == Role.NURSE || u.getRole() == Role.TECHNICIAN
                        || u.getRole() == Role.RECEPTIONIST)).count();
            int patients = (int) allUsers.stream()
                .filter(u -> matchesHospital(u, h.getId()) && u.getRole() == Role.PATIENT).count();
            return new HospitalRow(h.getId(), h.getName(), h.getSpecialty(), h.getAddress(),
                h.isPartnered(), doctors, staff, patients);
        }).toList();
    }

    private boolean matchesHospital(User u, UUID hospitalId) {
        return u.getHospital() != null && u.getHospital().getId().equals(hospitalId);
    }

    @Transactional
    public Hospital createHospital(String name, String address, String specialty, boolean partnered) {
        Hospital h = new Hospital();
        h.setName(name.trim());
        h.setAddress(address.trim());
        h.setSpecialty(specialty.trim());
        h.setPartnered(partnered);
        return hospitals.save(h);
    }

    @Transactional
    public void togglePartnered(UUID hospitalId, boolean partnered) {
        hospitals.findById(hospitalId).ifPresent(h -> {
            h.setPartnered(partnered);
            hospitals.save(h);
        });
    }

    public List<User> allUsers() {
        return users.findAll();
    }

    @Transactional
    public void updateUser(UUID userId, Role role, UUID hospitalId) {
        User u = users.findById(userId).orElseThrow();
        u.setRole(role);
        Hospital h = hospitalId == null ? null : hospitals.findById(hospitalId).orElse(null);
        u.setHospital(h);
        users.save(u);
    }

    public Optional<Hospital> findHospital(UUID id) {
        return hospitals.findById(id);
    }

    public List<Hospital> allHospitals() {
        return hospitals.findAll();
    }
}
