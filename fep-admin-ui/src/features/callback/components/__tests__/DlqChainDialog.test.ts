import { mount, flushPromises } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ElementPlus from 'element-plus';
import DlqChainDialog from '../DlqChainDialog.vue';
import { callbackDlqApi } from '../../api/callbackDlq';

vi.mock('../../api/callbackDlq');

const chainRows = [
  {
    queueId: 'D1',
    targetInterfaceId: 'IF-001',
    msgNo: '9001',
    status: 'DEAD_LETTER',
    retryCount: 5,
    lastError: 'io timeout',
    updateTime: '2026-06-05T10:00:00',
    originalDlqId: null,
    replayedBy: null,
    replayedAt: null,
  },
  {
    queueId: 'D2',
    targetInterfaceId: 'IF-001',
    msgNo: '9001',
    status: 'PENDING',
    retryCount: 0,
    lastError: null,
    updateTime: '2026-06-05T10:05:00',
    originalDlqId: 'D1',
    replayedBy: 'admin1',
    replayedAt: '2026-06-05T10:05:00',
  },
];

describe('DlqChainDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) document.body.removeChild(document.body.firstChild);
  });

  it('does not fetch the chain while closed', async () => {
    mount(DlqChainDialog, {
      props: { modelValue: false, queueId: 'D1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    expect(callbackDlqApi.chain).not.toHaveBeenCalled();
  });

  it('fetches and renders the replay chain when opened', async () => {
    vi.mocked(callbackDlqApi.chain).mockResolvedValue(chainRows);
    const wrapper = mount(DlqChainDialog, {
      props: { modelValue: false, queueId: 'D1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();

    await wrapper.setProps({ modelValue: true });
    await flushPromises();

    expect(callbackDlqApi.chain).toHaveBeenCalledWith('D1');
    const body = document.body.textContent || '';
    expect(body).toContain('D1');
    expect(body).toContain('D2');
    expect(body).toContain('admin1');
  });
});
