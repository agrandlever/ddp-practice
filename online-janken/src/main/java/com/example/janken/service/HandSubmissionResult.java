package com.example.janken.service;

import com.example.janken.model.Hand;

/**
 * 送信処理と同じ時点の情報を返す。Controllerによる状態の再取得を不要にする。
 *
 * @param outcome 今回の送信の処理結果
 * @param myHand 処理後の自分の確定済みの手。未確定ならnull
 */
public record HandSubmissionResult(HandSubmissionOutcome outcome, Hand myHand) {
}
