package com.example.janken.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 1人分の参加・対戦状態をメモリ上に保持する。
 * HTTPセッション本体には依存せず、識別用の文字列だけを受け取る。
 * このクラスはデータの保持を担当し、状態変更の可否や順序はGameServiceで判断する。
 */
public class PlayerState {
  /** 同一ユーザーを区別するセッションの識別文字列。作成後は変更しない。 */
  private final String sessionId;

  /** 現在、参加枠を使用しているか。結果情報の保持とは区別する。 */
  private boolean participating;

  /** 確定した自分の手。未選択、または対戦情報を消去した場合はnull。 */
  private Hand hand;

  /** 自分から見た勝敗。結果がまだ存在しない場合はnull。 */
  private GameResult result;

  /** 「初期画面に戻る」操作により、結果確認を終了したか。 */
  private boolean resultConfirmed;

  /** 最後に状態確認を受けた時刻。10秒の切断判定に使用する。 */
  private Instant lastStatusCheckedAt;

  /** 相手の切断をまだ通知していない場合にtrue。一度だけの通知に使用する。 */
  private boolean opponentDisconnectedPending;

  /**
   * 新しく参加するユーザーを作る。手・結果はnull、確認・通知フラグはfalseで始まる。
   * 初回の状態確認前にも時刻を持てるよう、呼び出す側から初期時刻を渡す。
   */
  public PlayerState(String sessionId, Instant initialStatusCheckedAt) {
    this.sessionId = Objects.requireNonNull(sessionId, "sessionIdは必須です");
    this.lastStatusCheckedAt = Objects.requireNonNull(
        initialStatusCheckedAt, "初期の状態確認時刻は必須です");
    this.participating = true;
  }

  /** ユーザーを識別する文字列を返す。 */
  public String getSessionId() {
    return sessionId;
  }

  /** 現在参加枠を使用しているかを返す。 */
  public boolean isParticipating() {
    return participating;
  }

  /** 参加枠の使用状態を記録する。手や結果は自動的には削除しない。 */
  public void setParticipating(boolean participating) {
    this.participating = participating;
  }

  /** 確定した手を返す。未選択の場合はnull。 */
  public Hand getHand() {
    return hand;
  }

  /**
   * 手を記録する。nullで消去できるため、再対戦の初期化にも使用できる。
   * 確定済みの再送信を無視する制御はGameServiceが担当する。
   */
  public void setHand(Hand hand) {
    this.hand = hand;
  }

  /** 自分の勝敗を返す。未確定の場合はnull。 */
  public GameResult getResult() {
    return result;
  }

  /** 判定済みの勝敗を記録する。nullを指定すると結果を消去する。 */
  public void setResult(GameResult result) {
    this.result = result;
  }

  /** 結果確認を終了しているかを返す。 */
  public boolean isResultConfirmed() {
    return resultConfirmed;
  }

  /** 結果確認の状態を記録する。次の対戦では呼び出す側がfalseへ戻す。 */
  public void setResultConfirmed(boolean resultConfirmed) {
    this.resultConfirmed = resultConfirmed;
  }

  /** 切断判定の基準となる最終状態確認時刻を返す。 */
  public Instant getLastStatusCheckedAt() {
    return lastStatusCheckedAt;
  }

  /** 呼び出す側が取得した時刻を記録する。ここでは切断判定を行わない。 */
  public void setLastStatusCheckedAt(Instant lastStatusCheckedAt) {
    this.lastStatusCheckedAt = Objects.requireNonNull(
        lastStatusCheckedAt, "最終状態確認時刻は必須です");
  }

  /** 相手の切断について未通知の情報があるかを返す。読み取りだけでは消費しない。 */
  public boolean isOpponentDisconnectedPending() {
    return opponentDisconnectedPending;
  }

  /** 切断通知の有無を記録する。通知済みにする際は呼び出す側がfalseを設定する。 */
  public void setOpponentDisconnectedPending(boolean opponentDisconnectedPending) {
    this.opponentDisconnectedPending = opponentDisconnectedPending;
  }
}
