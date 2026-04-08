package com.puchain.fep.web.dashboard.todo.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.dashboard.todo.domain.DashboardTodo;
import com.puchain.fep.web.dashboard.todo.domain.TodoPriority;
import com.puchain.fep.web.dashboard.todo.domain.TodoStatus;
import com.puchain.fep.web.dashboard.todo.dto.TodoCreateRequest;
import com.puchain.fep.web.dashboard.todo.dto.TodoResponse;
import com.puchain.fep.web.dashboard.todo.repository.DashboardTodoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DashboardTodoService 单元测试。
 *
 * <p>覆盖创建、完成、状态校验、不存在等核心业务场景。</p>
 */
@ExtendWith(MockitoExtension.class)
class DashboardTodoServiceTest {

    @Mock
    private DashboardTodoRepository todoRepository;

    @InjectMocks
    private DashboardTodoService todoService;

    @Test
    void create_withValidRequest_shouldReturnTodoWithPendingStatus() {
        TodoCreateRequest request = new TodoCreateRequest();
        request.setTitle("审核融资申请 #12345");
        request.setTaskType("FINANCING_APPLICATION");
        request.setPriority(TodoPriority.HIGH);

        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TodoResponse resp = todoService.create(request, "user-001");

        assertThat(resp.getTitle()).isEqualTo("审核融资申请 #12345");
        assertThat(resp.getTodoStatus()).isEqualTo(TodoStatus.PENDING);
        assertThat(resp.getPriority()).isEqualTo(TodoPriority.HIGH);
        assertThat(resp.getAssignedUserId()).isEqualTo("user-001");
    }

    @Test
    void complete_withPendingTodo_shouldTransitionToCompleted() {
        DashboardTodo todo = new DashboardTodo();
        todo.setTodoId("todo-001");
        todo.setTitle("测试待办");
        todo.setTaskType("TEST");
        todo.setPriority(TodoPriority.MEDIUM);
        todo.setTodoStatus(TodoStatus.PENDING);
        when(todoRepository.findById("todo-001")).thenReturn(Optional.of(todo));
        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TodoResponse resp = todoService.complete("todo-001");

        assertThat(resp.getTodoStatus()).isEqualTo(TodoStatus.COMPLETED);
        assertThat(resp.getCompletedTime()).isNotNull();
    }

    @Test
    void complete_withCompletedTodo_shouldThrowBiz5003() {
        DashboardTodo todo = new DashboardTodo();
        todo.setTodoId("todo-002");
        todo.setTitle("已完成待办");
        todo.setTaskType("TEST");
        todo.setPriority(TodoPriority.LOW);
        todo.setTodoStatus(TodoStatus.COMPLETED);
        when(todoRepository.findById("todo-002")).thenReturn(Optional.of(todo));

        assertThatThrownBy(() -> todoService.complete("todo-002"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5003));
    }

    @Test
    void complete_withNonExistentId_shouldThrowBiz5001() {
        when(todoRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.complete("non-existent"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5001));
    }
}
