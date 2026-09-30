package com.example.janken.dto;

import com.example.janken.model.PlayerStatus;

/**
 * API設計書3.6節の状態確認レスポンス。
 * recordは、指定した項目の保持と取得をJavaが自動生成するデータ用の型。
 * JSONの項目をstatusだけにするため、他のデータは持たせない。
 *
 * @param status 現在のユーザーから見た状態。JSONではenum名をそのまま返す。
 */
public record StatusResponse(PlayerStatus status) {
}
