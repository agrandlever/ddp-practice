package com.example.janken.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.janken.model.GameResult;
import com.example.janken.model.Hand;
import com.example.janken.model.PlayerStatus;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Web画面とは分けて、公開メソッドから参加枠と状態遷移の仕様を確認する。 */
class GameServiceTest {
  private static final Instant BASE = Instant.parse("2026-09-29T00:00:00Z");
  private Clock clock;
  private GameService service;

  @BeforeEach
  void setUp() {
    clock = mock(Clock.class);
    atMillis(0);
    service = new GameService(clock);
  }

  // 実際に待つとテストが遅く不安定になるため、サービスに渡す時計だけを進める。
  private void atMillis(long elapsed) {
    when(clock.instant()).thenReturn(BASE.plusMillis(elapsed));
  }

  private void startMatch() {
    assertEquals(HomeState.WAITING, service.joinOrGetHomeState("A"));
    assertEquals(HomeState.PLAYING, service.joinOrGetHomeState("B"));
  }

  private void finishMatch() {
    startMatch();
    service.submitHand("A", "rock");
    service.submitHand("B", "scissors");
  }

  @ParameterizedTest
  @ValueSource(strings = {"A", "B"})
  void confirmedPlayerKeepsSlotWithoutPollingBeyondTenSeconds(String confirmed) {
    finishMatch();
    String other = confirmed.equals("A") ? "B" : "A";
    service.confirmResult(confirmed);

    // 先に3人目をアクセスさせ、本人のポーリングによる時刻更新に頼らず保持を確認する。
    for (long elapsed : new long[] {10_000, 60_000}) {
      atMillis(elapsed);
      assertEquals(HomeState.MATCH_FULL, service.joinOrGetHomeState("C"));
      assertEquals(HomeState.WAITING, service.joinOrGetHomeState(confirmed));
      assertEquals(PlayerStatus.WAITING, service.checkStatus(confirmed));
      assertEquals(PlayerStatus.RESULT_READY, service.checkStatus(other));
      assertTrue(service.getResult(other).isPresent());
    }
  }

