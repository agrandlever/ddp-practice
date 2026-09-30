package com.example.janken.service;

import com.example.janken.model.GameResult;
import com.example.janken.model.Hand;

/**
 * 双方確定後にだけ公開する結果の写し。変更可能なPlayerStateは返さない。
 *
 * @param myHand 自分の確定した手
 * @param opponentHand 相手の確定した手
 * @param result 自分から見た勝敗
 */
public record ResultSnapshot(Hand myHand, Hand opponentHand, GameResult result) {
}
