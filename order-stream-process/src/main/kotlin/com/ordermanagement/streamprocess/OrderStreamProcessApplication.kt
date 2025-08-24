package com.ordermanagement.streamprocess

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class OrderStreamProcessApplication

fun main(args: Array<String>) {
    runApplication<OrderStreamProcessApplication>(*args)
}
