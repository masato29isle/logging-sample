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
        // {} プレースホルダで値を渡す。key=value の形にすると検索しやすい
        log.info("ユーザー登録開始 userId={} name={}", userId, name);

        if (userRepository.findById(userId).isPresent()) {
            // 続行できるが異常な状況なので WARN
            log.warn("ユーザーが重複しています userId={}", userId);
            throw new DuplicateUserException(userId);
        }

        try {
            userRepository.save(userId, name);
        } catch (RepositoryException e) {
            // Repository の事情（DB）を呼び出し元に見せないよう、Service の例外に包み直す
            throw new UserRegistrationException("ユーザー登録処理でエラーが発生しました", e);
        }

        // 業務上の節目なので INFO
        log.info("ユーザー登録完了 userId={}", userId);
    }
}
