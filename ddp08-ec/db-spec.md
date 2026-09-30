# DB設計書：ECサイト商品管理

## 1. 基本情報

### 1.1 使用データベース

H2 DatabaseのインメモリDBを使用する。

アプリケーションを終了した場合、保存された商品データが失われることを許容する。

### 1.2 テーブル一覧

| テーブル名 | 論理名 | 用途                         |
| ---------- | ------ | ---------------------------- |
| `products` | 商品   | ECサイトの商品情報を管理する |

今回の基本演習では、カテゴリを別テーブルへ分離しない。

---

# 2. productsテーブル

## 2.1 テーブル定義

| カラム名      | 論理名   | データ型       | NULL | デフォルト値 | PK  | 説明                            |
| ------------- | -------- | -------------- | ---- | ------------ | --- | ------------------------------- |
| `id`          | 商品ID   | `BIGINT`       | 不可 | 自動採番     | ○   | 商品を一意に識別する            |
| `name`        | 商品名   | `VARCHAR(200)` | 不可 | なし         | -   | 1～200文字の商品名              |
| `price`       | 価格     | `INTEGER`      | 不可 | なし         | -   | 0～9,999,999円                  |
| `description` | 説明     | `TEXT`         | 可   | `NULL`       | -   | 商品説明                        |
| `category`    | カテゴリ | `VARCHAR(50)`  | 不可 | なし         | -   | 定義済み5カテゴリのいずれか     |
| `on_sale`     | 販売状態 | `BOOLEAN`      | 不可 | `TRUE`       | -   | `TRUE`=販売中、`FALSE`=販売停止 |
| `created_at`  | 登録日時 | `TIMESTAMP`    | 不可 | なし         | -   | 商品を登録した日時              |
| `updated_at`  | 更新日時 | `TIMESTAMP`    | 不可 | なし         | -   | 最後に商品を更新した日時        |

---

## 2.2 主キー

`id`を主キーとする。

`id`は自動採番とし、新しい商品を登録する際にアプリケーション側でIDを指定しない。

---

# 3. カラム詳細

## 3.1 name

商品名を保存する。

条件：

- 必須
- 1～200文字
- 空文字・空白のみは不可

Java側では次のバリデーションを使用する。

```java
@NotBlank
@Size(max = 200)
```

Java型：

`String`

---

## 3.2 price

商品の価格を整数で保存する。

条件：

- 必須
- 0以上
- 9,999,999以下
- 小数は扱わない

Java側では次のバリデーションを使用する。

```java
@NotNull
@Min(0)
@Max(9999999)
```

Java型：

`Integer`

---

## 3.3 description

商品の説明を保存する。

条件：

- 任意
- 値が存在しない場合は`NULL`を許可する

Java型：

`String`

---

## 3.4 category

商品カテゴリを保存する。

使用できる値：

- 食品
- 日用品
- 家電
- 書籍
- その他

条件：

- 必須
- 上記5種類以外は登録しない

Java型：

`String`

---

## 3.5 on_sale

商品の販売状態を保存する。

| DB値    | 意味     |
| ------- | -------- |
| `TRUE`  | 販売中   |
| `FALSE` | 販売停止 |

新規登録時の初期値は`TRUE`とする。

Java型：

`Boolean`

---

## 3.6 created_at

商品を最初に登録した日時を保存する。

商品を編集しても変更しない。

Java型：

`LocalDateTime`

---

## 3.7 updated_at

商品を最後に更新した日時を保存する。

商品登録時にも現在日時を設定し、その後は商品編集時に更新する。

Java型：

`LocalDateTime`

---

# 4. ER図

今回使用するテーブルは`products`だけであり、他テーブルとのリレーションはない。

```text
┌─────────────────────────┐
│        products         │
├─────────────────────────┤
│ PK id           BIGINT  │
│    name         VARCHAR │
│    price        INTEGER │
│    description  TEXT    │
│    category     VARCHAR │
│    on_sale      BOOLEAN │
│    created_at  TIMESTAMP│
│    updated_at  TIMESTAMP│
└─────────────────────────┘
```

外部キーは使用しない。

---

# 5. Entityとの対応

| DBカラム      | Javaフィールド | Java型          |
| ------------- | -------------- | --------------- |
| `id`          | `id`           | `Long`          |
| `name`        | `name`         | `String`        |
| `price`       | `price`        | `Integer`       |
| `description` | `description`  | `String`        |
| `category`    | `category`     | `String`        |
| `on_sale`     | `onSale`       | `Boolean`       |
| `created_at`  | `createdAt`    | `LocalDateTime` |
| `updated_at`  | `updatedAt`    | `LocalDateTime` |

---

# 6. 初期データ

動作確認用として、5件以上の異なる商品データを用意する。

カテゴリフィルタ、販売状態、NULL許容などを確認できるように、異なる状態を含める。

| No. | 商品名             | 価格 | 説明                       | カテゴリ | 販売状態 |
| --: | ------------------ | ---: | -------------------------- | -------- | -------- |
|   1 | ミネラルウォーター |  120 | 500mlのミネラルウォーター  | 食品     | 販売中   |
|   2 | 洗濯用洗剤         |  498 | 液体タイプの洗濯用洗剤     | 日用品   | 販売中   |
|   3 | ワイヤレスマウス   | 3980 | USB接続のワイヤレスマウス  | 家電     | 販売中   |
|   4 | Java入門書         | 2800 | Javaの基礎を学ぶための書籍 | 書籍     | 販売停止 |
|   5 | ギフトバッグ       |  300 | `NULL`                     | その他   | 販売中   |

この5件によって、少なくとも次を確認できる。

- 5種類すべてのカテゴリ
- 販売中の商品
- 販売停止の商品
- 説明が存在する商品
- 説明が`NULL`の商品
- 複数桁の価格

---

# 7. 初期データSQL

`id`は自動採番のため、INSERT文では指定しない。

日時は初期データ投入時点の値を設定する。

```sql
INSERT INTO products
    (name, price, description, category, on_sale, created_at, updated_at)
VALUES
    ('ミネラルウォーター', 120, '500mlのミネラルウォーター', '食品', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products
    (name, price, description, category, on_sale, created_at, updated_at)
VALUES
    ('洗濯用洗剤', 498, '液体タイプの洗濯用洗剤', '日用品', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products
    (name, price, description, category, on_sale, created_at, updated_at)
VALUES
    ('ワイヤレスマウス', 3980, 'USB接続のワイヤレスマウス', '家電', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products
    (name, price, description, category, on_sale, created_at, updated_at)
VALUES
    ('Java入門書', 2800, 'Javaの基礎を学ぶための書籍', '書籍', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO products
    (name, price, description, category, on_sale, created_at, updated_at)
VALUES
    ('ギフトバッグ', 300, NULL, 'その他', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

---

# 8. 設計上の注意

- `id`は自動採番なので、通常のINSERTでは値を指定しない。
- `description`だけは`NULL`を許可する。
- カテゴリは基本演習では文字列として`products`へ直接保存する。
- `price`の0～9,999,999という制約はJava側のバリデーションでも確認する。
- `created_at`は商品編集時に変更しない。
- `updated_at`は商品編集時に更新する。
