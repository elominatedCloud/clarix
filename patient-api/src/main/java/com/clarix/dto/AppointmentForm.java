package com.clarix.dto;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

@Data
public class AppointmentForm {
    private boolean booked;

    @FutureOrPresent(message = "진료 일시는 현재 이후로 입력하세요")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime appointmentAt;

    @AssertTrue(message = "진료 일시를 입력하세요")
    public boolean isAppointmentAtPresentWhenBooked() {
        return !booked || appointmentAt != null;
    }
}
