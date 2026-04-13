import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';

/**
 * Todo priority levels exposed by backend DashboardTodoController.
 */
export type TodoPriority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';

/**
 * Todo lifecycle states.
 */
export type TodoStatus = 'PENDING' | 'IN_PROCESS' | 'COMPLETED';

/**
 * Single todo entry returned from the backend.
 *
 * Aligned with com.puchain.fep.web.dashboard.dto.DashboardTodoResponse.
 */
export interface TodoResponse {
  todoId: string;
  taskType: string;
  title: string;
  priority: TodoPriority;
  todoStatus: TodoStatus;
  targetUrl: string;
  assignedUserId: string;
  deadline: string | null;
  completedTime: string | null;
  createTime: string;
  updateTime: string;
}

/**
 * Payload for creating a new todo.
 */
export interface TodoCreateRequest {
  title: string;
  taskType: string;
  priority: TodoPriority;
  targetUrl: string;
  deadline?: string;
}

/**
 * Query parameters for paginated todo search.
 *
 * P0-1 fix (review finding): backend DashboardTodoController.search uses
 * @RequestParam pageNum / pageSize (not page/size), and only status is
 * exposed as optional filter (no priority). Keep the TS contract strictly
 * aligned with the server-side signature.
 */
export interface TodoSearchParams {
  pageNum: number;
  pageSize: number;
  status?: TodoStatus;
}

/**
 * API client for /api/v1/dashboard/todos endpoints.
 */
export const todoApi = {
  search: (params: TodoSearchParams): Promise<PageResult<TodoResponse>> =>
    httpClient.get('/api/v1/dashboard/todos', { params }),
  countPending: (): Promise<number> =>
    httpClient.get('/api/v1/dashboard/todos/count'),
  create: (req: TodoCreateRequest): Promise<TodoResponse> =>
    httpClient.post('/api/v1/dashboard/todos', req),
  startProcessing: (todoId: string): Promise<TodoResponse> =>
    httpClient.put(`/api/v1/dashboard/todos/${todoId}/process`),
  complete: (todoId: string): Promise<TodoResponse> =>
    httpClient.put(`/api/v1/dashboard/todos/${todoId}/complete`),
  delete: (todoId: string): Promise<void> =>
    httpClient.delete(`/api/v1/dashboard/todos/${todoId}`),
};
