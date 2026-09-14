package com.example.todo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // メソッド名からクエリが生成される。仕様のORDER BY id DESCで全件取得する。
    List<Todo> findAllByOrderByIdDesc();
}
