# ハンズオン: ログの読み方と出し方

所要時間の目安: 約 15 分（発展課題を除く）

## このハンズオンでやること

ユーザー登録を行う3層構造のサンプルアプリを動かし、わざと起こした障害のスタックトレースから **根本原因の行を見つけます**（読み方）。
次に、ログの出し方が悪い箇所を4つ直し、同じ障害が「誰が、どの処理で、なぜ失敗したか」まで **1行で追えるログ** にします（出し方）。

完了すると次の状態になります。

- 長いスタックトレースを見たとき、どの2箇所を見ればよいかが分かる
- `e.getMessage()` だけを出して原因が消える、という失敗をしなくなる
- ERROR / WARN / INFO / DEBUG を自分の言葉で使い分けられる

考え方は Java に限りません。C# や JavaScript でも、ログに「何を出すか」は同じです。

## 前提環境

作業を始める前に、以下を確認してください。

| 項目 | 要件 | 確認コマンド |
| --- | --- | --- |
| JDK | 17 以上 | `java -version` |
| ビルドツール | Maven 3.9.x | `mvn -v` |
| Git | 任意のバージョン | `git --version` |
| エディタ | IntelliJ IDEA / VS Code など任意 | - |
| ネットワーク | Maven Central に接続できること（依存のダウンロード用） | - |
| jq（任意） | 発展課題2で使う。なくてもよい | `jq --version` |

