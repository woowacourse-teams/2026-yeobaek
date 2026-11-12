package yeobaek.backend.admin.controller;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import org.springframework.stereotype.Controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 관리자 페이지(HTML). 페이지 접근 자체는 토큰이 불필요하며,
 * 페이지 안에서 호출하는 /api/admin/** 요청에 토큰을 실어 보낸다 (API.md 6장).
 */
@Controller
@Slf4j
public class AdminPageController {

    @GetMapping("/admin")
    public String adminPage() {
        log.atInfo().addKeyValue(OPERATION, "admin.page.adminPage")
                .log("관리자 도서 페이지 처리를 시작합니다.");
        log.atInfo().addKeyValue(OPERATION, "admin.page.adminPage").addKeyValue(RESULT, SUCCESS)
                .log("관리자 도서 페이지 처리를 완료했습니다.");
        return "admin";
    }

    @GetMapping("/admin/dashboard")
    public String dashboardPage() {
        log.atInfo().addKeyValue(OPERATION, "admin.page.dashboardPage")
                .log("관리자 대시보드 페이지 처리를 시작합니다.");
        log.atInfo().addKeyValue(OPERATION, "admin.page.dashboardPage").addKeyValue(RESULT, SUCCESS)
                .log("관리자 대시보드 페이지 처리를 완료했습니다.");
        return "admin-dashboard";
    }
}
