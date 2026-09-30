package com.example.janken.model;

/** ユーザー自身から見た勝敗。勝敗を計算する処理はこのenumには含めない。 */
public enum GameResult {
  /** 自分の勝ち。 */
  WIN("win"),
  /** 自分の負け。 */
  LOSE("lose"),
  /** 双方が同じ手を出した、あいこ。 */
  DRAW("draw");

  /** API設計書3.4節のModel属性resultに対応する値。 */
  private final String value;

  /** Java内部の定数と、画面へ渡す文字列を対応付ける。 */
  GameResult(String value) {
    this.value = value;
  }

  /** 勝敗メッセージの選択に使う外部値を返す。 */
  public String getValue() {
    return value;
  }
}
