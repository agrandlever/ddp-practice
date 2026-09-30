package com.example.janken.service;

/** 現在状態だけでは区別できない「今回の手の送信をどう処理したか」を表す。 */
public enum HandSubmissionOutcome {
  /** 対戦開始前、または参加していないため受け付けない。 */
  NOT_STARTED,
  /** 新しく確定し、相手の手を待っている。 */
  ACCEPTED_WAITING,
  /** 今回の確定で双方の手が揃い、勝敗が決まった。 */
  ACCEPTED_RESULT_READY,
  /** 再送信を無視した。相手はまだ未確定。 */
  ALREADY_CONFIRMED_WAITING,
  /** 再送信を無視した。双方確定済みで結果を表示できる。 */
  ALREADY_CONFIRMED_RESULT_READY,
  /** 未送信、空文字、または許可された3種類以外の入力。 */
  INVALID_HAND
}
