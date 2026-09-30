(() => {
    "use strict";

    const pendingKey = "opponentDisconnectedMessagePending";
    const errorMessage = "通信エラーが発生しました。ページを再読み込みしてください。";
    const errorElement = document.getElementById("communication-error");

    function showError(message) {
        // textContentは受け取った文言をHTMLとして解釈せず、文字として表示する。
        errorElement.textContent = message;
        errorElement.hidden = false;
    }

    // sessionStorageは、同じタブの画面遷移をまたいで通知だけを引き継ぐために使う。
    // 手・勝敗・参加人数などの対戦状態は保存しない。
    try {
        if (sessionStorage.getItem(pendingKey) === "true") {
            document.getElementById("opponent-disconnected-message").hidden = false;
            // 表示直後に削除し、再読み込み時に同じ切断通知が再表示されるのを防ぐ。
            sessionStorage.removeItem(pendingKey);
        }
    } catch {
        // ブラウザーの設定で保存領域を使用できない場合も、画面上で通知する。
        showError(errorMessage);
    }

    // これは通信の重複を防ぐ印であり、対戦状態を管理するものではない。
    let requestInProgress = false;

    async function checkStatus() {
        if (requestInProgress) {
            return;
        }
        requestInProgress = true;

        try {
            // fetchはHTTP通信でサーバーへ問い合わせる標準機能。GETで状態を取得する。
            const response = await fetch("/api/status");
            const data = await response.json();

            if (!response.ok) {
                showError(response.status === 500 && data.errorCode === "INTERNAL_ERROR"
                    && typeof data.message === "string" ? data.message : errorMessage);
                return;
            }

            switch (data.status) {
                case "PLAYING":
                    clearInterval(pollingTimer);
                    // window.location.hrefへの代入で、指定URLの画面へ移動する。
                    window.location.href = "/play";
                    break;
                case "OPPONENT_DISCONNECTED":
                    // 遷移先で一度だけ表示するため、移動前に通知の印を保存する。
                    sessionStorage.setItem(pendingKey, "true");
                    clearInterval(pollingTimer);
                    window.location.href = "/";
                    break;
                case "WAITING":
                    break;
                default:
                    // この画面で遷移が定義されていないstatusでは何もしない。
                    break;
            }
        } catch {
            // 通信失敗時も別画面へ移動せず、現在の画面にMSG-003を表示する。
            showError(errorMessage);
        } finally {
            requestInProgress = false;
        }
    }

    // setIntervalは処理を一定間隔で繰り返す。2000ms（2秒）ごとの状態確認を
    // ポーリングと呼ぶ。前の通信が続いている場合は重複した送信を避ける。
    const pollingTimer = setInterval(checkStatus, 2000);
})();
