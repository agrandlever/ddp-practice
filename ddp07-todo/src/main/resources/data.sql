-- DDP07の設計書で定義した初期データ。
-- この順番で登録すると、ID降順の一覧では
-- 「部屋の掃除」→「買い物に行く」→「Spring Bootを学ぶ」
-- の順に表示される。

INSERT INTO todos (title, description, completed)
VALUES ('Spring Bootを学ぶ', '公式ドキュメントを読む', false);

INSERT INTO todos (title, description, completed)
VALUES ('買い物に行く', '牛乳とパン', false);

INSERT INTO todos (title, description, completed)
VALUES ('部屋の掃除', NULL, true);
