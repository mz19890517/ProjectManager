package com.mz.projectmanager.data.model

enum class SortOption(val label: String) {
    NAME_ASC("名称 A-Z"),
    NAME_DESC("名称 Z-A"),
    DATE_NEWEST("最新修改"),
    DATE_OLDEST("最早修改"),
    SIZE_LARGEST("最大"),
    SIZE_SMALLEST("最小")
}
