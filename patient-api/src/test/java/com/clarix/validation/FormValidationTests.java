package com.clarix.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import com.clarix.domain.Emotion;
import com.clarix.domain.MealKind;
import com.clarix.domain.MedStatus;
import com.clarix.domain.Role;
import com.clarix.dto.AssessmentForm;
import com.clarix.dto.MedicationToggleForm;
import com.clarix.dto.MoodEntryForm;
import com.clarix.dto.PrescriptionForm;
import com.clarix.dto.SignupForm;
import com.clarix.dto.StaffInviteForm;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class FormValidationTests {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void signupFormRejectsInvalidAccountFields() {
        SignupForm form = new SignupForm();
        form.setEmail("bad-email");
        form.setPassword("123");
        form.setName("");
        form.setRole("admin");

        Set<String> messages = messages(validator.validate(form));

        assertThat(messages).contains(
            "올바른 이메일 형식이 아닙니다",
            "비밀번호는 6자 이상이어야 합니다",
            "이름을 입력하세요",
            "가입 유형이 올바르지 않습니다"
        );
    }

    @Test
    void assessmentFormRequiresAllPhq9AnswersInRange() {
        AssessmentForm form = new AssessmentForm();
        form.setQ1(0);
        form.setQ2(1);
        form.setQ3(2);
        form.setQ4(3);
        form.setQ5(4);

        Set<String> invalidFields = fields(validator.validate(form));

        assertThat(invalidFields).contains("q5", "q6", "q7", "q8", "q9");
    }

    @Test
    void prescriptionFormRequiresNameScheduleAndReasonableDays() {
        PrescriptionForm form = new PrescriptionForm();
        form.setMedicationName(" ");
        form.setDaysSupply(181);

        Set<String> messages = messages(validator.validate(form));

        assertThat(messages).contains(
            "약 이름을 입력하세요",
            "복약 시간을 하나 이상 선택하세요",
            "처방 일수는 180일 이하여야 합니다"
        );
    }

    @Test
    void enumBackedFormsBindValidEnumValues() {
        MoodEntryForm mood = new MoodEntryForm();
        DataBinder moodBinder = new DataBinder(mood);
        moodBinder.bind(new MutablePropertyValues(Map.of("emotion", "CALM")));

        MedicationToggleForm medication = new MedicationToggleForm();
        DataBinder medicationBinder = new DataBinder(medication);
        medicationBinder.bind(new MutablePropertyValues(Map.of("status", "TAKEN")));

        assertThat(moodBinder.getBindingResult().hasErrors()).isFalse();
        assertThat(mood.getEmotion()).isEqualTo(Emotion.CALM);
        assertThat(medicationBinder.getBindingResult().hasErrors()).isFalse();
        assertThat(medication.getStatus()).isEqualTo(MedStatus.TAKEN);
    }

    @Test
    void enumBackedFormsRejectInvalidEnumValues() {
        MoodEntryForm mood = new MoodEntryForm();
        DataBinder moodBinder = new DataBinder(mood);
        moodBinder.bind(new MutablePropertyValues(Map.of("emotion", "PANIC")));

        assertThat(moodBinder.getBindingResult().hasFieldErrors("emotion")).isTrue();
    }

    @Test
    void staffInviteOnlyAllowsStaffRoles() {
        StaffInviteForm form = new StaffInviteForm();
        form.setEmail("staff@example.com");
        form.setRole(Role.ADMIN);

        assertThat(messages(validator.validate(form))).contains("초대 가능한 역할이 아닙니다");
    }

    @Test
    void mealKindBindsToEnumValue() {
        com.clarix.dto.MealLogForm form = new com.clarix.dto.MealLogForm();
        DataBinder binder = new DataBinder(form);
        binder.bind(new MutablePropertyValues(Map.of("kind", "LUNCH")));

        assertThat(binder.getBindingResult().hasErrors()).isFalse();
        assertThat(form.getKind()).isEqualTo(MealKind.LUNCH);
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toSet());
    }

    private static Set<String> fields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
