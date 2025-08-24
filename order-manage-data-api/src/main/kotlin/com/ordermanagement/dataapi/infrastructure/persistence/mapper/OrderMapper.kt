package com.ordermanagement.dataapi.infrastructure.persistence.mapper

import com.ordermanagement.dataapi.domain.entity.*
import com.ordermanagement.dataapi.infrastructure.persistence.entity.*
import org.springframework.stereotype.Component

@Component
class OrderMapper {

    fun mapOrderToEntity(order: Order): OrderEntity {
        return OrderEntity(
                orderId = order.orderId,
                orderNumber = order.orderNumber,
                customerId = order.customerId,
                productTypeCount = order.productTypeCount,
                totalPrice = order.totalPrice,
                orderStatus = order.orderStatus,
                createDate = order.createDate,
                createBy = order.createBy,
                updateDate = order.updateDate,
                updateBy = order.updateBy
        )
    }

    fun mapEntityToOrder(entity: OrderEntity): Order {
        return Order(
                orderId = entity.orderId,
                orderNumber = entity.orderNumber,
                customerId = entity.customerId,
                productTypeCount = entity.productTypeCount,
                totalPrice = entity.totalPrice,
                orderStatus = entity.orderStatus,
                createDate = entity.createDate,
                createBy = entity.createBy,
                updateDate = entity.updateDate,
                updateBy = entity.updateBy
        )
    }

    fun mapOrderDetailToEntity(detail: OrderDetail, orderEntity: OrderEntity): OrderDetailEntity {
        return OrderDetailEntity(
                order = orderEntity,
                productId = detail.productId,
                price = detail.price,
                quantity = detail.quantity,
                createDate = detail.createDate,
                createBy = detail.createBy,
                updateDate = detail.updateDate,
                updateBy = detail.updateBy
        )
    }

    fun mapEntityToOrderDetail(entity: OrderDetailEntity): OrderDetail {
        return OrderDetail(
                orderId = entity.order.orderId,
                productId = entity.productId,
                price = entity.price,
                quantity = entity.quantity,
                createDate = entity.createDate,
                createBy = entity.createBy,
                updateDate = entity.updateDate,
                updateBy = entity.updateBy
        )
    }
}

@Component
class InventoryMapper {

    fun mapInventoryToEntity(inventory: Inventory): InventoryEntity {
        return InventoryEntity(
                productId = inventory.productId,
                sku = inventory.sku,
                productTitle = inventory.productTitle,
                productPrice = inventory.productPrice,
                currency = inventory.currency,
                availableQuantity = inventory.availableQuantity,
                createDate = inventory.createDate,
                createBy = inventory.createBy,
                updateDate = inventory.updateDate,
                updateBy = inventory.updateBy
        )
    }

    fun mapEntityToInventory(entity: InventoryEntity): Inventory {
        return Inventory(
                productId = entity.productId,
                sku = entity.sku,
                productTitle = entity.productTitle,
                productPrice = entity.productPrice,
                currency = entity.currency,
                availableQuantity = entity.availableQuantity,
                createDate = entity.createDate,
                createBy = entity.createBy,
                updateDate = entity.updateDate,
                updateBy = entity.updateBy
        )
    }
}
