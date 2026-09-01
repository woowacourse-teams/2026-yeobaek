package yeobaek.backend.preregistration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.preregistration.dto.PreRegistrationCreateRequest;
import yeobaek.backend.preregistration.service.PreRegistrationService;

@Tag(name = "사전신청")
@RestController
@RequiredArgsConstructor
public class PreRegistrationController {

    private final PreRegistrationService preRegistrationService;

    @Operation(summary = "사전신청", description = "이메일을 등록하여 여백 출시 안내를 신청한다. 회원 헤더는 필요하지 않다.")
    @PostMapping("/api/pre-registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody PreRegistrationCreateRequest request) {
        preRegistrationService.create(request.email());
    }
}
