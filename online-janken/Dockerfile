# ========================================
# 1段階目：Spring Bootアプリをビルドする
# ========================================
# Java 21のJDKを使用する。
# JDKはJavaプログラムのコンパイルに必要。
FROM eclipse-temurin:21-jdk AS build

# Dockerコンテナ内の作業場所を/appにする。
WORKDIR /app

# プロジェクト全体をコンテナ内へコピーする。
COPY . .

# Windowsではgradlew.batを使うが、
# RenderのDocker環境はLinuxなのでgradlewを使用する。
# chmod +xはgradlewに実行権限を付ける処理。
RUN chmod +x ./gradlew

# Spring Bootの実行可能JARを作成する。
RUN ./gradlew clean bootJar --no-daemon

# build/libsに作られたSpring BootのJARを
# 名前を固定して/app/app.jarへコピーする。
RUN cp "$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" app.jar


# ========================================
# 2段階目：実際にアプリを動かす
# ========================================
# 実行だけなので、JDKより軽量なJREを使用する。
FROM eclipse-temurin:21-jre

WORKDIR /app

# 1段階目で作成したJARだけをコピーする。
COPY --from=build /app/app.jar app.jar

# Renderで使用するHTTPポート。
EXPOSE 10000

# RenderのPORT環境変数を使用する。
# PORTが設定されていなければ10000を使用する。
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-10000} -jar app.jar"]
