package com.clarix.web;

import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

final class ValidationFeedback {
    private ValidationFeedback() {}

    static String redirect(String path, BindingResult errors, RedirectAttributes redirect) {
        // redirect 후에도 에러 메시지를 한 번 표시하기 위해 flash attribute를 사용합니다.
        redirect.addFlashAttribute("formError", firstError(errors));
        return "redirect:" + path;
    }

    static String firstError(BindingResult errors) {
        var field = errors.getFieldError();
        if (field != null && field.getDefaultMessage() != null) {
            String message = field.getDefaultMessage();
            return message.startsWith("Failed to convert") ? "입력값을 확인하세요" : message;
        }
        var global = errors.getGlobalError();
        return global != null && global.getDefaultMessage() != null
            ? global.getDefaultMessage()
            : "입력값을 확인하세요";
    }
}
