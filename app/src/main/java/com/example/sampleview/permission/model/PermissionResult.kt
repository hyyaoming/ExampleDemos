package com.example.sampleview.permission.model

/**
 * 权限请求结果的数据模型。
 *
 * @property allGranted 所有权限是否全部被授予。
 * @property granted 被授予的权限列表。
 * @property denied 被拒绝的权限列表。
 */
data class PermissionResult(
    val allGranted: Boolean = false,
    val granted: List<String> = emptyList(),
    val denied: List<String> = emptyList()
) {
    companion object {

        /**
         * 创建一个表示「所有权限都已授予」的结果对象。
         *
         * @param granted 被授予的权限列表。
         * @return 权限结果对象，`allGranted` 为 true，`denied` 为空。
         */
        fun allGranted(granted: List<String>): PermissionResult {
            return PermissionResult(
                allGranted = true,
                granted = granted,
                denied = emptyList()
            )
        }

        /**
         * 创建一个表示「部分权限被授予，部分被拒绝」的结果对象。
         *
         * @param granted 被授予的权限列表。
         * @param denied 被拒绝的权限列表。
         * @return 权限结果对象，`allGranted` 为 false。
         */
        fun partiallyGranted(granted: List<String>, denied: List<String>): PermissionResult {
            return PermissionResult(
                allGranted = false,
                granted = granted,
                denied = denied
            )
        }

        /**
         * 创建一个表示「所有权限都被拒绝」的结果对象。
         *
         * @param denied 被拒绝的权限列表。
         * @return 权限结果对象，`allGranted` 为 false，`granted` 为空。
         */
        fun allDenied(denied: List<String>): PermissionResult {
            return PermissionResult(
                allGranted = false,
                granted = emptyList(),
                denied = denied
            )
        }
    }
}
