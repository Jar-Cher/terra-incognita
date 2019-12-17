package ru.spbstu.terrai.players.samples

import ru.spbstu.terrai.core.*
import java.util.*
import kotlin.collections.ArrayList

class Vindictive : AbstractPlayer() {

    // Текущие координаты бота
    private lateinit var currentLocation: Location

    // Координаты бота на момент входа в лабиринт или выхода из ЧД
    private lateinit var initialLocation: Location

    private var lastMove: Move = WaitMove

    private var plan = ArrayDeque<Location>()

    private var treasureFound = false

    // Был ли найден выход когда-либо
    private var exitFound = false

    // Был ли найден выход с момента нашего последнего выхода из ЧД
    private var exitFoundCurrentExperience = false

    // Исследованная местность с момента нашего последнего выхода из ЧД
    private var currentExperience = mutableMapOf<Location, Room>()

    // Наши исследования местности, прерванные попаданиями в ЧД
    private val pastExperiences = ArrayList<MutableMap<Location, Room>>()

    // Следующая комната исходя из наших предположений
    private var expectedRoom: Room? = null

    // Индикатор того, что мы ходим кругами
    private var cyclometer = 0

    // Создаём новый пласт опыта
    override fun setStartLocationAndSize(location: Location, width: Int, height: Int) {

        super.setStartLocationAndSize(location, width, height)
        currentLocation = location
        initialLocation = location

        currentExperience.clear()

        // Устанавливаем стартовую комнату исходя из наших знаний
        if (pastExperiences.size == 0)
            currentExperience[currentLocation] = Entrance
        else
            currentExperience[currentLocation] = Wormhole(0)

        // Забиваем внешний периметр стенами - он непроходим по умолчанию
        for (i in -1..height) {
            currentExperience[Location(-1, i)] = Wall
            currentExperience[Location(width, i)] = Wall
        }
        for (i in 0 until width) {
            currentExperience[Location(i, height)] = Wall
            currentExperience[Location(i, -1)] = Wall
        }
    }

    // Строим путь (последовательность из Location'ов) в experience
    // Она ведёт из initialLocation в ближаюшую location с targetType Room внутри
    // Если targetType == null, то ищем ближайшую неразведанную Location
    // Стены непроходимы, проходимость ЧД определяется параметром considerWormholesAsWalls
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

        // Этот цикл - суть BFS
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

        // Если подходящих Location нет, мы возвращаем пустой стек
        var chosenLocation = if (targetLocations.isNotEmpty())
            targetLocations.first()
        else
            return ArrayDeque()

        // Иначе строим путь к этой Location
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

