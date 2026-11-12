## 1. 機能概要

- OFFICE ORDER の紹介や利用ガイドを確認できる固定ページを提供する。
- 利用規約、プライバシーポリシー、特定商取引法に基づく表記を確認できるようにする。

## 2. 入出力項目

| 項目区分 (入力/出力) | 項目名 | 説明 |
|---|---|---|
| 入力 | HTTP GET リクエスト `/about` | OFFICE ORDER の紹介ページを要求する。 |
| 入力 | HTTP GET リクエスト `/guide` | ご利用ガイドページを要求する。 |
| 入力 | HTTP GET リクエスト `/legal/terms` | 利用規約ページを要求する。 |
| 入力 | HTTP GET リクエスト `/legal/privacy` | プライバシーポリシーページを要求する。 |
| 入力 | HTTP GET リクエスト `/legal/tokusho` | 特定商取引法に基づく表記ページを要求する。 |
| 出力 | 遷移先 `pages/about` | OFFICE ORDER の紹介画面。 |
| 出力 | 遷移先 `pages/guide` | ご利用ガイド画面。 |
| 出力 | 遷移先 `pages/legal-terms` | 利用規約画面。 |
| 出力 | 遷移先 `pages/legal-privacy` | プライバシーポリシー画面。 |
| 出力 | 遷移先 `pages/legal-tokusho` | 特定商取引法に基づく表記画面。 |
