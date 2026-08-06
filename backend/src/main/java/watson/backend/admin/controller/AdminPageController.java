package watson.backend.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 관리자 페이지(HTML). 페이지 접근 자체는 토큰이 불필요하며,
 * 페이지 안에서 호출하는 /api/admin/** 요청에 토큰을 실어 보낸다 (API.md 6장).
 */
@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }
}
