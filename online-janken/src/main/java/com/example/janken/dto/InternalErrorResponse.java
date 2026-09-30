package com.example.janken.dto;

/**
 * API設計書6.3節の内部エラー応答。
 * recordは、指定した項目の保持と取得をJavaが自動生成するデータ用の型。
 * JSONを設計書どおりの2項目に限定し、例外名やスタックトレースなどは持たせない。
 *
 * @param errorCode エラーの種類を示すコード
 * @param message 利用者に表示するメッセージ
 */
public record InternalErrorResponse(String errorCode, String message) {
}
