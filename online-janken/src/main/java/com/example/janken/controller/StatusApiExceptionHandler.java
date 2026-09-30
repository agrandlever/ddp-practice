package com.example.janken.controller;

import com.example.janken.dto.InternalErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestControllerAdviceは、共通処理の戻り値をJSONなどのレスポンス本文として返す仕組み。
// 画面用の@ControllerAdviceと分け、StatusApiControllerだけに適用することで、
// JSONを期待するAPIの呼び出し元へHTMLの画面を返してしまうことを防ぐ。
@RestControllerAdvice(assignableTypes = StatusApiController.class)
public class StatusApiExceptionHandler {

  // @ExceptionHandlerにより、このControllerで発生したExceptionの応答を共通化する。
  // ThrowableやError全般は対象にせず、例外の詳細もJSONには含めない。
  @ExceptionHandler(Exception.class)
  public ResponseEntity<InternalErrorResponse> handleException() {
    // GameServiceの再実行は状態変更や再度の例外につながるため、応答の作成だけを行う。
    // ResponseEntityは、HTTPステータス・Content-Type・本文をまとめて指定できる。
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new InternalErrorResponse(
            "INTERNAL_ERROR", "通信エラーが発生しました。ページを再読み込みしてください。"));
  }
}
