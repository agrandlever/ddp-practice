package com.example.janken.service;

import com.example.janken.model.GameResult;
import com.example.janken.model.GameState;
import com.example.janken.model.Hand;
import com.example.janken.model.PlayerState;
import com.example.janken.model.PlayerStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 2人分の参加・対戦状態と、その状態を変更する規則をまとめて管理する。
 * DBを使わず、複数のControllerから同じ対戦を扱うため、Springが共有するServiceに保持する。
 * アプリケーションの再起動で状態は失われる（要件定義書4.1節）。
 *
 * <p>全public操作をsynchronizedで保護する。「空き枠確認→登録」や
 * 「手の確認→確定→勝敗判定」の途中に別のリクエストが割り込むと状態が壊れるため、
 * 読み取りと戻り値の作成も含めて、同じServiceのロックで一まとまりに実行する。
 */
@Service
public class GameService {
  private static final Duration DISCONNECTION_TIMEOUT = Duration.ofSeconds(10);

  private final GameState gameState = new GameState();
  private final Clock clock;

  // 結果画面の滞在時間を次対戦の通信停止時間に含めないための、監視再開の基準。
  // 最終状態確認時刻そのものを書き換えず、新しい対戦の開始時刻を別に保持する。
  private Instant matchStartedAt;

  /** Springによる通常の生成ではシステム時刻を使う。 */
  public GameService() {
    this(Clock.systemUTC());
  }

  /** 将来のテストではClockを渡し、実際に10秒待たずに境界時刻を確認できる。 */
  public GameService(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clockは必須です");
  }

  /** API設計書3.1節：参加登録、重複防止、人数制限と初期画面の状態判断。 */
  public synchronized HomeState joinOrGetHomeState(String sessionId) {
    Objects.requireNonNull(sessionId, "sessionIdは必須です");
    Instant now = clock.instant();
    releaseExpiredWaitingPlayers(now);
    PlayerState player = findPlayer(sessionId);

    if (player == null) {
      if (hasTwoPlayers()) {
        return HomeState.MATCH_FULL;
      }
      player = new PlayerState(sessionId, now);
      if (gameState.getPlayerOne() == null) {
        gameState.setPlayerOne(player);
      } else {
        gameState.setPlayerTwo(player);
      }
      // 既存参加者の再読み込みでは初期化しない。新規登録で2人揃った時だけ開始する。
      if (hasTwoPlayers()) {
        startMatch(now);
      }
    }

    return switch (determinePlayerStatus(player)) {
      case RESULT_READY -> HomeState.RESULT_READY;
      case PLAYING, WAITING_FOR_OPPONENT -> HomeState.PLAYING;
      default -> HomeState.WAITING;
    };
  }

  /** API設計書3.2節：画面表示の判断と自分の手だけを返す。通知は消費しない。 */
  public synchronized PlayState getPlayState(String sessionId) {
    PlayerState player = findPlayer(sessionId);
    if (player == null || !hasTwoPlayers()) {
      return new PlayState(PlayerStatus.WAITING, null);
    }
    // /playは双方確定済みなら/resultへ進む。ホームと状態確認のWAITING判定とは用途が異なる。
    PlayerStatus status = player.getResult() != null
        ? PlayerStatus.RESULT_READY : determinePlayerStatus(player);
    return new PlayState(status, player.getHand());
  }

  /** API設計書3.3節：手を確定し、最後の1人なら両者の勝敗まで記録する。 */
  public synchronized HandSubmissionResult submitHand(String sessionId, String handValue) {
    PlayerState player = findPlayer(sessionId);
    if (player == null || !hasTwoPlayers()) {
      return new HandSubmissionResult(HandSubmissionOutcome.NOT_STARTED, null);
    }

    // 再送信は値が不正でも「無視する」仕様なので、入力検証より先に確定済みか確認する。
    if (player.getHand() != null) {
      HandSubmissionOutcome outcome = player.getResult() == null
          ? HandSubmissionOutcome.ALREADY_CONFIRMED_WAITING
          : HandSubmissionOutcome.ALREADY_CONFIRMED_RESULT_READY;
      return new HandSubmissionResult(outcome, player.getHand());
    }

    Optional<Hand> hand = Hand.fromValue(handValue);
    if (hand.isEmpty()) {
      return new HandSubmissionResult(HandSubmissionOutcome.INVALID_HAND, null);
    }

    player.setHand(hand.get());
    PlayerState opponent = findOpponent(player);
    if (opponent.getHand() == null) {
      return new HandSubmissionResult(HandSubmissionOutcome.ACCEPTED_WAITING, player.getHand());
    }

    player.setResult(determineResult(player.getHand(), opponent.getHand()));
    opponent.setResult(determineResult(opponent.getHand(), player.getHand()));
    return new HandSubmissionResult(HandSubmissionOutcome.ACCEPTED_RESULT_READY, player.getHand());
  }

  /** API設計書3.4節：結果がなければ空のOptionalを返す。表示だけでは確認済みにしない。 */
  public synchronized Optional<ResultSnapshot> getResult(String sessionId) {
    PlayerState player = findPlayer(sessionId);
    if (player == null || player.getResult() == null) {
      return Optional.empty();
    }
    PlayerState opponent = findOpponent(player);
    // 相手の手を公開する入口をここだけに限定し、双方確定済みを確認する。
    if (opponent == null || player.getHand() == null || opponent.getHand() == null) {
      return Optional.empty();
    }
    return Optional.of(new ResultSnapshot(player.getHand(), opponent.getHand(), player.getResult()));
  }

