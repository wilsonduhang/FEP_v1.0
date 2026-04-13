/**
 * 分页响应结构，对齐后端 com.puchain.fep.common.domain.PageResult。
 *
 * 参见 PRD v1.3 §9.2 列表与分页规范。
 */
export interface PageResult<T> {
  /** 当前页记录列表 */
  records: T[];
  /** 总记录数 */
  total: number;
  /** 当前页码（1-based） */
  pageNum: number;
  /** 每页大小 */
  pageSize: number;
  /** 总页数 */
  totalPages: number;
}
