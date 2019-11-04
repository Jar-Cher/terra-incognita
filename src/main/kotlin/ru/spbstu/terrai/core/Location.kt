package ru.spbstu.terrai.core

// (0, 0) is in the upper-left corner (not including outer walls)
data class Location(val x: Int, val y: Int) {

    operator fun plus(other: Location) = Location(this.x + other.x, this.y + other.y)

    operator fun plus(other: Direction) = Location(this.x + other.dx, this.y + other.dy)

    operator fun minus(other: Location) = Location(this.x - other.x, this.y - other.y)

    operator fun minus(other: Direction) = Location(this.x - other.dx, this.y - other.dy)

    fun toDirection() = when {
        (x == 0) and (y == -1) -> Direction.NORTH
        (x == 1) and (y == 0) -> Direction.EAST
        (x == 0) and (y == 1) -> Direction.SOUTH
        (x == -1) and (y == 0) -> Direction.WEST
        else -> throw IllegalArgumentException("Incorrect location transformation to location")
    }
}