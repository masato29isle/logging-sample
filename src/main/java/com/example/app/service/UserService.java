package com.example.app.service;

import com.example.app.repository.RepositoryException;
import com.example.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** ユーザーに関する業務ロジックを担うクラス。 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ユーザーを登録する。
     *
     * @throws DuplicateUserException 同じ userId が登録済みのとき
     * @throws UserRegistrationException データアクセスに失敗したとき
     */
    public void register(long userId, String name) {
        // NOTE(Step 2-2): 文字列連結でログのメッセージを組み立てている
        log.info("ユーザー登録開始 " + userId + " " + name);

        if (userRepository.findById(userId).isPresent()) {
            // NOTE(Step 2-3): 処理は続行できるが異常な状況。INFO でよいか？
            log.info("ユーザーが重複しています userId={}", userId);
            throw new DuplicateUserException(userId);
        }

        try {
            userRepository.save(userId, name);
        } catch (RepositoryException e) {
            // Repository の事情（DB）を呼び出し元に見せないよう、Service の例外に包み直す
            throw new UserRegistrationException("ユーザー登録処理でエラーが発生しました", e);
        }

        // NOTE(Step 2-3): 正常に終わったのに ERROR で出している
        log.error("ユーザー登録完了 userId={}", userId);
    }
}
