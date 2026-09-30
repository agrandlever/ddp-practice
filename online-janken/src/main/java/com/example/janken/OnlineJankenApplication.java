// このクラスが所属するパッケージを指定する。
// フォルダー構造 src/main/java/com/example/janken と対応している。
package com.example.janken;

// Spring Bootアプリケーションを起動するためのクラス。
// SpringApplication.run(...)を利用するためにimportする。
import org.springframework.boot.SpringApplication;

// このクラスをSpring Bootアプリケーションの起点として扱うためのアノテーション。
import org.springframework.boot.autoconfigure.SpringBootApplication;

// このアノテーションを付けたクラスをSpring Bootアプリケーションの入口にする。
// また、このパッケージ以下にあるControllerやServiceなども
// Springが自動的に探す対象になる。
@SpringBootApplication
public class OnlineJankenApplication {

  // Javaプログラムが最初に実行するmainメソッド。
  public static void main(String[] args) {

    // Spring Bootを起動する。
    // OnlineJankenApplication.classを起点として、
    // WebサーバーやSpringの各機能を初期化する。
    SpringApplication.run(OnlineJankenApplication.class, args);
  }
}