  /** API設計書3.5節・BR-008：戻る操作を記録し、双方確認済みなら同じ2人で再対戦する。 */
  public synchronized void confirmResult(String sessionId) {
    PlayerState player = findPlayer(sessionId);
    if (player == null || player.getResult() == null) {
      return;
    }
    player.setResultConfirmed(true);
    PlayerState opponent = findOpponent(player);
    if (opponent != null && opponent.isResultConfirmed()) {
      startMatch(clock.instant());
    }
  }

  /** API設計書3.6節・5章：時刻更新、切断判定、一度だけの通知と状態判断。 */
  public synchronized PlayerStatus checkStatus(String sessionId) {
    Objects.requireNonNull(sessionId, "sessionIdは必須です");
    Instant now = clock.instant();
    PlayerState player = findPlayer(sessionId);
    if (player != null) {
      // 今届いた状態確認を記録してから、通信が届いていない相手を調べる。
      player.setLastStatusCheckedAt(now);
    }
    releaseExpiredWaitingPlayers(now);
    if (player == null) {
      // 状態確認だけでは参加登録しない。
      return PlayerStatus.WAITING;
    }

    PlayerState opponent = findOpponent(player);
    // 結果がある間は、結果画面の人も、確認後に待っている人も切断監視から外す。
    if (opponent != null && player.getResult() == null && opponent.getResult() == null
        && isTimedOut(opponent, now, matchStartedAt)) {
      releasePlayer(opponent);
      resetRoundData(player);
      matchStartedAt = null;
      player.setOpponentDisconnectedPending(true);
    }

    // 通知を返すと同時にフラグを下ろす。同時に複数回呼ばれても通知は1回だけになる。
    if (player.isOpponentDisconnectedPending()) {
      player.setOpponentDisconnectedPending(false);
      return PlayerStatus.OPPONENT_DISCONNECTED;
    }
    return determinePlayerStatus(player);
  }

  // 以下のprivate処理は、必ずsynchronizedなpublic操作の内側から呼び出す。

  private PlayerState findPlayer(String sessionId) {
    Objects.requireNonNull(sessionId, "sessionIdは必須です");
    PlayerState one = gameState.getPlayerOne();
    if (one != null && one.isParticipating() && one.getSessionId().equals(sessionId)) {
      return one;
    }
    PlayerState two = gameState.getPlayerTwo();
    if (two != null && two.isParticipating() && two.getSessionId().equals(sessionId)) {
      return two;
    }
    return null;
  }

  private PlayerState findOpponent(PlayerState player) {
    PlayerState opponent = gameState.getPlayerOne() == player
        ? gameState.getPlayerTwo() : gameState.getPlayerOne();
    return opponent != null && opponent.isParticipating() ? opponent : null;
  }

  private boolean hasTwoPlayers() {
    return gameState.getPlayerOne() != null && gameState.getPlayerOne().isParticipating()
        && gameState.getPlayerTwo() != null && gameState.getPlayerTwo().isParticipating();
  }

  private PlayerStatus determinePlayerStatus(PlayerState player) {
    if (player.getResult() != null) {
      // 結果確認後のWAITINGは参加枠を保持する。結果を消さず、通常の待機と区別する。
      return player.isResultConfirmed() ? PlayerStatus.WAITING : PlayerStatus.RESULT_READY;
    }
    if (findOpponent(player) == null) {
      return PlayerStatus.WAITING;
    }
    return player.getHand() == null ? PlayerStatus.PLAYING : PlayerStatus.WAITING_FOR_OPPONENT;
  }

  private void startMatch(Instant now) {
    resetRoundData(gameState.getPlayerOne());
    resetRoundData(gameState.getPlayerTwo());
    matchStartedAt = now;
  }

  private void resetRoundData(PlayerState player) {
    player.setHand(null);
    player.setResult(null);
    player.setResultConfirmed(false);
  }

  private GameResult determineResult(Hand mine, Hand opponent) {
    // 同じ手ならあいこ。それ以外は、自分の手が勝てる相手かを3通りで確認する。
    if (mine == opponent) {
      return GameResult.DRAW;
    }
    boolean wins = switch (mine) {
      case ROCK -> opponent == Hand.SCISSORS;
      case SCISSORS -> opponent == Hand.PAPER;
      case PAPER -> opponent == Hand.ROCK;
    };
    return wins ? GameResult.WIN : GameResult.LOSE;
  }

  private void releaseExpiredWaitingPlayers(Instant now) {
    // 2人で結果確認を待っている状態は通常WAITINGではない。参加枠を解放しない。
    if (hasTwoPlayers()) {
      return;
    }
    PlayerState one = gameState.getPlayerOne();
    PlayerState two = gameState.getPlayerTwo();
    if (one != null && one.getResult() == null && isTimedOut(one, now, null)) {
      releasePlayer(one);
    }
    if (two != null && two.getResult() == null && isTimedOut(two, now, null)) {
      releasePlayer(two);
    }
  }

  private boolean isTimedOut(PlayerState player, Instant now, Instant monitoringStartedAt) {
    Instant reference = player.getLastStatusCheckedAt();
    if (monitoringStartedAt != null && reference.isBefore(monitoringStartedAt)) {
      reference = monitoringStartedAt;
    }
    // 「10秒を超えた」ではなく「10秒以上」で切断とする。
    return Duration.between(reference, now).compareTo(DISCONNECTION_TIMEOUT) >= 0;
  }

  private void releasePlayer(PlayerState player) {
    player.setParticipating(false);
    if (gameState.getPlayerOne() == player) {
      gameState.setPlayerOne(null);
    } else if (gameState.getPlayerTwo() == player) {
      gameState.setPlayerTwo(null);
    }
  }
}
