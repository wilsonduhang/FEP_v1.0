package com.puchain.fep.web.sysmgmt.download.service;

/**
 * 导出任务执行器接口。
 *
 * <p>各业务模块实现此接口，提供具体的文件生成逻辑。
 * 执行过程中通过 {@link ProgressCallback} 回调上报进度，
 * 由 {@link DownloadTaskService} 负责更新数据库中的任务进度。
 * 参见 PRD v1.3 §5.10.5 下载任务。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface ExportTaskExecutor {

    /**
     * 执行导出任务，将结果写入指定路径。
     *
     * @param taskId           下载任务 ID
     * @param filePath         文件输出路径
     * @param progressCallback 进度回调，用于上报生成进度（0-100）
     * @throws Exception 文件生成过程中发生的任何异常
     */
    void execute(String taskId, String filePath, ProgressCallback progressCallback) throws Exception;

    /**
     * 任务进度回调接口。
     *
     * <p>由 {@link ExportTaskExecutor#execute} 实现者在关键节点调用，
     * 将当前进度百分比传递给任务管理层。</p>
     */
    @FunctionalInterface
    interface ProgressCallback {

        /**
         * 更新任务进度。
         *
         * @param percent 当前进度百分比（0-100）
         */
        void update(int percent);
    }
}
