package com.example.janken.service;

import com.example.janken.model.Hand;
import com.example.janken.model.PlayerStatus;

/**
 * 手の選択画面に必要な、変更不能なデータの写し。相手の手は含めない。
 *
 * @param status WAITING、PLAYING、WAITING_FOR_OPPONENT、RESULT_READYのいずれか
 * @param myHand 自分の確定済みの手。未確定ならnull
 */
public record PlayState(PlayerStatus status, Hand myHand) {
}
