package com.example.janken.model;

/** API設計書3.6節の、現在のユーザーから見た5種類の状態。 */
public enum PlayerStatus {
  /** 対戦相手を待っている。未参加セッションへの応答にも使用する。 */
  WAITING,
  /** 対戦中で、自分の手を選択できる。 */
  PLAYING,
  /** 自分の手は確定済みで、相手の選択を待っている。 */
  WAITING_FOR_OPPONENT,
  /** 双方の手が確定し、結果を表示できる。 */
  RESULT_READY,
  /** 相手の切断を検知したことを通知する。 */
  OPPONENT_DISCONNECTED
}
