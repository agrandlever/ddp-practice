package com.example.janken.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

// @ControllerAdviceは、Controllerに共通する例外処理などをまとめる仕組み。
// assignableTypesで対象をGameControllerだけに限定する。
// @RestControllerAdviceは戻り値をJSONなどの本文にするが、こちらは画面を返す。
// HTMLを表示する画面系とJSONを読むAPIでは応答形式が違うため、処理を分ける。
@ControllerAdvice(assignableTypes = GameController.class)
public class GameControllerExceptionHandler {

  // @ExceptionHandlerは、指定した型の例外が発生したときに呼ぶメソッドを指定する。
  // Exceptionを対象にし、ThrowableやError全般を捕捉する設定にはしない。
  @ExceptionHandler(Exception.class)
  public ModelAndView handleException(HttpServletRequest request, Exception exception)
      throws Exception {
    // servletPathはコンテキストパス（アプリの配置先）を含まない。
    // 現在の標準のServletマッピングでは、/janken/playで配置しても/playとして判定できる。
    String path = request.getServletPath();
    String template = switch (request.getMethod() + " " + path) {
      case "GET /" -> "index";
      case "GET /play", "POST /play" -> "play";
      case "GET /result", "POST /result/return" -> "result";
      // 対象Controllerにないリクエストの表示先を推測せず、元の例外をSpringへ戻す。
      default -> throw exception;
    };

    // ModelAndViewは、表示テンプレート・画面へ渡すデータ・HTTPステータスをまとめて指定できる。
    // redirect:を付けず、エラーが起きた画面を500で直接表示する。
    ModelAndView modelAndView = new ModelAndView(template);
    modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    modelAndView.addObject("errorCode", "INTERNAL_ERROR");
    modelAndView.addObject("errorMessage", "通信エラーが発生しました。ページを再読み込みしてください。");

    // GameServiceを再実行すると、参加登録などの状態変更や再度の例外が起こり得る。
    // 通常データの復元・推測はせず、エラー表示に必要な2項目だけを渡す。
    // ここではGameStateを変更せず、UC-003 E1の対戦終了処理も行わない。
    return modelAndView;
  }
}
