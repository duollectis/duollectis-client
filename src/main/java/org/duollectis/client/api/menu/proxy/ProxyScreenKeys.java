package org.duollectis.client.api.menu.proxy;

/** All translation-key constants used by the Proxy Manager screen. */
final class ProxyScreenKeys {
    private ProxyScreenKeys() {}

    // ── screen / list headers ─────────────────────────────────────────────────
    static final String TITLE           = "screen.duollectis.proxy_manager.title";
    static final String LIST_RAW        = "screen.duollectis.proxy_manager.list.raw";
    static final String LIST_CHECKED    = "screen.duollectis.proxy_manager.list.checked";

    // ── section headers ───────────────────────────────────────────────────────
    static final String SEC_ADD         = "screen.duollectis.proxy_manager.section.add_proxy";
    static final String SEC_EDIT        = "screen.duollectis.proxy_manager.section.edit_proxy";
    static final String SEC_CHECKER     = "screen.duollectis.proxy_manager.section.checker";

    // ── field labels ──────────────────────────────────────────────────────────
    static final String LBL_NAME        = "screen.duollectis.proxy_manager.label.name";
    static final String LBL_ADDRESS     = "screen.duollectis.proxy_manager.label.address";
    static final String LBL_PORT        = "screen.duollectis.proxy_manager.label.port";
    static final String LBL_USERNAME    = "screen.duollectis.proxy_manager.label.username";
    static final String LBL_PASSWORD    = "screen.duollectis.proxy_manager.label.password";
    static final String LBL_MAX_PINGS   = "screen.duollectis.proxy_manager.label.max_pings";
    static final String LBL_DELAY_MS    = "screen.duollectis.proxy_manager.label.delay_ms";
    static final String LBL_THREADS     = "screen.duollectis.proxy_manager.label.threads";
    static final String LBL_TARGET      = "screen.duollectis.proxy_manager.label.target";

    // ── field placeholders ────────────────────────────────────────────────────
    static final String FLD_NAME        = "screen.duollectis.proxy_manager.field.name";
    static final String FLD_ADDRESS     = "screen.duollectis.proxy_manager.field.address";
    static final String FLD_PORT        = "screen.duollectis.proxy_manager.field.port";
    static final String TARGET_FLD_PORT = "screen.duollectis.proxy_manager.field.port";
    static final String FLD_USERNAME    = "screen.duollectis.proxy_manager.field.username";
    static final String FLD_PASSWORD    = "screen.duollectis.proxy_manager.field.password";
    static final String FLD_THREADS     = "screen.duollectis.proxy_manager.field.threads";

    // ── buttons ───────────────────────────────────────────────────────────────
    static final String BTN_PING_ON     = "screen.duollectis.proxy_manager.btn.ping_on";
    static final String BTN_PING_OFF    = "screen.duollectis.proxy_manager.btn.ping_off";
    static final String BTN_ADD         = "screen.duollectis.proxy_manager.btn.add";
    static final String BTN_REMOVE      = "screen.duollectis.proxy_manager.btn.remove";
    static final String BTN_SAVE        = "screen.duollectis.proxy_manager.btn.save";
    static final String BTN_CANCEL      = "screen.duollectis.proxy_manager.btn.cancel";
    static final String BTN_START       = "screen.duollectis.proxy_manager.btn.start";
    static final String BTN_STOP        = "screen.duollectis.proxy_manager.btn.stop";
    static final String BTN_IMPORT_CLIP = "screen.duollectis.proxy_manager.btn.import_clip";
    static final String BTN_IMPORT_FILE = "screen.duollectis.proxy_manager.btn.import_file";
    static final String BTN_DEL_FAILED  = "screen.duollectis.proxy_manager.btn.del_failed";
    static final String BTN_CLEAR       = "screen.duollectis.proxy_manager.btn.clear";
    static final String BTN_RESET_RAW   = "screen.duollectis.proxy_manager.btn.reset_raw";
    static final String BTN_EXPORT_OK   = "screen.duollectis.proxy_manager.btn.export_ok";
    static final String BTN_RESET_CHK   = "screen.duollectis.proxy_manager.btn.reset_checked";
    static final String BTN_RECHECK     = "screen.duollectis.proxy_manager.btn.recheck";
    static final String BTN_SORT        = "screen.duollectis.proxy_manager.btn.sort";
    static final String BTN_DONE        = "screen.duollectis.proxy_manager.btn.done";

