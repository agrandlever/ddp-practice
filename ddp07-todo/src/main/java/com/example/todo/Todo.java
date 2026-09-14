package com.example.todo;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "todos")
public class Todo {

    @Id
    // IDENTITYは、IDの自動採番をDBに任せる指定。
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    // TEXT型で保存する。500文字以内の入力検証はサーバー側で行う。
    @Column(columnDefinition = "TEXT")
    private String description;

    // Javaでの初期値と、テーブル生成時のDBのデフォルトをそろえる。
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean completed = false;

    @CreationTimestamp
    // 作成日時は登録時に設定し、更新SQLの対象から除外する。
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // DBのデフォルトだけでは更新されないため、Hibernateで自動設定する。
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // JPAがDBの行からオブジェクトを生成するために必要。
    public Todo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
