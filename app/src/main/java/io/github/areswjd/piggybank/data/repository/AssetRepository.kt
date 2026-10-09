package io.github.areswjd.piggybank.data.repository

import androidx.room.withTransaction
import io.github.areswjd.piggybank.data.local.AppDatabase
import io.github.areswjd.piggybank.data.local.entity.AssetEntity
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity
import io.github.areswjd.piggybank.model.ValidationError
import io.github.areswjd.piggybank.model.ValidationException
import io.github.areswjd.piggybank.model.validateInitialBalance
import io.github.areswjd.piggybank.model.validateName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 자산 그룹과 세부 자산. 규칙을 어기면 [ValidationException]을 던진다.
 * 그룹·자산은 삭제해도 행을 남기고(soft delete) 화면과 선택 목록에서만 뺀다.
 */
class AssetRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val groupDao = db.assetGroupDao()
    private val assetDao = db.assetDao()

    fun observeAssetTree(): Flow<AssetTree> =
        combine(groupDao.observeActive(), assetDao.observeActiveWithBalance(), ::buildAssetTree)

    suspend fun addGroup(name: String): Long = db.withTransaction {
        val trimmed = requireValidName(name)
        if (groupDao.countActiveWithName(trimmed, excludeId = 0) > 0) fail(ValidationError.NAME_DUPLICATE)
        groupDao.insert(AssetGroupEntity(name = trimmed, sortOrder = groupDao.maxSortOrder() + 1))
    }

    suspend fun renameGroup(id: Long, name: String) = db.withTransaction {
        val group = requireActiveGroup(id)
        val trimmed = requireValidName(name)
        if (groupDao.countActiveWithName(trimmed, excludeId = id) > 0) fail(ValidationError.NAME_DUPLICATE)
        groupDao.update(group.copy(name = trimmed))
    }

    /** 그룹 안에 활성 자산이 없을 때만 삭제한다. */
    suspend fun deleteGroup(id: Long) = db.withTransaction {
        val group = requireActiveGroup(id)
        if (assetDao.countActiveInGroup(id) > 0) fail(ValidationError.GROUP_NOT_EMPTY)
        groupDao.update(group.copy(deletedAt = clock()))
    }

    suspend fun addAsset(groupId: Long, name: String, initialBalance: Long): Long = db.withTransaction {
        requireActiveGroup(groupId)
        val trimmed = requireValidName(name)
        validateInitialBalance(initialBalance)?.let(::fail)
        if (assetDao.countActiveWithName(groupId, trimmed, excludeId = 0) > 0) fail(ValidationError.NAME_DUPLICATE)
        assetDao.insert(
            AssetEntity(
                groupId = groupId,
                name = trimmed,
                initialBalance = initialBalance,
                sortOrder = assetDao.maxSortOrder(groupId) + 1,
            ),
        )
    }

    /** 이름, 초기 잔액, 소속 그룹을 바꾼다. 다른 그룹으로 옮기면 그 그룹의 맨 뒤에 붙인다. */
    suspend fun updateAsset(id: Long, groupId: Long, name: String, initialBalance: Long) = db.withTransaction {
        val asset = requireActiveAsset(id)
        requireActiveGroup(groupId)
        val trimmed = requireValidName(name)
        validateInitialBalance(initialBalance)?.let(::fail)
        if (assetDao.countActiveWithName(groupId, trimmed, excludeId = id) > 0) fail(ValidationError.NAME_DUPLICATE)
        val sortOrder = if (groupId == asset.groupId) asset.sortOrder else assetDao.maxSortOrder(groupId) + 1
        assetDao.update(
            asset.copy(groupId = groupId, name = trimmed, initialBalance = initialBalance, sortOrder = sortOrder),
        )
    }

    /** 현재 잔액이 0원일 때만 삭제한다. 거래 내역은 그대로 남는다. */
    suspend fun deleteAsset(id: Long) = db.withTransaction {
        val asset = requireActiveAsset(id)
        if ((assetDao.balanceOf(id) ?: 0L) != 0L) fail(ValidationError.BALANCE_NOT_ZERO)
        assetDao.update(asset.copy(deletedAt = clock()))
    }

    /** 처음 시작할 때(그룹이 하나도 없을 때) 기본 자산 "현금 > 지갑"을 만든다. */
    suspend fun seedDefaultsIfEmpty() = db.withTransaction {
        if (groupDao.countAll() == 0) {
            val groupId = groupDao.insert(AssetGroupEntity(name = DEFAULT_GROUP_NAME, sortOrder = 0))
            assetDao.insert(AssetEntity(groupId = groupId, name = DEFAULT_ASSET_NAME, initialBalance = 0, sortOrder = 0))
        }
    }

    private suspend fun requireActiveGroup(id: Long): AssetGroupEntity {
        val group = groupDao.get(id)
        if (group == null || group.deletedAt != null) fail(ValidationError.GROUP_NOT_FOUND)
        return group
    }

    private suspend fun requireActiveAsset(id: Long): AssetEntity {
        val asset = assetDao.get(id)
        if (asset == null || asset.deletedAt != null) fail(ValidationError.ASSET_NOT_FOUND)
        return asset
    }

    private fun requireValidName(name: String): String {
        val trimmed = name.trim()
        validateName(trimmed)?.let(::fail)
        return trimmed
    }

    companion object {
        const val DEFAULT_GROUP_NAME = "현금"
        const val DEFAULT_ASSET_NAME = "지갑"
    }
}

internal fun fail(error: ValidationError): Nothing = throw ValidationException(error)
