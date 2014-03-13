package ee.webmedia.alfresco.common.listener;

/**
 * I refactored this enum out from StatisticsPhaseListener, to prevent NPE during development
 */
public enum StatisticsPhaseListenerLogColumn {
    REQUEST_END,
    REQUEST_CANCEL,
    SERVLET_PATH,
    PHASE_1RESTORE_VIEW,
    PHASE_2APPLY_REQUEST_VALUES,
    PHASE_3PROCESS_VALIDATIONS,
    PHASE_4UPDATE_MODEL_VALUES,
    PHASE_5INVOKE_APPLICATION,
    PHASE_6RENDER_RESPONSE,
    ACTION_LISTENER,
    ACTION,
    OUTCOME,
    VIEWID,
    EVENT,
    SESSION_SIZE,
    HIBERNATE,
    DB,
    TX_RETRY,
    TX_ROLLBACK_RO,
    TX_ROLLBACK_RW,
    TX_COMMIT_RO,
    TX_COMMIT_RW,
    IDX_FLUSH,
    IDX_PREPARE,
    IDX_COMMIT,
    IDX_ROLLBACK,
    IDX_QUERY,
    SRV_OOO,
    SRV_MSO,
}