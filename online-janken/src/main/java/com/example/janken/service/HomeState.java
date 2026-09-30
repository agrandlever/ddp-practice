package com.example.janken.service;

/** 初期画面へのアクセス結果。HTTP応答への変換はControllerが担当する。 */
public enum HomeState {
  /** 初回の相手待ち、または結果確認後の相手待ち。 */
  WAITING,
  /** 手の選択画面へ進める。自分の手が確定済みの場合も含む。 */
  PLAYING,
  /** 本人がまだ確認を終了していない結果がある。 */
  RESULT_READY,
  /** すでに2人が参加しているため、新規参加できない。 */
  MATCH_FULL
}
