import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import DataTable from '../DataTable.vue';
import type { DataTableColumn } from '../DataTable.vue';

const columns: DataTableColumn[] = [
  { prop: 'name', label: '名称' },
  { prop: 'status', label: '状态', width: 120 },
];

const data = [
  { name: '测试项目', status: 'ACTIVE' },
  { name: '另一个项目', status: 'DRAFT' },
];

describe('DataTable', () => {
  it('renders el-table and el-pagination components', () => {
    const wrapper = mount(DataTable, {
      props: { data, columns, total: 2, pageNum: 1, pageSize: 10 },
      global: { plugins: [ElementPlus] },
    });
    expect(wrapper.find('.el-table').exists()).toBe(true);
    expect(wrapper.find('.el-pagination').exists()).toBe(true);
  });

  it('passes data array to ElTable component', () => {
    const wrapper = mount(DataTable, {
      props: { data, columns, total: 2, pageNum: 1, pageSize: 10 },
      global: { plugins: [ElementPlus] },
    });
    const table = wrapper.findComponent({ name: 'ElTable' });
    expect(table.exists()).toBe(true);
    expect(table.props('data')).toEqual(data);
  });

  it('renders empty table when data is empty', () => {
    const wrapper = mount(DataTable, {
      props: { data: [], columns, total: 0, pageNum: 1, pageSize: 10 },
      global: { plugins: [ElementPlus] },
    });
    expect(wrapper.find('.el-table').exists()).toBe(true);
    // Empty state text from Element Plus
    expect(wrapper.find('.el-table__empty-text').exists()).toBe(true);
  });

  it('shows pagination with total count', () => {
    const wrapper = mount(DataTable, {
      props: { data, columns, total: 50, pageNum: 1, pageSize: 10 },
      global: { plugins: [ElementPlus] },
    });
    expect(wrapper.find('.el-pagination').exists()).toBe(true);
    expect(wrapper.text()).toContain('50');
  });

  it('emits update:pageNum when pagination current-page changes', async () => {
    const wrapper = mount(DataTable, {
      props: { data, columns, total: 50, pageNum: 1, pageSize: 10 },
      global: { plugins: [ElementPlus] },
    });
    const pagination = wrapper.findComponent({ name: 'ElPagination' });
    expect(pagination.exists()).toBe(true);
    pagination.vm.$emit('update:current-page', 3);
    expect(wrapper.emitted('update:pageNum')).toBeTruthy();
    expect(wrapper.emitted('update:pageNum')![0]).toEqual([3]);
  });
});
