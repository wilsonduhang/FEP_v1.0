import { onUnmounted } from 'vue';

export interface AutoRefreshController {
  start: () => void;
  stop: () => void;
  refresh: () => Promise<void>;
}

/**
 * Runs an async loader once immediately then on a fixed interval.
 * Automatically stops on component unmount.
 *
 * @param loader Async loader invoked on start and on every interval tick.
 * @param intervalMs Polling interval in milliseconds (default 30000).
 */
export function useAutoRefresh(
  loader: () => Promise<void>,
  intervalMs = 30000,
): AutoRefreshController {
  let timer: ReturnType<typeof setInterval> | null = null;

  function stop(): void {
    if (timer !== null) {
      clearInterval(timer);
      timer = null;
    }
  }

  function start(): void {
    stop();
    void loader();
    timer = setInterval(() => {
      void loader();
    }, intervalMs);
  }

  async function refresh(): Promise<void> {
    await loader();
  }

  onUnmounted(stop);

  return { start, stop, refresh };
}
