package io.github.areswjd.piggybank.data.repository

import io.github.areswjd.piggybank.data.local.dao.AssetBalance
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity

/** 자산 탭에 보여줄 그룹 > 자산 트리 (활성 항목만). */
data class AssetTree(val groups: List<AssetGroupWithAssets>) {
    val total: Long get() = groups.sumOf { it.subtotal }
    val isEmpty: Boolean get() = groups.isEmpty()
}

data class AssetGroupWithAssets(
    val group: AssetGroupEntity,
    val assets: List<AssetBalance>,
) {
    val subtotal: Long get() = assets.sumOf { it.balance }
}

/** 그룹 순서를 유지하면서 각 그룹 아래 자산을 붙인다. 목록에 없는 그룹의 자산은 버린다. */
fun buildAssetTree(groups: List<AssetGroupEntity>, assets: List<AssetBalance>): AssetTree {
    val assetsByGroup = assets.groupBy { it.groupId }
    return AssetTree(groups.map { AssetGroupWithAssets(it, assetsByGroup[it.id].orEmpty()) })
}
