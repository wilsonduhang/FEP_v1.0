import { mount, flushPromises } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ElementPlus, { ElMessage } from 'element-plus';
import DlqReplayConfirmDialog from '../DlqReplayConfirmDialog.vue';
import { callbackDlqApi } from '../../api/callbackDlq';
import { findButtonByText, setupElMessageSpies } from '@/shared/test-utils';

vi.mock('../../api/callbackDlq');

const record = {
  queueId: 'D1',
  targetInterfaceId: 'IF-001',
  msgNo: '9001',
  status: 'DEAD_LETTER',
  retryCount: 5,
  lastError: 'io timeout',
  updateTime: 't',
  originalDlqId: null,
  replayedBy: null,
  replayedAt: null,
} as const;

describe('DlqReplayConfirmDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    setupElMessageSpies();
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) document.body.removeChild(document.body.firstChild);
  });

  it('calls replay with queueId and emits replayed on confirm', async () => {
    vi.mocked(callbackDlqApi.replay).mockResolvedValue({
      newQueueId: 'NEW-1',
      originalDlqId: 'D1',
      replayedAt: 't',
    });
    const wrapper = mount(DlqReplayConfirmDialog, {
      props: { modelValue: true, record },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    expect(document.body.textContent || '').toContain('D1');

    findButtonByText('确认重放')!.click();
    await flushPromises();

    expect(callbackDlqApi.replay).toHaveBeenCalledWith('D1');
    expect(ElMessage.success).toHaveBeenCalledWith('已重放，新队列 ID：NEW-1');
    expect(wrapper.emitted('replayed')).toBeTruthy();
  });
});
