package ru.spbstu.terrai.players.samples

import ru.spbstu.terrai.core.*
import java.util.*
import kotlin.collections.ArrayList

class Vindictive : AbstractPlayer() {

    private lateinit var currentLocation: Location

    private lateinit var initialLocation: Location

    private var lastMove: Move = WaitMove

    private var plan = ArrayDeque<Location>()

    private var treasureFound = false

    private var exitFound = false

    private var exitFoundCurrentExperience = false

    // Исследованная местность с момента нашего последнего выхода из ЧД
    private var currentExperience = mutableMapOf<Location, Room>()

    // Наши исследования местности, прерванные попаданиями в ЧД
    private val pastExperiences = ArrayList<MutableMap<Location, Room>>()

    // Во время предыдущих исследований мы натыкались на локации с координатами (i,j) относительно выхода из ЧД
    // Будем запоминать виды и количество комнат, что на этих координатах нам попадались
    //private val reputations = mutableMapOf<Location, MutableMap<Room, Int>>()

    private var expectedRoom: Room? = null

    private var cyclometer = 0

    override fun setStartLocationAndSize(location: Location, width: Int, height: Int) {

        super.setStartLocationAndSize(location, width, height)
        currentLocation = location
        initialLocation = location

        currentExperience.clear()

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
            considerWormholesAsWalls: Boolean
    ): ArrayDeque<Location> {

        var maxDist = Int.MAX_VALUE
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
                            ((experience[i] is Wormhole) and (considerWormholesAsWalls)) -> {

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

        var chosenLocation = if (targetLocations.isNotEmpty())
            targetLocations.first()
        else
            return ArrayDeque<Location>()

        val ans = ArrayDeque<Location>()

        while (paths[chosenLocation] != null) {
            ans.push(chosenLocation)
            chosenLocation = paths[chosenLocation]!!
        }

        return ans
    }

    override fun getNextMove(): Move {

        // CurrentExperience + Выводы, полученные из предыдущих опытов
        val subjectiveExperience = mutableMapOf<Location, Room>()

        if (plan.isEmpty())
            when {
                (!treasureFound) or (!exitFound) -> {

                    val applicableExperiences = pastExperiences
                            .filter { i -> i
                                    .all { (key, value) ->
                                        ((currentExperience[key] == value) or (key !in currentExperience)) } }

                    /*var additionExperience = if (applicableExperiences.isEmpty())
                        mutableSetOf<Pair<Location, Room>>()
                    else
                        applicableExperiences[0].toList().toSet()

                    for (i in 1 until applicableExperiences.size) {
                        val expIntoSet = applicableExperiences[i].toList().toSet()
                        additionExperience = additionExperience.intersect(expIntoSet).toMutableSet()
                    }*/

                    val reputations = mutableMapOf<Location, MutableMap<Room, Int>>()

                    for (i in applicableExperiences) {
                        for (j in i) {
                            if (reputations[j.key] == null)
                                reputations[j.key] = mutableMapOf<Room, Int>()
                            reputations[j.key]!![j.value] = reputations[j.key]?.get(j.value)?.plus(1) ?: 1
                        }
                    }

                    val fullAdditionalExperience = mutableMapOf<Location, Room>()
                    val wormholeAdditionalExperience = mutableMapOf<Location, Room>()

                    for (i in reputations) {
                        if (reputations[i.key]!!.size == 1)
                            fullAdditionalExperience[i.key] = i.value.toList().first().first
                        if (Wormhole(0) in i.value) {
                            fullAdditionalExperience[i.key] = Wormhole(0)
                            wormholeAdditionalExperience[i.key] = Wormhole(0)
                        }
                    }

                    subjectiveExperience.putAll(fullAdditionalExperience)
                    subjectiveExperience.putAll(currentExperience)

                    plan = getTargetLocationPath(subjectiveExperience,
                            currentLocation,
                            null,
                            true)

                    if (plan.isEmpty()) {

                        val currentExperienceWithWormholes = mutableMapOf<Location, Room>()
                        currentExperienceWithWormholes.putAll(currentExperience)
                        currentExperienceWithWormholes.putAll(wormholeAdditionalExperience)

                        plan = getTargetLocationPath(currentExperienceWithWormholes,
                                currentLocation,
                                null,
                                true)

                        if (plan.isEmpty()) {

                            if (cyclometer < 6) {

                                cyclometer++
                                plan = getTargetLocationPath(currentExperienceWithWormholes,
                                        currentLocation,
                                        Wormhole(0),
                                        false)
                            }
                            else {
                                cyclometer = 0
                                plan = getTargetLocationPath(currentExperienceWithWormholes,
                                        currentLocation,
                                        Wormhole(0),
                                        false)

                                if (plan.isNotEmpty()) {
                                    currentExperienceWithWormholes[plan.last] = Wall
                                    plan = getTargetLocationPath(currentExperienceWithWormholes,
                                            currentLocation,
                                            Wormhole(0),
                                            false)
                                }
                            }
                        }
                    }
                }

                exitFoundCurrentExperience ->
                    plan = getTargetLocationPath(currentExperience,
                            currentLocation,
                            Exit,
                            true)

                else -> {
                    val applicableExitExperiences = pastExperiences
                            .filter { i ->
                                (i.all { (key, value)
                                    -> ((currentExperience[key] == value) or (key !in currentExperience)) } and
                                i.any { (_, value)
                                    -> value == Exit } ) }

                    val wormholes = mutableMapOf<Location, Room>()
                    val applicableWormholeExperiences = pastExperiences
                            .filter { i ->
                                (i.all { (key, value)
                                    -> (currentExperience[key] == value) or (key !in currentExperience) } ) }

                    for (i in applicableWormholeExperiences)
                        for (j in i)
                            if (j.value == Wormhole(0))
                                wormholes[j.key] = Wormhole(0)

                    subjectiveExperience.putAll(currentExperience)
                    subjectiveExperience.putAll(wormholes)

                    if (applicableExitExperiences.isEmpty())
                        if (currentLocation != initialLocation)
                            plan = getTargetLocationPath(currentExperience,
                                    currentLocation,
                                    Wormhole(0),
                                    false)
                        else {
                            for (i in Direction.values())
                                if ((initialLocation + i) !in wormholes) {
                                    plan.push(initialLocation + i)
                                    break
                                }

                            if (plan.isEmpty())
                                plan.push(initialLocation + Direction.values().random())
                        }
                    else {
                        subjectiveExperience.putAll(applicableExitExperiences[0])

                        plan = getTargetLocationPath(subjectiveExperience,
                                currentLocation,
                                Exit,
                                true)
                    }
                }
            }

        val nextLocation = if (plan.isNotEmpty())
            plan.pop()
        else
            return WaitMove

        expectedRoom = when (nextLocation) {
            in currentExperience -> currentExperience[nextLocation]!!
            in subjectiveExperience -> subjectiveExperience[nextLocation]!!
            else -> null
        }

        lastMove = WalkMove((nextLocation - currentLocation).toDirection())
        return WalkMove((nextLocation - currentLocation).toDirection())

        //throw IllegalArgumentException("Incorrect path to target location")
    }

    override fun setMoveResult(result: MoveResult) {

        val lastMove = lastMove

        if (lastMove !is WalkMove) {
            println("No move")
            return
        }

        println(lastMove.direction)
        println(result.room)

        val newLocation = lastMove.direction + currentLocation

        val room = if (result.room !is Wormhole)
            result.room
        else
            Wormhole(0)

        currentExperience[newLocation] = room

        if ((expectedRoom != room) and (expectedRoom != null))
            plan.clear()

        if (result.successful) {
            when(room) {
                is WithContent -> {
                    if (!treasureFound && result.condition.hasTreasure) {
                        treasureFound = true
                        cyclometer = 0
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

                    if (pastExperiences.isNotEmpty()) {
                        val copy = currentExperience.toMutableMap()
                        pastExperiences.add(0, copy)
                    }

                    if (pastExperiences.isEmpty()) {
                        currentExperience = currentExperience.mapKeys {
                            Location(
                                    it.key.x + (width - 1 - newLocation.x),
                                    it.key.y + (height - 1 - newLocation.y)
                            )
                        }.toMutableMap()
                    }
                    else {
                        currentExperience = currentExperience.mapKeys {
                            Location(
                                    it.key.x + (initialLocation.x - newLocation.x),
                                    it.key.y + (initialLocation.y - newLocation.y)
                            )
                        }.toMutableMap()
                    }

                    val copy2 = mutableMapOf<Location, Room>() + currentExperience
                    pastExperiences.add(0, copy2 as MutableMap<Location, Room>)

                    if (pastExperiences.size == 1)
                        setStartLocationAndSize(
                                Location(width - 1, height - 1),
                                2 * width - 1,
                                2 * height - 1
                        )
                    else
                        setStartLocationAndSize(Location( (width - 1) / 2, (height - 1) / 2), width, height)

                    /*
                    val freshExp1 = pastExperiences[0]
                    for (i in freshExp1) {
                        if (reputations[i.key] == null)
                            reputations[i.key] = mutableMapOf<Room, Int>()
                        reputations[i.key]!![i.value] = reputations[i.key]?.get(i.value)?.plus(1) ?: 1
                    }

                    if (pastExperiences.size > 1) {
                        val freshExp2 = pastExperiences[1]
                        for (i in freshExp2) {
                            if (reputations[i.key] == null)
                                reputations[i.key] = mutableMapOf<Room, Int>()
                            reputations[i.key]!![i.value] = reputations[i.key]?.get(i.value)?.plus(1) ?: 1
                        }
                    }*/

                }
            }
            when(room) {
                !is Wormhole -> {
                    currentLocation = newLocation
                }
            }
        }
        else
            plan.clear()


    }
}