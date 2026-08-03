package com.listeik.familyapp.data.model

enum class ItemCategory(
    val label: String,
    val defaultStatus: ItemStatus,
) {
    FOOD("Еда", ItemStatus.READY),
    BUY("Покупки", ItemStatus.NEED_TO_BUY),
    TASK("Дела", ItemStatus.TODO),
    WISH("Хотелки", ItemStatus.WANTED),
}

enum class ItemStatus(val label: String) {
    READY("Готово"),
    IN_PROGRESS("В процессе"),
    FINISHED("Закончено"),
    NEED_TO_BUY("Купить"),
    IN_CART("В корзине"),
    BOUGHT("Куплено"),
    TODO("Сделать"),
    DONE("Сделано"),
    WANTED("Хочется"),
    ARCHIVED("Закрыто");

    fun nextFor(category: ItemCategory): ItemStatus =
        when (category) {
            ItemCategory.FOOD ->
                when (this) {
                    READY -> IN_PROGRESS
                    IN_PROGRESS -> FINISHED
                    else -> FINISHED
                }

            ItemCategory.BUY ->
                when (this) {
                    NEED_TO_BUY -> IN_CART
                    IN_CART -> BOUGHT
                    else -> BOUGHT
                }

            ItemCategory.TASK -> DONE
            ItemCategory.WISH -> ARCHIVED
        }
}

data class FamilySession(
    val familyId: String,
    val familyName: String,
    val inviteCode: String,
    val userId: String,
    val userName: String,
)

data class FamilyItem(
    val id: String,
    val familyId: String,
    val title: String,
    val category: ItemCategory,
    val status: ItemStatus,
    val createdBy: String,
    val updatedBy: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val totalPortions: Int? = null,
    val remainingPortions: Int? = null,
)

data class ActivityEvent(
    val id: String,
    val familyId: String,
    val actorId: String,
    val text: String,
    val itemId: String? = null,
    val createdAtMillis: Long,
)

data class FamilyMessage(
    val id: String,
    val familyId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val itemId: String? = null,
    val createdAtMillis: Long,
)
