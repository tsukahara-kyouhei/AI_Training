# Db2 Community Edition ローカル開発環境の導入提案

## 1. 導入・目的

開発者全員のローカルPCで手軽に利用できる、無料のDb2開発環境として、IBM Db2 Community Editionのコンテナイメージを採用することを提案します。

本書は、公開されているIBMの情報をもとに、採用候補のイメージとバージョン、利用条件、確認が必要な事項を整理し、DBチームで合意するための提案です。調査日は2026年11月17日です。

## 2. 推奨するコンテナイメージとバージョン

| 項目 | 提案 |
| --- | --- |
| イメージ | `icr.io/db2_community/db2` |
| バージョン | `12.1.5.0` |
| 使用するタグ | `icr.io/db2_community/db2:12.1.5.0` |

IBMはDb2 Community EditionのDockerイメージの配布先をIBM Container Registry（ICR）に移したと案内しています。調査時点でICRのタグ一覧にある最新の番号付きタグは`12.1.5.0`で、`latest`タグのイメージ情報からもDb2 12.1.5を確認しました。開発者間で同じ版を利用するため、更新で実体が変わり得る`latest`ではなく、番号付きタグを指定する案です。

IBMのドキュメントには12.1.6イメージへの言及がありますが、確認したICRタグ一覧には`12.1.6.0`がありませんでした。このため、本提案では調査時点でタグ一覧に存在する最新の番号付き版を推奨します。IBMが12.1.5.0を「安定版」と明記しているという意味ではありません。

## 3. ライセンスおよび利用上の注意

- IBMはCommunity Editionを無料のエントリー向けエディションとして案内しています。
- コンテナ版は**非本番用途**です。IBMサポートの対象外であり、Community Editionライセンスからのアップグレードもサポートされません。
- 起動時には`LICENSE=accept`を設定し、ライセンス条項への同意を示す必要があります。利用開始前に、IBMが案内するライセンス条項を確認してください。
- IBMの起動例では、`--privileged=true`、DB接続ポート`50000`の公開、およびデータ保存先`/database`の永続化が使われています。Rancher Desktopでの実際の起動可否や設定は、次の検証ステップで確認します。

## 4. ハードウェア・リソース要件に関する注意

- Community Editionのリソース上限は**CPU 4コア、メモリ16 GB**です。これは上限であり、最低要件を示す数値ではありません。
- 調査したIBMの公開情報からは、コンテナ起動に必要な**最低CPU数・最低メモリ量は確認できませんでした（不明）**。上限値を最低要件として扱わないでください。
- 調査時点で取得した`latest`イメージのレジストリ情報は`amd64`を示しています。ARM64環境でのネイティブ対応は確認できていません。

## 5. 次のステップ

1. DBチームで、イメージ・バージョンと「非本番のローカル開発用途」という利用範囲に合意する。
2. 各開発者のRancher Desktop環境で起動し、接続、初回セットアップ、データ永続化、停止後の再起動を検証する。最低リソース要件は不明のため、動作に必要な割当量もこの検証で確認する。
3. 検証結果を踏まえ、チーム共通のCompose設定へ反映する。本リポジトリにはすでに`docker-compose.yml`があるため、新規に同名ファイルを作るのではなく、Db2サービスの追加または既存DBサービスとの構成変更として扱う。
4. 既存のPostgreSQLサービスを維持するか、Db2へ置き換えるかは別途合意してから決定する。

## 6. 参照情報

いずれも2026年11月17日に確認しました。

- [IBM Docs: Db2 Community Edition for Docker](https://www.ibm.com/docs/en/db2/12.1.x?topic=deployments-db2-community-edition-docker)
- [IBM Container Registry: Db2 Community Editionのタグ一覧](https://icr.io/v2/db2_community/db2/tags/list)
- [IBM Container Registry: `latest`イメージの情報](https://icr.io/v2/db2_community/db2/manifests/latest)
- [IBM Db2 Databaseの価格・エディション情報](https://www.ibm.com/products/db2-database/pricing)
- [IBM Docs: Community Editionコンテナの構成](https://www.ibm.com/docs/en/db2/12.1.x?topic=system-configuring-db2-community-edition-docker)
- [IBMのDocker Hubページ（ICRへの移行案内、起動例）](https://hub.docker.com/r/ibmcom/db2)