サンプルプロジェクトは GitHub の [masato29isle/logging-sample](https://github.com/masato29isle/logging-sample) にあります。
事前に clone して、次のコマンドでログが出ることを確認しておいてください。

```bash
git clone https://github.com/masato29isle/logging-sample.git
cd logging-sample
mvn -q compile exec:java
```

**期待される結果**: `INFO` や `ERROR` で始まるログが 20 行ほど出て、途中に長いスタックトレースが混ざる

`main` ブランチが開始状態、`solution` ブランチが完成状態です。途中で詰まったら、完成状態のファイルを取り出して見比べてください。

```bash
# 開始状態との差分を確認する
git diff main solution

# 特定のファイルだけ完成状態を取り出す（例: Controller）
git checkout solution -- src/main/java/com/example/app/controller/UserController.java
```

> このハンズオンでは API キーや外部サービスは使いません。DB も `FakeDatabase` というインメモリの擬似 DB です。

## サンプルの構成

`Main` が Web フレームワークの代わりにリクエストを4件受け付け、Controller → Service → Repository → 擬似 DB の順に処理が流れます。

```
Main（リクエスト受付）
 └─ UserController.register()      @RestController  結果（OK / DUPLICATE / ERROR）を返す
     └─ UserService.register()     @Service         重複チェックをして保存を依頼する
         └─ UserRepository.save()  @Repository      擬似 DB に保存する
             └─ FakeDatabase       JDBC ドライバと DB の代わり。userId=102 で接続エラーを起こす
```

4件のリクエストは次のとおりです。

| 順番 | userId | name | 結果 |
| --- | --- | --- | --- |
| 1 | 101 | alice | OK |
| 2 | 102 | bob | ERROR（DB 接続エラー） |
| 3 | 101 | alice | DUPLICATE（登録済み） |
| 4 | 103 | carol | OK |

Spring のアノテーションは「そのクラスの役割を示す目印」です。Spring Boot として起動しないので、DI や設定の知識は不要です。
ログは SLF4J の API（`Logger` / `LoggerFactory`）で出し、実装は Logback です。Spring Boot の既定と同じ組み合わせなので、現場のコードとほぼ同じ書き方になります。

## 手順

### Step 1: 動かして、スタックトレースを読む（5分）

まず開始状態のまま実行します。

```bash
mvn -q compile exec:java
```

2件目（userId=102）のところで、次のような出力が出ます（時刻と `requestId` は実行ごとに変わります）。

```
12:00:44.947 INFO  Main - リクエスト受付 requestId=583ee3d1 userId=102
12:00:44.947 INFO  UserService - ユーザー登録開始 102 bob
12:00:45.075 ERROR UserController - ユーザー登録でエラーが発生しました: ユーザー登録処理でエラーが発生しました
com.example.app.service.UserRegistrationException: ユーザー登録処理でエラーが発生しました
	at com.example.app.service.UserService.register(UserService.java:41)
	at com.example.app.controller.UserController.register(UserController.java:33)
	at com.example.app.Main.handleRequest(Main.java:60)
	at com.example.app.Main.runSequentially(Main.java:40)
	at com.example.app.Main.main(Main.java:33)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at org.codehaus.mojo.exec.AbstractExecJavaBase.executeMainMethod(AbstractExecJavaBase.java:402)
	at org.codehaus.mojo.exec.ExecJavaMojo.executeMainMethod(ExecJavaMojo.java:142)
	at org.codehaus.mojo.exec.AbstractExecJavaBase.doExecClassLoader(AbstractExecJavaBase.java:377)
	at org.codehaus.mojo.exec.AbstractExecJavaBase.lambda$execute$0(AbstractExecJavaBase.java:287)
	at java.base/java.lang.Thread.run(Thread.java:1474)
Caused by: com.example.app.repository.RepositoryException: ユーザーの保存に失敗しました
	at com.example.app.repository.UserRepository.save(UserRepository.java:33)
	at com.example.app.service.UserService.register(UserService.java:38)
	... 11 more
Caused by: java.sql.SQLException: DB への接続に失敗しました
	at com.example.fakedb.FakeDatabase.insert(FakeDatabase.java:32)
	at com.example.app.repository.UserRepository.save(UserRepository.java:28)
	... 12 more
Caused by: java.net.ConnectException: Connection refused: db.example.internal:5432 (擬似)
	at com.example.fakedb.FakeDatabase.insert(FakeDatabase.java:31)
	... 13 more
12:00:45.077 INFO  Main - リクエスト完了 requestId=583ee3d1 result=ERROR
```

最初の `ERROR` の行だけ読んでみてください。「ユーザー登録処理でエラーが発生しました」としか書かれておらず、**誰の登録が、なぜ失敗したのか** は分かりません。
その下に続くスタックトレースは、Controller が `e.printStackTrace()` を直書きしているために出ているものです（時刻もレベルも付いていないことに注目してください）。

このスタックトレースを読みます。見る場所は **2箇所** だけです。

1. **一番下の `Caused by`** を見る。ここが一番最初に起きた例外（根本原因）です
   - `java.net.ConnectException: Connection refused: db.example.internal:5432` → DB に接続できなかった、という **「なぜ」** が分かる
2. そこから **上に向かって** 読み、**最初に自分のパッケージ（`com.example.app`）が出てくる行** を見る。自分のコードのどこで起きたか、という **「どこ」** が分かる
   - 一番下のブロックには `com.example.app` の行がないので、1つ上のブロックへ
   - `at com.example.app.repository.UserRepository.save(UserRepository.java:28)` → `UserRepository.java` の 28 行目（`db.insert(...)` を呼んでいる行）

読むときの補足です。

- スタックトレースは **最後に投げられた例外が一番上**、元の例外ほど下に出ます。一番上の `UserRegistrationException` は Service で包み直した例外なので、ここだけ見ても原因は分かりません
- `at java.base/...` や `at org.codehaus.mojo...` は JDK や Maven の行です。自分のパッケージ名で絞って読んでください
- `... 11 more` は「上のブロックと同じ行が 11 行続くので省略した」という意味です

**期待される結果**: 根本原因は「DB への接続拒否」、起きた場所は `UserRepository.java` の 28 行目、と答えられる

### Step 2: 追えるログに直す（8分）

ソースコードに `NOTE(Step 2-x)` というコメントを 4 箇所入れています。順に直して、そのたびに `mvn -q compile exec:java` で出力を確認してください。

#### Step 2-1: 例外オブジェクトをログに渡す（Controller）

`src/main/java/com/example/app/controller/UserController.java` の `catch (Exception e)` の中を直します。

直す前:

```java
        } catch (Exception e) {
            // NOTE(Step 2-1): e.getMessage() だけを出しているので、原因（スタックトレース）が残らない
            log.error("ユーザー登録でエラーが発生しました: " + e.getMessage());
            // NOTE(Step 2-1): printStackTrace の直書き。ログの設定を通らず、標準エラー出力に直接出る
            e.printStackTrace();
            return "ERROR";
        }
```

直した後:

```java
        } catch (Exception e) {
            // 例外オブジェクトを最後の引数で渡すと、スタックトレース（Caused by を含む）がログに出る
            log.error("ユーザー登録に失敗しました userId={}", userId, e);
            return "ERROR";
        }
```

ポイントは2つです。

- `{}` の数より **1つ多い最後の引数** に例外を渡すと、SLF4J はそれをスタックトレースとして出力します。`e.getMessage()` を文字列に混ぜるのではなく、`e` そのものを渡します
- `e.printStackTrace()` は標準エラー出力に直接書くので、ログの設定（出力先、フォーマット、レベル）を通りません。本番では収集されない、時刻が付かない、他の行と混ざる、といった問題が起きます。試しに `e.printStackTrace()` だけ先に消して実行すると、スタックトレースが **完全に消える** ことも確認できます

```bash
mvn -q compile exec:java
```

**期待される結果**: `ERROR` の行に時刻とレベルが付き、その直下に `Caused by` を含むスタックトレースが続く。Step 1 と同じ読み方で根本原因にたどり着ける

```
12:01:17.669 ERROR UserController - ユーザー登録に失敗しました userId=102
com.example.app.service.UserRegistrationException: ユーザー登録処理でエラーが発生しました
	at com.example.app.service.UserService.register(UserService.java:41)
	...
Caused by: java.net.ConnectException: Connection refused: db.example.internal:5432 (擬似)
	at com.example.fakedb.FakeDatabase.insert(FakeDatabase.java:31)
	... 13 common frames omitted
```

#### Step 2-2: 文字列連結をプレースホルダにする（Service）

`src/main/java/com/example/app/service/UserService.java` の `register` の先頭を直します。

直す前:

```java
        // NOTE(Step 2-2): 文字列連結でログのメッセージを組み立てている
        log.info("ユーザー登録開始 " + userId + " " + name);
```

直した後:

```java
        // {} プレースホルダで値を渡す。key=value の形にすると検索しやすい
        log.info("ユーザー登録開始 userId={} name={}", userId, name);
```

- `{}` は引数の値で順に置き換えられます。文字列連結と違い、そのレベルが無効なとき（例: DEBUG を出さない設定で `log.debug`）は連結自体が行われないので無駄がありません
- `userId=101` のように **`キー=値` の形** にしておくと、`grep userId=101` で絞れます。`101 alice` のように値だけ並べると、後から何の数字か分かりません

**期待される結果**: `ユーザー登録開始 userId=101 name=alice` のように出る

#### Step 2-3: ログレベルを直す（Service）

同じファイルに2箇所あります。

直す前:

```java
            // NOTE(Step 2-3): 処理は続行できるが異常な状況。INFO でよいか？
            log.info("ユーザーが重複しています userId={}", userId);
```

```java
        // NOTE(Step 2-3): 正常に終わったのに ERROR で出している
        log.error("ユーザー登録完了 userId={}", userId);
```

直した後:

```java
            // 続行できるが異常な状況なので WARN
            log.warn("ユーザーが重複しています userId={}", userId);
```

```java
        // 業務上の節目なので INFO
        log.info("ユーザー登録完了 userId={}", userId);
```

レベルの目安は次のとおりです。「誰が、いつ見るか」で決めます。

| レベル | 意味 | 例 |
| --- | --- | --- |
| ERROR | 人が対応する必要がある。放置すると業務に影響する | DB に接続できない、必須の外部 API が落ちている |
| WARN | 処理は続行できるが、想定外または要注意 | 重複登録の要求、リトライで成功した、設定が既定値にフォールバックした |
| INFO | 業務上の節目。正常時に流れを追うための行 | 登録開始・完了、バッチの件数、起動時の設定値 |
| DEBUG | 開発時の詳細。本番では通常出さない | SQL とパラメータ、分岐の判定結果 |

正常終了を ERROR で出すと、監視のアラートが鳴り続けて **本当の ERROR が埋もれます**。逆に異常を INFO で出すと、障害時に `grep ERROR` しても見つかりません。

**期待される結果**: 3件目（重複）が `WARN`、1件目と4件目の完了が `INFO` で出る

#### Step 2-4: ID を入れる（Repository）

`src/main/java/com/example/app/repository/UserRepository.java` の `save` を直します。

直す前:

```java
        try {
            db.insert(userId, name);
            // NOTE(Step 2-4): 誰を保存したのか、この行だけでは分からない
            log.info("ユーザーを保存しました");
        } catch (SQLException e) {
            // NOTE(Step 2-4): 例外メッセージにも誰の保存に失敗したのかが入っていない
            throw new RepositoryException("ユーザーの保存に失敗しました", e);
        }
```

直した後:

```java
        try {
            db.insert(userId, name);
            log.info("ユーザーを保存しました userId={}", userId);
        } catch (SQLException e) {
            // 例外メッセージにも ID を入れる。ログに出るのは受け止める側（Controller）なので、ここでは投げ直すだけ
            throw new RepositoryException("ユーザーの保存に失敗しました userId=" + userId, e);
        }
```

- 「隣の行に ID があるから分かる」は本番では通用しません。他のリクエストの行が間に挟まります（発展課題1で体験します）
- 例外メッセージにも ID を入れておくと、`Caused by` の行だけ見ても誰の話か分かります
- ここでは **ログを出さずに投げ直すだけ** にしています。Repository でも ERROR を出すと、Controller の ERROR と合わせて同じ障害が2回出て、件数を数えるときに紛らわしくなります。「catch したら、出すか、投げ直すか、どちらか」が目安です

**期待される結果**: `ユーザーを保存しました userId=101` のように出る。エラー時は `Caused by: ...RepositoryException: ユーザーの保存に失敗しました userId=102` と出る

### Step 3: 直したログを読み直す（2分）

```bash
mvn -q compile exec:java
```

2件目の失敗が、次のように出ます。

```
12:01:17.669 ERROR UserController - ユーザー登録に失敗しました userId=102
com.example.app.service.UserRegistrationException: ユーザー登録処理でエラーが発生しました
	at com.example.app.service.UserService.register(UserService.java:41)
	...
Caused by: com.example.app.repository.RepositoryException: ユーザーの保存に失敗しました userId=102
	at com.example.app.repository.UserRepository.save(UserRepository.java:32)
	...
Caused by: java.net.ConnectException: Connection refused: db.example.internal:5432 (擬似)
```

Step 1 の出力と見比べてください。

- **誰が**: `userId=102`（ERROR の行だけで分かる）
- **どの処理で**: ユーザー登録（ERROR の行だけで分かる）
- **なぜ**: DB への接続拒否（一番下の `Caused by`）
- **どこで**: `UserRepository.java` の `db.insert` の行（下から上に向かって最初の `com.example.app`）

直す前は、この4つのうち「どの処理で」しか分かりませんでした。
`solution` ブランチと差分を取ると、直した箇所をまとめて確認できます（`solution` には発展課題1の MDC も入っています）。

```bash
git diff main solution
```

**期待される結果**: 「誰が、どの処理で、なぜ、どこで」失敗したかが、ERROR の行とその直下だけで分かる

### 発展課題1: MDC でリクエスト ID を全行に付ける

本編が早く終わった人向けです。2スレッドで同時にリクエストを処理するモードで実行します。

```bash
mvn -q compile exec:java -Dexec.args=concurrent
```

4件のリクエスト（201 dave、102 bob、202 erin、203 frank）が2スレッドで同時に流れ、ログが混ざります。
`リクエスト受付` と `リクエスト完了` の行には `requestId` がありますが、間の Service / Repository の行にはありません。
「userId=102 の失敗の直前に何が起きていたか」を追おうとすると、どの行がそのリクエストのものか分からないはずです。

これを解決するのが **MDC（Mapped Diagnostic Context）** です。スレッドに紐づく小さな Map で、ここに入れた値は **そのスレッドで出す全ログ行** に自動で付きます。

`src/main/java/com/example/app/Main.java` の `handleRequest` を直します。

直す前:

```java
    private static void handleRequest(UserController controller, long userId, String name) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        log.info("リクエスト受付 requestId={} userId={}", requestId, userId);
        String result = controller.register(userId, name);
        log.info("リクエスト完了 requestId={} result={}", requestId, result);
    }
```

直した後（`import org.slf4j.MDC;` を追加します）:

```java
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
```

次に `src/main/resources/logback.xml` のパターンに `%X{requestId}` を足します。`%X{キー}` が MDC の値を出す指定です。

直す前:

```xml
      <pattern>%d{HH:mm:ss.SSS} %-5level %logger{0} - %msg%n</pattern>
```

直した後:

```xml
      <pattern>%d{HH:mm:ss.SSS} %-5level [%X{requestId:--}] %logger{0} - %msg%n</pattern>
```

`:--` は「未設定ならハイフンを出す」という既定値の指定です（コロンとハイフンに続けて既定値を書く書式で、既定値がハイフン1文字なので `:--` になります）。

```bash
mvn -q compile exec:java -Dexec.args=concurrent
```

**期待される結果**: 全行の先頭に `[7f28fd31]` のような requestId が付く。`grep` で1リクエスト分だけ取り出せる

```bash
mvn -q compile exec:java -Dexec.args=concurrent > app.log 2>&1
grep '\[7f28fd31\]' app.log     # 括弧の中は ERROR 行の requestId に置き換える
```

```
12:01:19.440 INFO  [7f28fd31] Main - リクエスト受付 userId=102
12:01:19.442 INFO  [7f28fd31] UserService - ユーザー登録開始 userId=102 name=bob
12:01:19.553 ERROR [7f28fd31] UserController - ユーザー登録に失敗しました userId=102
12:01:19.555 INFO  [7f28fd31] Main - リクエスト完了 result=ERROR
```

補足です。

- `finally` で `MDC.clear()` を呼ぶのを忘れると、スレッドが使い回されたときに **前のリクエストの ID が付いたまま** になります。Web アプリでは、この `put` と `clear` をサーブレットフィルタや Spring のインターセプタで行うのが定番です
- 「スレッド名（`%thread`）を出せば十分では」と思うかもしれませんが、サーバーのスレッドはプールで使い回されるので、スレッド名はリクエストと1対1になりません
- Spring Boot には、この requestId（traceId）を自動で付ける仕組み（Micrometer Tracing）があります。現場のログに `[traceId, spanId]` のような角括弧があれば、それが MDC 経由で出ているものです

### 発展課題2: 構造化ログ（JSON）に切り替えて検索する

Kibana や CloudWatch Logs Insights のようなログ基盤は、ログを **人が読む文章** ではなく **機械が検索・集計できるデータ** として扱います。
その入口が構造化ログで、1行を1つの JSON にして出します。

設定ファイルを `logback-json.xml` に切り替えて実行します。コードは変えません。

```bash
mvn -q compile exec:java -Dexec.args=concurrent -Dlogback.configurationFile=src/main/resources/logback-json.xml > app.jsonl 2>/dev/null
head -2 app.jsonl
```

**期待される結果**: 1行1 JSON で出る。MDC に入れた `requestId` が自動的にフィールドになっている

```json
{"@timestamp":"2026-10-17T12:01:21.337841+09:00","message":"リクエスト受付 userId=201","logger_name":"com.example.app.Main","thread_name":"pool-1-thread-1","level":"INFO","requestId":"3ae5f051"}
```

`jq` で絞り込んでみます（`jq` がない場合は発表者のデモを見てください）。

```bash
# 1. ERROR の行の requestId を調べる
jq -r 'select(.level == "ERROR") | .requestId' app.jsonl

# 2. そのリクエストの行だけ取り出す（混ざったログが一本の流れに戻る）
jq -c 'select(.requestId == "ここに1の結果") | {level, logger_name, message}' app.jsonl

# 3. レベルごとの件数を数える
jq -r '.level' app.jsonl | sort | uniq -c

# 4. ERROR の行のスタックトレースだけ見る
jq -r 'select(.level == "ERROR") | .stack_trace' app.jsonl
```

**期待される結果**: 2 で4行（受付、登録開始、ERROR、完了）だけが出る。3 で `ERROR 1、INFO 18` のように集計される

ここで見せたいのは、ツールの設定ではなく **発想の転換** です。

- 文章のログ（`ユーザー123の登録が在庫不足で失敗しました`）から `userId=123` の失敗を全部探すには、正規表現を書く必要があります。フィールドなら `userId` で絞るだけです
- そのためには **message は短く固定し、変わる値はフィールドに逃がす** のが原則です。テキストのログでも「`キー=値` を必ず入れる」（Step 2-2、2-4）という形で、今日から同じ原則を使えます
- MDC に入れた値は JSON のフィールドになります。「MDC に入れる → JSON にすると検索キーになる」と地続きです

`userId` のような値もフィールドにしたい場合は、SLF4J 2.x の fluent API を使う方法があります。message が固定になり、値は JSON のフィールドとして出ます。

```java
// 変わる値をフィールドにする（JSON では "userId":101,"reason":"DUPLICATE" になる）
log.atWarn().addKeyValue("userId", userId).addKeyValue("reason", "DUPLICATE").log("ユーザー登録に失敗");
```

テキスト形式のパターンでこの値を出すには、`logback.xml` のパターンに `%kvp` を足します（出力例: `ユーザー登録に失敗 userId="101" reason="DUPLICATE"`）。

今日はやらないこと: Elasticsearch / Kibana / Datadog の構築、フィールド名の規約（ECS、OpenTelemetry）の詳細。後者は「チームでフィールド名を揃える」と覚えておけば十分です。

## トラブルシューティング

| 症状 | 原因 | 対処 |
| --- | --- | --- |
| `mvn -q compile exec:java` で何も出ない、または `BUILD FAILURE` | コンパイルエラー（`-q` でメッセージが隠れている） | `-q` を外して実行し、`[ERROR]` の行を読む |
| 日本語が文字化けする（Windows） | コンソールの文字コードが UTF-8 でない | 実行前に `chcp 65001` を実行する（**要確認**。直らない場合は Windows Terminal を使う） |
| `-Dexec.args=concurrent` を付けても順番に実行される | PowerShell が引数を分割している | `"-Dexec.args=concurrent"` のように引用符で囲む |
| `JoranException: Problem parsing XML document` | `logback.xml` の XML が壊れている（XML のコメント内に `--` を書くと起きる） | エラーメッセージの行番号を見て直す。コメント内で `--` を使わない |
| 全行に `[-]` と出る（発展課題1） | `MDC.put` が呼ばれていない、またはキー名が `%X{...}` と一致していない | `MDC.put("requestId", ...)` と `%X{requestId...}` のキー名を揃える |
| 別のリクエストの requestId が付いている（発展課題1） | `MDC.clear()` を忘れてスレッドが使い回された | `finally` で `MDC.clear()` を呼ぶ |
| JSON ではなく通常の形式で出る（発展課題2） | `-Dlogback.configurationFile=` のパスが違う | プロジェクトのルートで実行し、パスを `src/main/resources/logback-json.xml` にする |
| `jq: command not found` | jq が入っていない | macOS は `brew install jq`、Windows は `winget install jqlang.jq`。入れなくても `grep requestId` で代用できる |
| 実行の最後に `WARNING: thread ... will linger` のような警告が出る | exec-maven-plugin がスレッドプールの終了を待っている | 動作には影響しない。無視してよい |

## 片付け

なし。`app.log` や `app.jsonl` を作った場合は削除してください（`.gitignore` で `*.log` は除外されていますが、`*.jsonl` は除外されていません）。

## 参考資料

- [SLF4J user manual](https://www.slf4j.org/manual.html): プレースホルダ、例外の渡し方、fluent API の説明がある
- [Logback Manual: Layouts](https://logback.qos.ch/manual/layouts.html): `%d` や `%X{key}` などパターンの書式一覧。[MDC の章](https://logback.qos.ch/manual/mdc.html)も同じマニュアル内にある
- [logstash-logback-encoder](https://github.com/logfellow/logstash-logback-encoder): JSON 出力のエンコーダ。フィールドの追加や省略の設定方法
- [jq Manual](https://jqlang.org/manual/): `select` や `-r` オプションの説明
