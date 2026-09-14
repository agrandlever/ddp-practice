package com.example.todo;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public List<Todo> findAll() {
        return todoRepository.findAllByOrderByIdDesc();
    }

    public Optional<Todo> findById(Long id) {
        // Optional.empty()は対象なしを表す。DBの取得失敗は例外のまま呼び出し元へ伝える。
        return todoRepository.findById(id);
    }

    @Transactional
    public Todo create(TodoForm form) {
        Todo todo = new Todo();
        applyForm(todo, form);
        todo.setCompleted(false);
        return todoRepository.save(todo);
    }

    @Transactional
    public Optional<Todo> update(Long id, TodoForm form) {
        return todoRepository.findById(id).map(todo -> {
            // 取得したタスクの入力項目だけを変更し、ID・完了状態・作成日時を維持する。
            applyForm(todo, form);
            return todoRepository.save(todo);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<Todo> todo = todoRepository.findById(id);
        if (todo.isEmpty()) {
            return false;
        }
        todoRepository.delete(todo.get());
        return true;
    }

    @Transactional
    public Optional<Todo> toggleComplete(Long id) {
        return todoRepository.findById(id).map(todo -> {
            // 業務項目は完了状態だけを変更する。更新日時はEntityの指定で自動更新される。
            todo.setCompleted(!todo.isCompleted());
            return todoRepository.save(todo);
        });
    }

    private void applyForm(Todo todo, TodoForm form) {
        todo.setTitle(form.getTitle().strip());
        String description = form.getDescription();
        // 説明の空白や改行は保持し、未入力・空文字だけをNULLに統一する。
        todo.setDescription(description == null || description.isEmpty() ? null : description);
    }
}
