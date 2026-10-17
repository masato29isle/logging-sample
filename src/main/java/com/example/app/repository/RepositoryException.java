package com.example.app.repository;

/** Repository 層でのデータアクセス失敗を表す例外。 */
public class RepositoryException extends RuntimeException {

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
