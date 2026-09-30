package com.example.ddp08ec.controller;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.ddp08ec.entity.Product;
import com.example.ddp08ec.form.ProductForm;
import com.example.ddp08ec.service.ProductService;

@Controller
@RequestMapping("/products")
public class ProductController {

    private static final List<String> CATEGORIES = List.of("食品", "日用品", "家電", "書籍", "その他");

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(name = "category", required = false) String category, Model model) {
        List<Product> products;
        if (category == null || category.isEmpty()) {
            products = productService.findAll();
        } else if (CATEGORIES.contains(category)) {
            products = productService.findByCategory(category);
        } else {
            // 不正な値で絞り込まず、全商品を表示して選択状態を「すべて」に戻す。
            products = productService.findAll();
            category = "";
            model.addAttribute("errorMessage", "指定されたカテゴリは使用できません。");
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        return "products/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        ProductForm productForm = new ProductForm();
        productForm.setOnSale(Boolean.TRUE);
        model.addAttribute("productForm", productForm);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("isNew", true);
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("productForm") ProductForm productForm,
            BindingResult bindingResult, Model model) {
        // BindingResultをフォームの直後に置き、入力値と検証エラーを保持する。
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", CATEGORIES);
            model.addAttribute("isNew", true);
            return "products/form";
        }

        productService.create(productForm);
        return "redirect:/products";
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Product> product = productService.findById(id);
        if (product.isEmpty()) {
            return redirectNotFound(redirectAttributes);
        }

        model.addAttribute("product", product.get());
        return "products/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Product> existingProduct = productService.findById(id);
        if (existingProduct.isEmpty()) {
            return redirectNotFound(redirectAttributes);
        }

        Product product = existingProduct.get();
        ProductForm productForm = new ProductForm();
        productForm.setName(product.getName());
        productForm.setPrice(product.getPrice());
        productForm.setDescription(product.getDescription());
        productForm.setCategory(product.getCategory());
        productForm.setOnSale(product.getOnSale());
        model.addAttribute("productForm", productForm);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("isNew", false);
        model.addAttribute("productId", id);
        return "products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
            @Valid @ModelAttribute("productForm") ProductForm productForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        // 入力エラーがあっても、対象が存在しなければ一覧へ戻す。
        if (productService.findById(id).isEmpty()) {
            return redirectNotFound(redirectAttributes);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", CATEGORIES);
            model.addAttribute("isNew", false);
            model.addAttribute("productId", id);
            return "products/form";
        }

        if (productService.update(id, productForm).isEmpty()) {
            return redirectNotFound(redirectAttributes);
        }
        return "redirect:/products/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (!productService.delete(id)) {
            return redirectNotFound(redirectAttributes);
        }
        return "redirect:/products";
    }

    private String redirectNotFound(RedirectAttributes redirectAttributes) {
        // Flash属性はリダイレクト先のリクエストへ一度だけ引き継がれる。
        redirectAttributes.addFlashAttribute("errorMessage", "商品が見つかりません");
        return "redirect:/products";
    }
}
