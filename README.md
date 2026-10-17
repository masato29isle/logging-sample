# logging-sample

社内勉強会「ログの読み方と出し方」のハンズオン用サンプルプロジェクトです。
Controller / Service / Repository の3層構造のユーザー登録処理に、**ログの出し方が悪い箇所** が意図的に入っています。
前回の勉強会で使った `archunit-sample` を元にしたログ学習用のサンプルで、ArchUnit は入っていません。

各クラスには Spring の `@RestController` / `@Service` / `@Repository` を付けていますが、
**Spring Boot ではないので Web アプリとしては起動しません**。業務コードと同じ見た目にするための飾りです。
代わりに `Main` クラスが Web フレームワークの役をして、ユーザー登録のリクエストを Controller に渡します。

## ハンズオン手順

手順は [docs/handson.md](docs/handson.md) を参照してください。

## ブランチ

| ブランチ | 内容 |
| --- | --- |
| `main` | 開始状態。ログの出し方が悪く、障害が起きても「誰が、どの処理で、なぜ失敗したか」が追えない |
| `solution` | 完成状態。本編の修正（例外の渡し方、プレースホルダ、ログレベル、ID）と、発展課題1の MDC（requestId）まで入れたもの |

ハンズオンでは `main` から始めます。詰まったときは `solution` ブランチを参照してください。

```bash
# 開始状態との差分を確認する
git diff main solution

# 特定のファイルだけ完成状態を取り出す（例: Controller）
git checkout solution -- src/main/java/com/example/app/controller/UserController.java
```

発展課題2の JSON 形式のログは、ブランチではなく設定ファイル（`src/main/resources/logback-json.xml`）の切り替えで試します。

## 前提環境

- JDK 17 以上（`java -version` で確認）
- Maven 3.9.x（`mvn -v` で確認）
- 任意: IntelliJ IDEA や VS Code などのエディタ
- 任意: `jq`（発展課題2で JSON のログを絞り込むのに使う。なくても発表者のデモで内容は分かる）

## 始め方

```bash
git clone https://github.com/masato29isle/logging-sample.git
cd logging-sample
mvn -q compile exec:java
```

ユーザー登録が4件実行され、ログが出ます（2件目がエラー、3件目が重複で失敗します）。
依存ライブラリのダウンロードに時間がかかるため、勉強会の前に一度実行しておいてください。

```bash
# 発展課題1: 2 スレッドで同時に処理してログが混ざる様子を見る
mvn -q compile exec:java -Dexec.args=concurrent

# 発展課題2: JSON 形式で出力する
mvn -q compile exec:java -Dlogback.configurationFile=src/main/resources/logback-json.xml
```

## 構成

```
pom.xml                                   Maven の設定（SLF4J、Logback、exec-maven-plugin）
docs/handson.md                           ハンズオン手順
src/main/resources/logback.xml            ログの出力先とフォーマット
src/main/resources/logback-json.xml       JSON 形式で出すときの設定（発展課題2）
src/main/java/com/example/app
├── Main.java                             起動クラス。リクエストを受け付けて Controller に渡す
├── controller/UserController.java        @RestController  リクエストを受け付ける
├── service/UserService.java              @Service         業務ロジック（重複チェック）
├── service/DuplicateUserException.java   重複時の例外（想定内）
├── service/UserRegistrationException.java 登録失敗の例外（想定外）
├── repository/UserRepository.java        @Repository      データの保存
└── repository/RepositoryException.java   データアクセス失敗の例外
src/main/java/com/example/fakedb
└── FakeDatabase.java                     JDBC ドライバと DB の代わり（インメモリ）。読まなくてよい
```

ビルド成果物（`target/`）や IDE の設定ファイルは `.gitignore` で除外しています。
