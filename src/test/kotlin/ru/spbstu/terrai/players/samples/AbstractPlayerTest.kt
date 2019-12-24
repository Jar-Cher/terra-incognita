package ru.spbstu.terrai.players.samples

import org.junit.Assert.assertEquals
import ru.spbstu.terrai.core.Location
import ru.spbstu.terrai.core.Player
import ru.spbstu.terrai.core.Treasure
import ru.spbstu.terrai.core.WithContent
import ru.spbstu.terrai.lab.Controller
import ru.spbstu.terrai.lab.Labyrinth

abstract class AbstractPlayerTest {

    abstract fun createPlayer(): Player

    fun doTestLab(fileName: String, expectedResult: Controller.GameResult) {
        val lab = Labyrinth.createFromFile(fileName)
        val player = createPlayer()
        val controller = Controller(lab, player)
        val actualResult = controller.makeMoves(500)
        assertEquals(controller.playerPath.toString(), expectedResult.exitReached, actualResult.exitReached)
        if (expectedResult.exitReached && actualResult.exitReached && expectedResult.moves >= 0) {
            assertEquals(controller.playerPath.toString(), expectedResult.moves, actualResult.moves)
        }
    }

    fun doTestLab(lab: Labyrinth, expectedResult: Controller.GameResult) {
        val player = createPlayer()
        val controller = Controller(lab, player)
        val actualResult = controller.makeMoves(500)
        assertEquals(controller.playerPath.toString(), expectedResult.exitReached, actualResult.exitReached)
        if (expectedResult.exitReached && actualResult.exitReached && expectedResult.moves >= 0) {
            assertEquals(controller.playerPath.toString(), expectedResult.moves, actualResult.moves)
        }
    }

    fun doFullTestLab(fileName: String, expectedResult: Controller.GameResult) {
        val wormholesCopy = Labyrinth.createFromFile(fileName).wormholeMap.values.toList()
        //val treasures = lab.map.filter { (_, room) -> room == WithContent(Treasure)}.keys
        if (wormholesCopy.isEmpty())
            doTestLab(fileName, expectedResult)
        else {
            val first = wormholesCopy[0]
            val possibleWormholeMaps = permutations(wormholesCopy.drop(1)) as List<List<Location>>
            for (i in possibleWormholeMaps) {
                val newWormholeMap = mutableListOf<Location>()
                newWormholeMap.add(first)
                newWormholeMap.addAll(i)
                val lab = Labyrinth.createFromFile(fileName)
                /*for (j in treasures)
                    lab.map[j] = WithContent(Treasure)*/
                for (j in newWormholeMap.indices)
                    lab.wormholeMap[newWormholeMap[j]] = newWormholeMap[(j + 1) % newWormholeMap.size]
                val player = createPlayer()
                val controller = Controller(lab, player)
                println("\n\n")
                println(lab.wormholeMap)
                val actualResult = controller.makeMoves(500)
                assertEquals(controller.playerPath.toString(), expectedResult.exitReached, actualResult.exitReached)
            }
        }
    }

    fun permutations(input: List<Any>) : List<List<Any>> {
        return if (input.size <= 1)
            mutableListOf(input.toMutableList())
        else {
            val ans= mutableListOf<List<Any>>()
            val elementToMove = input[0]
            for (i in permutations(input.drop(1)))
                for (j in 0..i.size) {
                    val newPermutation= i.toMutableList()
                    newPermutation.add(j, elementToMove)
                    ans.add(newPermutation.toList())
                }
            return ans
        }
    }

}