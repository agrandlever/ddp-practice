package com.example.ddp08ec.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProductForm {

    @NotBlank(message = "商品名を入力してください。")
    @Size(max = 200, message = "商品名は200文字以内で入力してください。")
    private String name;

    @NotNull(message = "価格を入力してください。")
    @Min(value = 0, message = "価格は0～9,999,999の整数で入力してください。")
    @Max(value = 9999999, message = "価格は0～9,999,999の整数で入力してください。")
    private Integer price;

    private String description;

    @NotBlank(message = "カテゴリを選択してください。")
    @Pattern(regexp = "食品|日用品|家電|書籍|その他", message = "指定されたカテゴリは使用できません。")
    private String category;

    // 未送信を検出するため、初期値は新規画面を表示するControllerで設定する。
    @NotNull(message = "販売状態を選択してください。")
    private Boolean onSale;

    public ProductForm() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getOnSale() {
        return onSale;
    }

    public void setOnSale(Boolean onSale) {
        this.onSale = onSale;
    }
}
