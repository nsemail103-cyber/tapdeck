# TapDeck

スマートフォンのブラウザを、PCのマクロパッド（Stream Deck風）として使うための個人開発アプリです。
PC側で小さなサーバーを動かし、スマホのグリッドをタップすると、PCでショートカットキーの実行、アプリ起動、定型文の貼り付けなどができます。

A personal macro pad: run the small server on your PC, open the grid on your phone, and taps fire hotkeys, app launches and text snippets on the PC.

## 構成

- **サーバー（PC側）**: Java 21 + Spring Boot。デッキ定義の保持、WebSocketでの配信、キー操作の実行を担当します。
- **クライアント（スマホ側）**: PWA（ブラウザで開いてホーム画面に追加）。アプリのインストールは不要です。
- ペアリングはPC画面に表示されるQRコード＋ワンタイムコードで行い、セッショントークンを発行します。

## できること

- キーに割り当てられるアクション（7種）
  - ホットキー（例: Ctrl+S、Win+Shift+S）
  - 定型文の貼り付け（貼り付け後にクリップボードを復元）
  - アプリ起動（`%LOCALAPPDATA%` などの環境変数展開に対応）
  - URL・ファイル・フォルダを開く
  - メディアキー（再生/停止、音量、ミュートなど）
  - 待機（ディレイ）
  - マルチ（上記の組み合わせを順に実行）
- 複数ページのデッキ（メイン／勉強／定型文／Web など）
- PC上の編集画面（`/editor`）でキーを編集すると、スマホへ即時反映されます
- デッキ定義はJSONファイルに保存され、エクスポート/インポートできます（`examples/custom-deck.json` は実際に使っている配置の例）

## 動かし方

前提: JDK 21。

```bash
cd server
mvn package -DskipTests
java -jar target/stream-deck-server-0.1.0-SNAPSHOT.jar
```

起動後、PCのブラウザで `http://localhost:8081/pair` を開き、表示されるQRコードをスマホで読み取ってペアリングします。
スマホとPCは同じLAN（Wi-Fi）に接続してください。

主な環境変数:

| 変数 | 意味 | 既定値 |
| --- | --- | --- |
| `DECK_TOKEN` | 静的アクセストークン（ペアリングの代わりに使えます） | `dev-token` |
| `DECK_DATA_DIR` | デッキ定義や認証情報の保存先 | `./data` |
| `DECK_EXECUTOR` | `robot`（実機操作） / `mock`（送信内容の記録のみ） | `robot` |

## セキュリティ上の注意

- このアプリは「自宅LANの中で自分のPCを操作する」前提で設計しています。**ポートをインターネットに公開しないでください。** スマホからのキー操作がそのままPCで実行されます。
- 編集画面やペアリング用APIは、PC本体（localhost）からのアクセスのみに制限しています。
- 公開ネットワークや共有PCでの使用は想定していません。

## 開発

```bash
cd server
mvn test
```

## ライセンス

MIT License（[LICENSE](LICENSE) 参照）。

依存ライブラリ（Spring Boot、ZXing など）のライセンスは各プロジェクトに従います。
同梱配布用の実行環境として Eclipse Temurin（OpenJDK）を別途パッケージ化する場合、そのライセンス（GPLv2 + Classpath Exception）も併せて確認してください。