    // ── sort modes ────────────────────────────────────────────────────────────
    static final String SORT_NONE       = "screen.duollectis.proxy_manager.sort.none";
    static final String SORT_LAT_ASC    = "screen.duollectis.proxy_manager.sort.ping_asc";
    static final String SORT_LAT_DESC   = "screen.duollectis.proxy_manager.sort.ping_desc";
    static final String SORT_PING_COUNT = "screen.duollectis.proxy_manager.sort.ping_count";
    static final String SORT_TYPE       = "screen.duollectis.proxy_manager.sort.type";
    static final String SORT_IP         = "screen.duollectis.proxy_manager.sort.ip";

    // ── status labels ─────────────────────────────────────────────────────────
    static final String ST_CHECKING     = "screen.duollectis.proxy_manager.status.checking";
    static final String ST_FAILED       = "screen.duollectis.proxy_manager.status.failed";
    static final String ST_OK           = "screen.duollectis.proxy_manager.status.ok";
    static final String ST_UNKNOWN      = "screen.duollectis.proxy_manager.status.unknown";
    static final String ST_DEAD_IP      = "screen.duollectis.proxy_manager.status.dead_ip";
    static final String ST_DEAD_SERVER  = "screen.duollectis.proxy_manager.status.dead_server";
    static final String ST_PING_ERROR   = "screen.duollectis.proxy_manager.status.ping_error";

    // ── progress bar ──────────────────────────────────────────────────────────
    static final String PROG_CHECKING   = "screen.duollectis.proxy_manager.progress.checking";
    static final String PROG_PAUSED     = "screen.duollectis.proxy_manager.progress.paused";
    static final String PROG_DONE       = "screen.duollectis.proxy_manager.progress.done";

    // ── active proxy ──────────────────────────────────────────────────────────
    static final String ACTIVE_NONE     = "screen.duollectis.proxy_manager.active.none";
    static final String ACTIVE_PREFIX   = "screen.duollectis.proxy_manager.active.prefix";

    // ── entry display ─────────────────────────────────────────────────────────
    static final String ENTRY_PING      = "screen.duollectis.proxy_manager.entry.ping";
    static final String ENTRY_PING_MS   = "screen.duollectis.proxy_manager.entry.ping_ms";
    static final String ENTRY_COPY_BTN  = "screen.duollectis.proxy_manager.entry.copy_btn";

    // ── status messages ───────────────────────────────────────────────────────
    static final String MSG_SETTINGS    = "screen.duollectis.proxy_manager.msg.settings_applied";
    static final String MSG_EDITING     = "screen.duollectis.proxy_manager.msg.editing";
    static final String MSG_SAVED       = "screen.duollectis.proxy_manager.msg.proxy_saved";
    static final String MSG_ADDR_REQ    = "screen.duollectis.proxy_manager.msg.address_port_required";
    static final String MSG_BAD_PORT    = "screen.duollectis.proxy_manager.msg.invalid_port";
    static final String MSG_CLIP_EMPTY  = "screen.duollectis.proxy_manager.msg.clipboard_empty";
    static final String MSG_NO_PROXIES  = "screen.duollectis.proxy_manager.msg.no_valid_proxies";
    static final String MSG_NO_RECHECK  = "screen.duollectis.proxy_manager.msg.no_checked_to_recheck";
    static final String MSG_DELETED     = "screen.duollectis.proxy_manager.msg.deleted_failed";
    static final String MSG_CLEARED     = "screen.duollectis.proxy_manager.msg.cleared_raw";
    static final String MSG_RESET_RAW   = "screen.duollectis.proxy_manager.msg.reset_raw";
    static final String MSG_RESET_CHK   = "screen.duollectis.proxy_manager.msg.reset_checked";
    static final String MSG_RECHECKING  = "screen.duollectis.proxy_manager.msg.rechecking";
    static final String MSG_IMPORTED    = "screen.duollectis.proxy_manager.msg.imported";
    static final String MSG_EXPORTED    = "screen.duollectis.proxy_manager.msg.exported";
    static final String MSG_ERR_FILE    = "screen.duollectis.proxy_manager.msg.error_reading_file";
    static final String MSG_ERR_EXPORT  = "screen.duollectis.proxy_manager.msg.export_error";
    static final String MSG_COPIED      = "screen.duollectis.proxy_manager.msg.copied";

    // ── file dialog ───────────────────────────────────────────────────────────
    static final String DLG_IMP_TITLE   = "screen.duollectis.proxy_manager.dialog.import_title";
    static final String DLG_IMP_FILTER  = "screen.duollectis.proxy_manager.dialog.import_filter";
    static final String DLG_EXP_TITLE   = "screen.duollectis.proxy_manager.dialog.export_title";
    static final String DLG_EXP_FILTER  = "screen.duollectis.proxy_manager.dialog.export_filter";
}
