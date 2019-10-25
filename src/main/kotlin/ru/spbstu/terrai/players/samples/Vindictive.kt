package ru.spbstu.terrai.players.samples

import ru.spbstu.terrai.core.*

class Vindictive : AbstractPlayer() {

    private lateinit var currentLocation: Location

    private var lastMove: Move = WaitMove

    private var plan = ArrayList<Location>()

    private var treasureFound = false

    private var exitFound = false

    private var exitFoundCurrentExperience = false

    // Исследованная местность с момента нашего последнего выхода из ЧД
    private val currentExperience = mutableMapOf<Location, Room>()

    // Наши исследования местности, прерванные попаданиями в ЧД
    private val pastExperiences = ArrayList<MutableMap<Location, Room>>()

    // Во время предыдущих исследований мы натыкались на локации с координатами (i,j) относительно выхода из ЧД
    // Будем запоминать виды и количество комнат, что на этих координатах нам попадались
    private val reputations = mutableMapOf<Location, MutableMap<Room, Int>>()

    // CurrentExperience + Выводы, полученные из reputations
    private val subjectiveExperience = mutableMapOf<Location, Room>()

    override fun setStartLocationAndSize(location: Location, width: Int, height: Int) {

        super.setStartLocationAndSize(location, width, height)
        currentLocation = location

        exitFoundCurrentExperience = false

        if (pastExperiences.size == 0)
            currentExperience[currentLocation] = Entrance
        else
            currentExperience[currentLocation] = Wormhole(0)

        for (i in -1..height) {
            currentExperience[Location(-1, i)] = Wall
            currentExperience[Location(width, i)] = Wall
        }
        for (i in 0 until width) {
            currentExperience[Location(i, height)] = Wall
            currentExperience[Location(i, -1)] = Wall
        }
    }

    private fun getTargetLocationPath(
            experience: Map<Location, Room>,
            initialLocation: Location,
            targetType: Room?,
            ignoreWormholes: Boolean
    ): ArrayList<Location> {

        var maxDist = 100500
        val locationQueue = ArrayList<Location>()
        val distances = mutableMapOf<Location, Int>()
        val targetLocations = ArrayList<Location>()
        val paths = mutableMapOf<Location,Location>()

        distances[initialLocation] = 0
        locationQueue.add(initialLocation)

        while (locationQueue.isNotEmpty()) {

            val currentLocation = locationQueue[0]

            val neighbors = mutableListOf(Direction.NORTH + currentLocation,
                    Direction.SOUTH + currentLocation,
                    Direction.WEST + currentLocation,
                    Direction.EAST + currentLocation
                    ).filterNot { i -> i in distances }

            for (i in neighbors) {
                distances[i] = distances[currentLocation]!! + 1
                paths[i] = currentLocation
            }

            for (i in neighbors) {

                when {
                    (i in locationQueue) or
                            (experience[i] is Wall) or
                            (distances[i]!! > maxDist) or
                            ((experience[i] is Wormhole) and (!ignoreWormholes)) -> {

                    }
                    (((i !in experience) and (targetType == null)) or
                            (experience[i] == targetType)) and
                            (distances[i]!! <= maxDist) -> {

                        targetLocations.add(i)
                        maxDist = distances[i]!!
                    }
                    else -> locationQueue.add(i)
                }
            }

            locationQueue.removeAt(0)
        }

        var chosenLocation = targetLocations.random()

        val ans = ArrayList<Location>()

        while (paths[chosenLocation] != null) {
            ans.add(0, chosenLocation)
            chosenLocation = paths[chosenLocation]!!
        }

        return ans
    }

    override fun getNextMove(): Move {

        if (plan.isEmpty())
            when {
                (!treasureFound) or (!exitFound) ->
                    plan = getTargetLocationPath(currentExperience,
                            currentLocation,
                            null,
                            true)
                exitFoundCurrentExperience ->
                    plan = getTargetLocationPath(currentExperience,
                            currentLocation,
                            Exit,
                            false)
                else -> {

                }
            }

        val nextLocation = plan[0]
        plan.removeAt(0)

        for (i in Direction.values())
            if (i + currentLocation == nextLocation) {
                lastMove = WalkMove(i)
                return WalkMove(i)
            }

        throw IllegalArgumentException("Incorrect path to target location")
    }

    override fun setMoveResult(result: MoveResult) {

        println((lastMove as? WalkMove)?.direction ?: "no move")
        println(result.room)

        val newLocation = (lastMove as? WalkMove)?.let { it.direction + currentLocation } ?: currentLocation
        val room = result.room
        currentExperience[newLocation] = room
        if (result.successful) {
            when(room) {
                is WithContent -> {
                    if (!treasureFound && result.condition.hasTreasure) {
                        treasureFound = true
                        plan.clear()
                    }
                }
                is Exit -> {
                    exitFound = true
                    exitFoundCurrentExperience = true
                }
                is Wormhole -> {
                    exitFoundCurrentExperience = false
                    plan.clear()
                }
            }
            currentLocation = newLocation
        }
        else
            plan.clear()


    }
}