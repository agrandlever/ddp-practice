package com.example.janken.model;

/**
 * 2人分の状態をまとめてメモリ上に保持する。DBやファイルへの保存は行わない。
 * 参加人数、手、結果確認状態から分かる情報は、別のフィールドに重複して保存しない。
 * 参加可否、対戦開始、枠の解放、同時アクセスの制御はGameServiceが担当する。
 */
public class GameState {
  /** 1人目の状態。まだ割り当てられていない場合はnull。 */
  private PlayerState playerOne;

  /** 2人目の状態。まだ割り当てられていない場合はnull。 */
  private PlayerState playerTwo;

  /** 参加者がいない初期状態を作る。 */
  public GameState() {
  }

  /** 1人目の状態を返す。結果情報が残っていても、参加中とは限らない。 */
  public PlayerState getPlayerOne() {
    return playerOne;
  }

  /** 1人目の状態を設定する。nullを指定すると保持していた情報を外す。 */
  public void setPlayerOne(PlayerState playerOne) {
    this.playerOne = playerOne;
  }

  /** 2人目の状態を返す。参加中かはPlayerStateのparticipatingで確認する。 */
  public PlayerState getPlayerTwo() {
    return playerTwo;
  }

  /** 2人目の状態を設定する。重複参加や置き換えの可否はGameServiceで確認する。 */
  public void setPlayerTwo(PlayerState playerTwo) {
    this.playerTwo = playerTwo;
  }
}
