package com.ordermanagement.dataapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class OrderDataApiApplication

fun main(args: Array<String>) {
    runApplication<OrderDataApiApplication>(*args)
}
