package com.example.todo;

import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/todos")
public class TodoController {

    private static final String NOT_FOUND = "タスクが見つかりません。";
    private static final String READ_ERROR = "タスクを読み込めませんでした。もう一度お試しください。";
    private static final String SAVE_ERROR = "タスクを保存できませんでした。もう一度お試しください。";
    private static final String DELETE_ERROR = "タスクを削除できませんでした。もう一度お試しください。";
    private static final String TOGGLE_ERROR = "完了状態を変更できませんでした。もう一度お試しください。";

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("loadFailed", false);
        try {
            model.addAttribute("todos", todoService.findAll());
        } catch (DataAccessException | TransactionException ex) {
            // テンプレートはloadFailed時に一覧・0件の案内を隠し、再読み込みを案内する。
            model.addAttribute("loadFailed", true);
            model.addAttribute("errorMessage", READ_ERROR);
        }
        return "todos/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("todoForm", new TodoForm());
        prepareForm(model, null);
        return "todos/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("todoForm") TodoForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        // BindingResultは検証対象の直後に置く。同じフォームを返すことで入力とエラーを保持する。
        prepareForm(model, null);
        if (bindingResult.hasErrors()) {
            return "todos/form";
        }
        try {
            todoService.create(form);
        } catch (DataAccessException | TransactionException ex) {
            model.addAttribute("errorMessage", SAVE_ERROR);
            return "todos/form";
        }
        // Serviceのトランザクションが正常に確定した後でのみ成功メッセージを渡す。
        redirectAttributes.addFlashAttribute("successMessage", "タスクを登録しました。");
        return "redirect:/todos";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        model.addAttribute("loadFailed", false);
        try {
            Optional<Todo> todo = todoService.findById(id);
            if (todo.isEmpty()) {
                return redirectNotFound(redirectAttributes);
            }
            model.addAttribute("todo", todo.get());
        } catch (DataAccessException | TransactionException ex) {
            // テンプレートはloadFailed時にタスク内容・編集・削除ボタンを隠す。
            model.addAttribute("loadFailed", true);
            model.addAttribute("errorMessage", READ_ERROR);
        }
        return "todos/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        prepareForm(model, id);
        try {
            Optional<Todo> todo = todoService.findById(id);
            if (todo.isEmpty()) {
                return redirectNotFound(redirectAttributes);
            }
            TodoForm form = new TodoForm();
            form.setTitle(todo.get().getTitle());
            form.setDescription(todo.get().getDescription());
            model.addAttribute("todoForm", form);
        } catch (DataAccessException | TransactionException ex) {
            // 初期取得に失敗した場合はフォームを隠し、「一覧へ戻る」を表示する。
            model.addAttribute("loadFailed", true);
            model.addAttribute("errorMessage", READ_ERROR);
        }
        return "todos/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
            @Valid @ModelAttribute("todoForm") TodoForm form, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {
        prepareForm(model, id);
        if (bindingResult.hasErrors()) {
            return "todos/form";
        }
        try {
            if (todoService.update(id, form).isEmpty()) {
                return redirectNotFound(redirectAttributes);
            }
        } catch (DataAccessException | TransactionException ex) {
            // 再取得したDB値でフォームを上書きせず、送信された入力を残す。
            model.addAttribute("errorMessage", SAVE_ERROR);
            return "todos/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "タスクを更新しました。");
        return "redirect:/todos/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            if (!todoService.delete(id)) {
                return redirectNotFound(redirectAttributes);
            }
        } catch (DataAccessException | TransactionException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", DELETE_ERROR);
            return "redirect:/todos";
        }
        redirectAttributes.addFlashAttribute("successMessage", "タスクを削除しました。");
        return "redirect:/todos";
    }

    @PostMapping("/{id}/toggle")
    public String toggleComplete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Todo> todo = todoService.toggleComplete(id);
            if (todo.isEmpty()) {
                return redirectNotFound(redirectAttributes);
            }
            redirectAttributes.addFlashAttribute("successMessage", todo.get().isCompleted()
                    ? "タスクを完了にしました。" : "タスクを未完了に戻しました。");
        } catch (DataAccessException | TransactionException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", TOGGLE_ERROR);
        }
        return "redirect:/todos";
    }

    private String redirectNotFound(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", NOT_FOUND);
        return "redirect:/todos";
    }

    private void prepareForm(Model model, Long id) {
        // 共用テンプレートの表示情報。再表示時も対象ID・送信先・キャンセル先を維持する。
        boolean editing = id != null;
        model.addAttribute("editing", editing);
        model.addAttribute("todoId", id);
        model.addAttribute("pageTitle", editing ? "タスク編集" : "タスク登録");
        model.addAttribute("submitLabel", editing ? "保存する" : "登録する");
        model.addAttribute("formAction", editing ? "/todos/" + id : "/todos");
        model.addAttribute("cancelUrl", editing ? "/todos/" + id : "/todos");
        model.addAttribute("loadFailed", false);
    }
}
