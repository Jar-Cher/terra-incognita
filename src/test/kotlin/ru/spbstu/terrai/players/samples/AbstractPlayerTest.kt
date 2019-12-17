package ru.spbstu.terrai.players.samples

import org.junit.Assert.assertEquals
import ru.spbstu.terrai.core.Location
import ru.spbstu.terrai.core.Player
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
        var lab = Labyrinth.createFromFile(fileName)
        val wormholesCopy = lab.wormholeMap.values.toList()
        if (wormholesCopy.isEmpty())
            doTestLab(fileName, expectedResult)
        else {
            val player = createPlayer()
            val first = wormholesCopy[0]
            var possibleWormholeMaps = permutations(wormholesCopy.drop(1)) as List<List<Location>>
            for (i in possibleWormholeMaps) {
                val newWormholeMap = mutableListOf<Location>()
                newWormholeMap.add(first)
                newWormholeMap.addAll(i)
                for (j in newWormholeMap) {
                    //lab.wormholeMap[j] = newWormholeMap[j]
                }
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