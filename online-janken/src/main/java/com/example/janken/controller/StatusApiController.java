package com.example.janken.controller;

import com.example.janken.dto.StatusResponse;
import com.example.janken.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @Controllerが画面用テンプレートを選ぶのに対し、@RestControllerは戻り値を本文にする。
// ここではSpringがStatusResponseをJSONへ変換し、statusだけを返す。
@RestController
@RequestMapping("/api")
public class StatusApiController {
  private final GameService gameService;

  public StatusApiController(GameService gameService) {
    this.gameService = gameService;
  }

  @GetMapping("/status")
  public StatusResponse getStatus(HttpSession session) {
    // 画面と同じHttpSessionで利用者を識別し、ServiceにはIDの文字列だけを渡す。
    // 正常応答は常に200なので、ステータス等を個別指定するResponseEntityは不要。
    return new StatusResponse(gameService.checkStatus(session.getId()));
  }
}
