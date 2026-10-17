package com.example.app;

import com.example.app.controller.UserController;
import com.example.app.repository.UserRepository;
import com.example.app.service.UserService;
import com.example.fakedb.FakeDatabase;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * ハンズオン用の起動クラス。Web フレームワークの代わりに、リクエストを受け付けて Controller に渡す役。
 *
 * <pre>
 *   mvn -q compile exec:java                        4 件の登録を順番に処理する（本編）
 *   mvn -q compile exec:java -Dexec.args=concurrent 4 件の登録を 2 スレッドで同時に処理する（発展課題1）
 * </pre>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        // Spring Boot として起動しないので、DI の代わりに手で組み立てる
        UserController controller = new UserController(new UserService(new UserRepository(new FakeDatabase())));

        if (args.length > 0 && args[0].equals("concurrent")) {
            runConcurrently(controller);
        } else {
            runSequentially(controller);
        }
    }

    /** 本編: 4 件の登録を順番に処理する。2 件目が DB エラー、3 件目が重複で失敗する。 */
    private static void runSequentially(UserController controller) {
        handleRequest(controller, 101, "alice");
        handleRequest(controller, 102, "bob");
        handleRequest(controller, 101, "alice");
        handleRequest(controller, 103, "carol");
    }

    /** 発展課題1: 4 件の登録を 2 スレッドで同時に処理する。ログが混ざる。 */
    private static void runConcurrently(UserController controller) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> handleRequest(controller, 201, "dave"));
        pool.submit(() -> handleRequest(controller, 102, "bob"));
        pool.submit(() -> handleRequest(controller, 202, "erin"));
        pool.submit(() -> handleRequest(controller, 203, "frank"));
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
    }

    /**
     * 1 件のリクエストを受け付けて Controller に渡す。リクエストごとに requestId を割り当てる。
     *
     * <p>発展課題1: requestId を MDC に入れると、このスレッドで出す全ログ行に requestId が付く
     * （出力位置は logback.xml の {@code %X{requestId}}）。スレッドは使い回されるので、終わったら必ず消す。
     */
    private static void handleRequest(UserController controller, long userId, String name) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        try {
            log.info("リクエスト受付 userId={}", userId);
            String result = controller.register(userId, name);
            log.info("リクエスト完了 result={}", result);
        } finally {
            MDC.clear();
        }
    }
}
