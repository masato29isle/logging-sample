package com.example.app.repository;

import com.example.fakedb.FakeDatabase;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/** ユーザー情報の永続化を担うクラス。 */
@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final FakeDatabase db;

    public UserRepository(FakeDatabase db) {
        this.db = db;
    }

    public Optional<String> findById(long userId) {
        return db.find(userId);
    }

    public void save(long userId, String name) {
        try {
            db.insert(userId, name);
            log.info("ユーザーを保存しました userId={}", userId);
        } catch (SQLException e) {
            // 例外メッセージにも ID を入れる。ログに出るのは受け止める側（Controller）なので、ここでは投げ直すだけ
            throw new RepositoryException("ユーザーの保存に失敗しました userId=" + userId, e);
        }
    }
}