        // Если плана нет, нужно его создать
        if (plan.isEmpty())
            when {
                // Если ни сокровище, ни выход никогда не были найдены
                (!treasureFound) or (!exitFound) -> {

                    // Отберём опыты, не противоречащие текущему (применимые опыты)
                    val applicableExperiences = pastExperiences
                            .filter { i -> i
                                    .all { (key, value) ->
                                        ((currentExperience[key] == value) or (key !in currentExperience)) } }

                    // Во время предыдущих исследований мы натыкались на локации с координатами (i,j) относительно выхода из ЧД
                    // Вспомним виды и количество комнат, что на этих координатах нам попадались
                    val reputations = mutableMapOf<Location, MutableMap<Room, Int>>()

                    for (i in applicableExperiences) {
                        for (j in i) {
                            if (reputations[j.key] == null)
                                reputations[j.key] = mutableMapOf()
                            reputations[j.key]!![j.value] = reputations[j.key]?.get(j.value)?.plus(1) ?: 1
                        }
                    }

                    // Из всех применимых опытов мы отберём те Location'ы, чьи Room'ы имеют "чистую" репутацию
                    // и те Location, в которых, возможно, расположена ЧД
                    val fullAdditionalExperience = mutableMapOf<Location, Room>()

                    // Карта всех Location, в которых, возможно, расположена ЧД
                    val wormholeAdditionalExperience = mutableMapOf<Location, Room>()

                    for (i in reputations) {
                        if (reputations[i.key]!!.size == 1)
                            fullAdditionalExperience[i.key] = i.value.toList().first().first
                        if (Wormhole(0) in i.value) {
                            fullAdditionalExperience[i.key] = Wormhole(0)
                            wormholeAdditionalExperience[i.key] = Wormhole(0)
                        }
                    }

                    // Добавим наши выводы к текущей ситуации
                    subjectiveExperience.putAll(fullAdditionalExperience)
                    subjectiveExperience.putAll(currentExperience)

                    // Ищем гарантированно не исследованные Location
                    plan = getTargetLocationPath(subjectiveExperience,
                            currentLocation,
                            null,
                            true)

                    // Если таких Location нет, значит, fullAdditionalExperience не соответствует действительности
                    if (plan.isEmpty()) {

                        // Угрозу ЧД никто не отменял, избегаем их
                        val currentExperienceWithWormholes = mutableMapOf<Location, Room>()
                        currentExperienceWithWormholes.putAll(currentExperience)
                        currentExperienceWithWormholes.putAll(wormholeAdditionalExperience)

                        plan = getTargetLocationPath(currentExperienceWithWormholes,
                                currentLocation,
                                null,
                                true)

                        // Если кроме Location'ов, подозрительных на наличие ЧД, у нас ничего не осталось,
                        // то идём в ближайшую неисследованную область, даже несмотря на то, что там, возможно, ЧД
                        if (plan.isEmpty()) {

                            // Математически выяснено, что более 5 абсолютно идентичных комнат в лабиринте быть не может
                            if (cyclometer < 6) {

                                cyclometer++
                                plan = getTargetLocationPath(currentExperienceWithWormholes,
                                        currentLocation,
                                        Wormhole(0),
                                        false)
                            }
                            // Таким образом, если мы слишком долго ходим в цикле,
                            // то ломаем шаблон и идём какую-то ДРУГУЮ ЧД
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

                // Сокровище найдено, выход видели с момента нашего последнего выхода из ЧД
                // aka "rush E"
                exitFoundCurrentExperience ->
                    plan = getTargetLocationPath(currentExperience,
                            currentLocation,
                            Exit,
                            true)

                // Сокровище найдено, но выход видели до нашего последнего выхода из ЧД
                else -> {
                    // Отбираем опыты, не противоречащие текущему и имеющие выход
                    val applicableExitExperiences = pastExperiences
                            .filter { i ->
                                (i.all { (key, value)
                                    -> ((currentExperience[key] == value) or (key !in currentExperience)) } and
                                i.any { (_, value)
                                    -> value == Exit } ) }

                    // Выбираем локации, подозрительные на наличие ЧД
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

                    // Если мы ранее видели выход, но все предыдущие опыты, имеющие выход, противоречат текущему опыту,
                    // то идём в ближайшую ЧД
                    if (applicableExitExperiences.isEmpty())
                        if (currentLocation != initialLocation)
                            plan = getTargetLocationPath(currentExperience,
                                    currentLocation,
                                    Wormhole(0),
                                    false)
                        else {
                            // Если мы уже стоим на ЧД, то нужно сделать шаг в сторону, чтобы потом мы могли вернуться
                            // обратно в исходную ЧД во избежание попадания в цикл
                            for (i in Direction.values())
                                if ((initialLocation + i) !in wormholes) {
                                    plan.push(initialLocation + i)
                                    break
                                }

                            // Дефолтный вариант
                            if (plan.isEmpty())
                                plan.push(initialLocation + Direction.values().random())
                        }
                    else {
                        // Если у нас есть предполагаемая локация с выходом, топаем туда
                        subjectiveExperience.putAll(applicableExitExperiences[0])

                        plan = getTargetLocationPath(subjectiveExperience,
                                currentLocation,
                                Exit,
                                true)
                    }
                }
            }

        // следующий шаг
        val nextLocation = if (plan.isNotEmpty())
            plan.pop()
        else
            return WaitMove

        // указываем тип комнаты, который ожидаем увидеть
        expectedRoom = when (nextLocation) {
            in currentExperience -> currentExperience[nextLocation]!!
            in subjectiveExperience -> subjectiveExperience[nextLocation]!!
            else -> null
        }

        lastMove = WalkMove((nextLocation - currentLocation).toDirection())
        return WalkMove((nextLocation - currentLocation).toDirection())

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

        // Во избежание ряда конфликтов id ЧД принимаем равной 0 во всех случаях
        val room = if (result.room !is Wormhole)
            result.room
        else
            Wormhole(0)

        currentExperience[newLocation] = room

        // Если получили неожиданный результат, очищаем план
        if ((expectedRoom != room) and (expectedRoom != null))
            plan.clear()

        if (result.successful) {
            when(room) {
                is WithContent -> {
                    // Нашли сокровище
                    if (!treasureFound && result.condition.hasTreasure) {
                        treasureFound = true
                        cyclometer = 0
                        plan.clear()
                    }
                }
                // Нашли выход
                is Exit -> {
                    exitFound = true
                    exitFoundCurrentExperience = true
                }
                // Попали в ЧД
                is Wormhole -> {
                    exitFoundCurrentExperience = false
                    plan.clear()

                    // Если это не первая наша ЧД, то
                    // добавляем в прошлый опыт текущий опыт с исходной ЧД нашего опыта в центре карты
                    if (pastExperiences.isNotEmpty()) {
                        val copy = currentExperience.toMutableMap()
                        pastExperiences.add(0, copy)
                    }

                    // Сдвигаем текущий опыт так, чтобы итоговая ЧД оказалась в центре карты
                    if (pastExperiences.isEmpty()) {
                        // Если это наша первая ЧД
                        currentExperience = currentExperience.mapKeys {
                            Location(
                                    it.key.x + (width - 1 - newLocation.x),
                                    it.key.y + (height - 1 - newLocation.y)
                            )
                        }.toMutableMap()
                    }
                    else {
                        // Если это не первая наша ЧД
                        currentExperience = currentExperience.mapKeys {
                            Location(
                                    it.key.x + (initialLocation.x - newLocation.x),
                                    it.key.y + (initialLocation.y - newLocation.y)
                            )
                        }.toMutableMap()
                    }

                    // Добавляем в прошлый опыт текущий опыт с завершающей ЧД нашего опыта в центре карты
                    val copy2 = mutableMapOf<Location, Room>() + currentExperience
                    pastExperiences.add(0, copy2 as MutableMap<Location, Room>)

                    // Задаём стартовую точку и границы лабиринта - после первого попадания в ЧД и после последующих
                    if (pastExperiences.size == 1)
                        setStartLocationAndSize(
                                Location(width - 1, height - 1),
                                2 * width - 1,
                                2 * height - 1
                        )
                    else
                        setStartLocationAndSize(Location( (width - 1) / 2, (height - 1) / 2), width, height)

                }
            }
            when(room) {
                !is Wormhole -> {
                    currentLocation = newLocation
                }
            }
        }
        else // Если ударились в стену
            plan.clear()

    }
}