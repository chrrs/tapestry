package me.chrr.tapestry.gradle

enum class Environment(val value: String) {
    Client("client"),
    Server("server"),
    Both("*"),
}