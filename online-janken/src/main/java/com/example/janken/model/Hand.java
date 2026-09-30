package com.example.janken.model;

import java.util.Optional;

/** じゃんけんで選択できる3種類の手（機能仕様書 VAL-001）。 */
public enum Hand {
  /** グー。 */
  ROCK("rock"),
  /** チョキ。 */
  SCISSORS("scissors"),
  /** パー。 */
  PAPER("paper");

  /** API設計書で定義された、フォームや画面用の小文字の値。 */
  private final String value;

  /** Java内部の定数と、外部で使用する文字列を対応付ける。 */
  Hand(String value) {
    this.value = value;
  }

  /** 画面用データなどへ渡す外部値を返す。 */
  public String getValue() {
    return value;
  }

  /**
   * 外部値を手に変換する。未送信・空文字・許可値以外は空のOptionalを返す。
   * Optionalは「変換できる値がない」ことを表せる型。
   * 大文字化や空白除去はせず、設計書の3つの値だけを受け付ける。
   * エラー応答や確定済みの送信を無視する判断は、呼び出す側で行う。
   */
  public static Optional<Hand> fromValue(String value) {
    for (Hand hand : values()) {
      if (hand.value.equals(value)) {
        return Optional.of(hand);
      }
    }
    return Optional.empty();
  }
}