  @Test
  void bothConfirmingRestartsSamePlayersAndRestartsTimeout() {
    finishMatch();
    service.confirmResult("A");
    atMillis(60_000);
    service.confirmResult("B");

    for (String id : new String[] {"A", "B"}) {
      assertEquals(HomeState.PLAYING, service.joinOrGetHomeState(id));
      assertNull(service.getPlayState(id).myHand());
      assertTrue(service.getResult(id).isEmpty());
    }
    assertEquals(HomeState.MATCH_FULL, service.joinOrGetHomeState("C"));
    atMillis(69_999);
    assertEquals(PlayerStatus.PLAYING, service.checkStatus("A"));
    atMillis(70_000);
    assertEquals(PlayerStatus.OPPONENT_DISCONNECTED, service.checkStatus("A"));
    assertEquals(PlayerStatus.WAITING, service.checkStatus("A"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"A", "B"})
  void disconnectNotifiesOnceAndResetsSurvivorBeforeHomeAccess(String survivor) {
    startMatch();
    String disconnected = survivor.equals("A") ? "B" : "A";
    service.submitHand(survivor, "paper");
    atMillis(9_999);
    assertEquals(PlayerStatus.WAITING_FOR_OPPONENT, service.checkStatus(survivor));
    atMillis(10_000);
    assertEquals(PlayerStatus.OPPONENT_DISCONNECTED, service.checkStatus(survivor));

    // GET / に相当する操作を挟まなくても、サーバー内で既に待機状態へ戻っている。
    assertEquals(PlayerStatus.WAITING, service.getPlayState(survivor).status());
    assertNull(service.getPlayState(survivor).myHand());
    assertTrue(service.getResult(survivor).isEmpty());
    assertEquals(PlayerStatus.WAITING, service.checkStatus(survivor));
    assertEquals(PlayerStatus.WAITING, service.checkStatus(survivor));
    assertEquals(PlayerStatus.WAITING, service.checkStatus(disconnected));
    assertEquals(HomeState.WAITING, service.joinOrGetHomeState(survivor));
    assertEquals(HomeState.PLAYING, service.joinOrGetHomeState("C"));
    assertEquals(PlayerStatus.PLAYING, service.checkStatus(survivor));
    assertEquals(HandSubmissionOutcome.ACCEPTED_WAITING,
        service.submitHand(survivor, "rock").outcome());

    // 「1回だけ」は切断1件ごと。新しい相手の切断は改めて通知される。
    atMillis(20_000);
    assertEquals(PlayerStatus.OPPONENT_DISCONNECTED, service.checkStatus(survivor));
    assertEquals(PlayerStatus.WAITING, service.checkStatus(survivor));
  }

  @Test
  void ordinaryWaitingSlotExpiresAtTenSeconds() {
    service.joinOrGetHomeState("A");
    atMillis(10_000);
    assertEquals(HomeState.WAITING, service.joinOrGetHomeState("B"));
    assertEquals(PlayerStatus.WAITING, service.checkStatus("A"));
    assertEquals(HomeState.PLAYING, service.joinOrGetHomeState("C"));
    assertEquals(HomeState.MATCH_FULL, service.joinOrGetHomeState("A"));
  }

  @Test
  void pollingRefreshesOrdinaryWaitingSlot() {
    service.joinOrGetHomeState("A");
    atMillis(9_999);
    assertEquals(PlayerStatus.WAITING, service.checkStatus("A"));
    atMillis(10_000);
    assertEquals(HomeState.PLAYING, service.joinOrGetHomeState("B"));
  }

  @Test
  void resultScreenDoesNotTimeOut() {
    finishMatch();
    atMillis(60_000);
    assertEquals(HomeState.MATCH_FULL, service.joinOrGetHomeState("C"));
    assertEquals(PlayerStatus.RESULT_READY, service.checkStatus("A"));
    assertEquals(PlayerStatus.RESULT_READY, service.checkStatus("B"));
  }

  @ParameterizedTest
  @CsvSource({
      "rock, rock, DRAW, DRAW", "rock, scissors, WIN, LOSE", "rock, paper, LOSE, WIN",
      "scissors, rock, LOSE, WIN", "scissors, scissors, DRAW, DRAW", "scissors, paper, WIN, LOSE",
      "paper, rock, WIN, LOSE", "paper, scissors, LOSE, WIN", "paper, paper, DRAW, DRAW"
  })
  void allHandPairsProduceResults(String first, String second, GameResult a, GameResult b) {
    startMatch();
    assertEquals(HandSubmissionOutcome.ACCEPTED_WAITING, service.submitHand("A", first).outcome());
    assertTrue(service.getResult("A").isEmpty());
    assertTrue(service.getResult("B").isEmpty());
    assertEquals(HandSubmissionOutcome.ACCEPTED_RESULT_READY,
        service.submitHand("B", second).outcome());
    assertEquals(new ResultSnapshot(Hand.fromValue(first).orElseThrow(),
        Hand.fromValue(second).orElseThrow(), a), service.getResult("A").orElseThrow());
    assertEquals(b, service.getResult("B").orElseThrow().result());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"invalid", "ROCK", " rock "})
  void invalidHandDoesNotConfirm(String input) {
    startMatch();
    assertEquals(HandSubmissionOutcome.INVALID_HAND, service.submitHand("A", input).outcome());
    assertNull(service.getPlayState("A").myHand());
    assertEquals(PlayerStatus.PLAYING, service.checkStatus("A"));
  }

  @Test
  void duplicateSubmissionAndHomeAccessKeepConfirmedHand() {
    startMatch();
    service.submitHand("A", "rock");
    assertEquals(HomeState.PLAYING, service.joinOrGetHomeState("A"));
    assertEquals(HandSubmissionOutcome.ALREADY_CONFIRMED_WAITING,
        service.submitHand("A", "paper").outcome());
    assertEquals(Hand.ROCK, service.getPlayState("A").myHand());
    service.submitHand("B", "scissors");
    assertEquals(HandSubmissionOutcome.ALREADY_CONFIRMED_RESULT_READY,
        service.submitHand("A", null).outcome());
    assertEquals(GameResult.WIN, service.getResult("A").orElseThrow().result());
  }

  @Test
  void unregisteredAccessDoesNotJoinOrChangeMatch() {
    assertEquals(PlayerStatus.WAITING, service.checkStatus("unknown"));
    assertEquals(PlayerStatus.WAITING, service.getPlayState("unknown").status());
    assertTrue(service.getResult("unknown").isEmpty());
    service.confirmResult("unknown");
    assertEquals(HandSubmissionOutcome.NOT_STARTED, service.submitHand("unknown", "rock").outcome());
    assertEquals(HomeState.WAITING, service.joinOrGetHomeState("A"));
    assertEquals(HomeState.WAITING, service.joinOrGetHomeState("A"));
    assertEquals(HandSubmissionOutcome.NOT_STARTED, service.submitHand("A", "rock").outcome());
    assertNull(service.getPlayState("A").myHand());
  }
}
