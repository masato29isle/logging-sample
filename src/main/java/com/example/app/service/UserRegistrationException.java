package com.example.app.service;

/** ユーザー登録処理が想定外の理由で失敗したときに投げる例外。 */
public class UserRegistrationException extends RuntimeException {

    public UserRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
