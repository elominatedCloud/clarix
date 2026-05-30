package com.clarix.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.clarix.domain.CareRequest;
import com.clarix.domain.CareRequestStatus;
import com.clarix.domain.User;
import com.clarix.repo.CareRequestRepository;
import com.clarix.repo.UserRepository;

@Service
public class CareRequestService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Seoul");

    private final CareRequestRepository requests;
    private final UserRepository users;

    public CareRequestService(CareRequestRepository requests, UserRepository users) {
        this.requests = requests;
        this.users = users;
    }

    public List<CareRequest> pendingForHospital(UUID hospitalId) {
        return requests.findByHospitalIdAndStatusOrderByCreatedAtAsc(hospitalId, CareRequestStatus.PENDING);
    }

    public List<CareRequest> recentForPatient(UUID patientId) {
        return requests.findTop5ByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public Optional<CareRequest> pendingForPatient(UUID patientId) {
        return requests.findFirstByPatientIdAndStatusOrderByCreatedAtDesc(patientId, CareRequestStatus.PENDING);
    }

    @Transactional
    public CareRequest create(User patient, String reason) {
        if (patient.getHospital() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "병원 연결 후 진료 요청을 보낼 수 있습니다");
        }
        CareRequest request = new CareRequest();
        request.setPatient(patient);
        request.setHospital(patient.getHospital());
        request.setReason(reason.trim());
        return requests.save(request);
    }

    @Transactional
    public void cancel(UUID patientId, UUID requestId) {
        CareRequest request = requirePatientRequest(patientId, requestId);
        if (!request.isPending()) return;
        request.setStatus(CareRequestStatus.CANCELED);
        request.setHandledAt(OffsetDateTime.now());
        requests.save(request);
    }

    @Transactional
    public void markScheduled(UUID staffHospitalId, UUID requestId, LocalDateTime appointmentAt) {
        CareRequest request = requests.findById(requestId).orElseThrow();
        if (!request.getHospital().getId().equals(staffHospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 병원의 진료 요청입니다");
        }
        if (appointmentAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약 시간을 입력하세요");
        }
        request.setStatus(CareRequestStatus.SCHEDULED);
        request.setHandledAt(OffsetDateTime.now());
        requests.save(request);
        User patient = request.getPatient();
        patient.setBookedAt(appointmentAt.atZone(CLINIC_ZONE).toOffsetDateTime());
        users.save(patient);
    }

    private CareRequest requirePatientRequest(UUID patientId, UUID requestId) {
        CareRequest request = requests.findById(requestId).orElseThrow();
        if (!request.getPatient().getId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no permission");
        }
        return request;
    }
}
