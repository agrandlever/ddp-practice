(() => {
    "use strict";

    const pendingKey = "opponentDisconnectedMessagePending";
    const errorMessage = "通信エラーが発生しました。ページを再読み込みしてください。";
    const errorElement = document.getElementById("communication-error");
    // 通信の重複だけを防ぐ。手や対戦状態の判断はGameServiceに任せる。
    let requestInProgress = false;

    function showError(message) {
        // JSONの文言をHTMLとして実行せず、そのまま文字として表示する。
        errorElement.textContent = message;
        errorElement.hidden = false;
    }

    async function checkStatus() {
        if (requestInProgress) {
            return;
        }
        requestInProgress = true;

        try {
            // fetchはHTTP通信を行う標準機能。GETでサーバーの最新状態を取得する。
            const response = await fetch("/api/status");
            const data = await response.json();

            if (!response.ok) {
                showError(response.status === 500 && data.errorCode === "INTERNAL_ERROR"
                    && typeof data.message === "string" ? data.message : errorMessage);
                return;
            }

            switch (data.status) {
                case "RESULT_READY":
                    clearInterval(pollingTimer);
                    // window.location.hrefへURLを代入すると、その画面へ遷移する。
                    window.location.href = "/result";
                    break;
                case "OPPONENT_DISCONNECTED":
                    // sessionStorageは同じタブの画面遷移をまたぐ通知の引き継ぎ専用。
                    // index.jsが表示直後に削除するので、再読み込みで再表示されない。
                    sessionStorage.setItem(pendingKey, "true");
                    clearInterval(pollingTimer);
                    window.location.href = "/";
                    break;
                case "WAITING_FOR_OPPONENT":
                    break;
                default:
                    // 設計書でこの画面の遷移が定義されていないstatusでは留まる。
                    break;
            }
        } catch {
            // 通信そのものが失敗した場合も、現在の画面でMSG-003を表示する。
            showError(errorMessage);
        } finally {
            requestInProgress = false;
        }
    }

    // setIntervalで2000ms（2秒）ごとに状態確認を繰り返す（ポーリング）。
    // 応答が遅いときはcheckStatus内で重複送信を防ぐ。
    const pollingTimer = setInterval(checkStatus, 2000);
})();
