package com.example.app.controller;

import com.example.app.service.DuplicateUserException;
import com.example.app.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ユーザー関連のリクエストを受け付けるクラス。
 *
 * <p>このサンプルは Spring Boot として起動しないため、アノテーションは「業務コードと同じ見た目」にするためのものです。
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** ユーザーを登録し、結果を文字列で返す（OK / DUPLICATE / ERROR）。 */
    @PostMapping
    public String register(@RequestParam long userId, @RequestParam String name) {
        try {
            userService.register(userId, name);
            return "OK";
        } catch (DuplicateUserException e) {
            // 業務上の想定内エラー。Service 側でログを出しているので、ここでは結果を返すだけ
            return "DUPLICATE";
        } catch (Exception e) {
            // NOTE(Step 2-1): e.getMessage() だけを出しているので、原因（スタックトレース）が残らない
            log.error("ユーザー登録でエラーが発生しました: " + e.getMessage());
            // NOTE(Step 2-1): printStackTrace の直書き。ログの設定を通らず、標準エラー出力に直接出る
            e.printStackTrace();
            return "ERROR";
        }
    }
}
