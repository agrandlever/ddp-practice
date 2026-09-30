package com.example.janken.controller;

import com.example.janken.service.GameService;
import com.example.janken.service.HandSubmissionResult;
import com.example.janken.service.PlayState;
import com.example.janken.service.ResultSnapshot;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 画面への案内を担当し、参加・手の確定・勝敗などの判断はGameServiceに任せる。 */
// @Controllerでは戻り値の文字列をテンプレート名として扱う。
// @RestControllerは戻り値そのものをJSONなどのレスポンス本文として返す点が異なる。
@Controller
public class GameController {
  private final GameService gameService;

  // Springが管理する共通のGameServiceを受け取り、両Controllerで同じ対戦を扱う。
  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @GetMapping("/")
  public String showHome(HttpSession session, Model model, HttpServletResponse response) {
    // HttpSessionは、再読み込み後も同じ利用者を識別するために使う。
    // ServiceにはHTTPの仕組みを持ち込まず、識別用のStringだけを渡す。
    String sessionId = session.getId();
    return switch (gameService.joinOrGetHomeState(sessionId)) {
      case WAITING -> {
        // Modelは、テンプレートで表示やボタンの状態を決めるためのデータの受け渡し役。
        model.addAttribute("waiting", true);
        yield "index";
      }
      // redirect:はテンプレート名ではなく、指定URLへ再アクセスするようブラウザーへ
      // 指示する書き方。通常はHTTP 302を返し、ブラウザーが遷移先をGETする。
      case PLAYING -> "redirect:/play";
      case RESULT_READY -> "redirect:/result";
      case MATCH_FULL -> {
        // setStatusなら、エラー用テンプレートを表示しつつHTTPステータスを指定できる。
        // sendErrorは使わず、別のエラー画面への処理の切り替えを避ける。
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("errorCode", "MATCH_FULL");
        model.addAttribute("errorMessage", "現在対戦中のため参加できません。");
        yield "error/match-full";
      }
    };
  }

  @GetMapping("/play")
  public String showPlay(HttpSession session, Model model) {
    PlayState state = gameService.getPlayState(session.getId());
    return switch (state.status()) {
      case WAITING -> "redirect:/";
      case PLAYING -> {
        model.addAttribute("myHand", null);
        model.addAttribute("waitingForOpponent", false);
        yield "play";
      }
      case WAITING_FOR_OPPONENT -> {
        model.addAttribute("myHand", state.myHand().getValue());
        model.addAttribute("waitingForOpponent", true);
        yield "play";
      }
      case RESULT_READY -> "redirect:/result";
      // 切断通知はcheckStatus専用。getPlayStateの契約外の値は、正常な画面に置き換えない。
      case OPPONENT_DISCONNECTED -> throw new IllegalStateException(
          "getPlayStateでは切断通知を返しません");
    };
  }

  @PostMapping("/play")
  public String submitHand(
      // required=falseにより、hand未送信でもSpringが先に400で処理を止めず、
      // nullをServiceへ渡せる。不正入力か確定済みの再送信かはServiceが判断する。
      @RequestParam(name = "hand", required = false) String hand,
      HttpSession session, Model model, HttpServletResponse response) {
    HandSubmissionResult submission = gameService.submitHand(session.getId(), hand);
    return switch (submission.outcome()) {
      case NOT_STARTED -> "redirect:/";
      case ACCEPTED_WAITING -> {
        // 再取得せず、手を確定した処理と同じ時点の情報を画面へ渡す。
        model.addAttribute("myHand", submission.myHand().getValue());
        model.addAttribute("waitingForOpponent", true);
        yield "play";
      }
      case ACCEPTED_RESULT_READY, ALREADY_CONFIRMED_RESULT_READY -> "redirect:/result";
      case ALREADY_CONFIRMED_WAITING -> "redirect:/play";
      case INVALID_HAND -> {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("errorCode", "VALIDATION_ERROR");
        model.addAttribute("errorMessage", "グー・チョキ・パーのいずれかを選択してください。");
        // 手が未確定で、選択ボタンを操作できる状態をテンプレートへ伝える。
        model.addAttribute("myHand", null);
        model.addAttribute("waitingForOpponent", false);
        yield "play";
      }
    };
  }

  @GetMapping("/result")
  public String showResult(HttpSession session, Model model) {
    Optional<ResultSnapshot> result = gameService.getResult(session.getId());
    if (result.isEmpty()) {
      return "redirect:/";
    }

    // 表示だけでは結果確認済みにしない。getValueで設計書どおりの小文字にする。
    ResultSnapshot snapshot = result.get();
    model.addAttribute("myHand", snapshot.myHand().getValue());
    model.addAttribute("opponentHand", snapshot.opponentHand().getValue());
    model.addAttribute("result", snapshot.result().getValue());
    return "result";
  }

  @PostMapping("/result/return")
  public String returnToHome(HttpSession session) {
    // 結果確認の記録と次の対戦を始める判断はServiceに任せ、常に初期画面へ案内する。
    gameService.confirmResult(session.getId());
    return "redirect:/";
  }
}
