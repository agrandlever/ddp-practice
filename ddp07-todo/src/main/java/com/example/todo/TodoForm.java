package com.example.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TodoForm {

    @NotBlank(message = "タイトルは必須です")
    @Size(max = 100, message = "タイトルは100文字以内で入力してください")
    private String title;

    @Size(max = 500, message = "説明は500文字以内で入力してください")
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        // フォームの値が設定される時点で空白を除去し、@Validで除去後の長さを検証する。
        this.title = title == null ? null : title.strip();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
