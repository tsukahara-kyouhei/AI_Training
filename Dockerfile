# 1. ビルド用ステージ（Java 25とMavenを使ってアプリを組み立てる）
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# 必要なファイルだけをコンテナにコピー
# まずpom.xml（設計図）だけをコピー
COPY pom.xml .
# 依存するライブラリを先に一括ダウンロード（pom.xmlに変更がない限りキャッシュされる）
RUN mvn dependency:go-offline

# その後でソースコードをコピーしてビルド
COPY src ./src
RUN mvn clean package -DskipTests

# 2. 実行用ステージ（組み立てたファイルだけを動かす軽量な環境）
FROM eclipse-temurin:25-jdk
WORKDIR /app
# ビルド用ステージで作ったjarファイルをコピー
COPY --from=build /app/target/*.jar app.jar
# ポート8080番を開放
EXPOSE 8080
# アプリを起動
ENTRYPOINT ["java", "-jar", "app.jar"]