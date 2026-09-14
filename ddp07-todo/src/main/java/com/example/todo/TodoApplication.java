package com.example.todo;

// Spring Bootアプリケーションを起動するためのクラスを読み込む。
import org.springframework.boot.SpringApplication;

// Spring Bootの設定・自動構成・コンポーネント探索を有効にする。
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Todoアプリの起動クラス。
 */
@SpringBootApplication
public class TodoApplication {

  /**
   * Javaプログラムの開始地点。
   */
  public static void main(String[] args) {
    // Spring Bootアプリケーションを起動する。
    SpringApplication.run(TodoApplication.class, args);
  }
}
