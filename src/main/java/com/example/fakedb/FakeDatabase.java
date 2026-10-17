package com.example.fakedb;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JDBC ドライバと DB の代わりをするインメモリの擬似 DB。
 *
 * <p>ハンズオンでは中身を読む必要はありません。「自分のコードではないライブラリ」の役割で、
 * 特定のユーザーの INSERT で接続エラー（{@link SQLException}）を擬似的に起こします。
 */
public class FakeDatabase {

    /** この userId を INSERT しようとすると接続エラーになる。 */
    private static final long POISON_USER_ID = 102;

    private final Map<Long, String> rows = new ConcurrentHashMap<>();

    public Optional<String> find(long userId) {
        simulateLatency();
        return Optional.ofNullable(rows.get(userId));
    }

    public void insert(long userId, String name) throws SQLException {
        simulateLatency();
        if (userId == POISON_USER_ID) {
            ConnectException cause = new ConnectException("Connection refused: db.example.internal:5432 (擬似)");
            throw new SQLException("DB への接続に失敗しました", cause);
        }
        rows.put(userId, name);
    }

    /** DB アクセスにかかる時間を擬似的に再現する（発展課題1でログが混ざる様子を見せるため）。 */
    private static void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(20, 80));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
