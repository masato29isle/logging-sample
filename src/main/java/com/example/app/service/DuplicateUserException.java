package com.example.app.service;

/** 同じ userId のユーザーが既に登録されているときに投げる例外（業務上の想定内エラー）。 */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(long userId) {
        super("ユーザーが重複しています userId=" + userId);
    }
}
