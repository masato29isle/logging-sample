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
            // NOTE(Step 2-4): 誰を保存したのか、この行だけでは分からない
            log.info("ユーザーを保存しました");
        } catch (SQLException e) {
            // NOTE(Step 2-4): 例外メッセージにも誰の保存に失敗したのかが入っていない
            throw new RepositoryException("ユーザーの保存に失敗しました", e);
        }
    }
}
