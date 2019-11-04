package ru.spbstu.terrai.players.samples

import org.junit.Test
import ru.spbstu.terrai.lab.Controller

class VindictiveTest : AbstractPlayerTest() {

    override fun createPlayer() = Vindictive()

    @Test
    fun testLab1() {
        doTestLab("labyrinths/lab1.txt", Controller.GameResult(4, exitReached = true))
        println("1-over")
        println()
    }

    @Test
    fun testLab2() {
        doTestLab("labyrinths/lab2.txt", Controller.GameResult(34, exitReached = true))
        println("2-over")
        println()
    }

    @Test
    fun testLab3() {
        doTestLab("labyrinths/lab3.txt", Controller.GameResult(31, exitReached = true))
        println("3-over")
        println()
    }

    @Test
    fun testLab4() {
        doTestLab("labyrinths/lab4.txt", Controller.GameResult(83, exitReached = true))
        println("4-over")
        println()
    }

    @Test
    fun testLab5() {
        doTestLab("labyrinths/lab5.txt", Controller.GameResult(135, exitReached = true))
        println("5-over")
        println()
    }

    @Test
    fun testLab6() {
        doTestLab("labyrinths/lab6.txt", Controller.GameResult(198, exitReached = true))
        println("6-over")
        println()
    }

    @Test
    fun testLab7() {
        doTestLab("labyrinths/lab7.txt", Controller.GameResult(59, exitReached = true))
        println("7-over")
        println()
    }

    @Test
    fun testMaze() {
        doTestLab("labyrinths/maze.txt", Controller.GameResult(283, exitReached = true))
        println("maze-over")
        println()
    }

    @Test
    fun testSharp() {
        doTestLab("labyrinths/sharp.txt", Controller.GameResult(100500, exitReached = false))
        println("sharp-over")
        println()
    }

    @Test
    fun testSharp1210() {
        doTestLab("labyrinths/sharp1210.txt", Controller.GameResult(24, exitReached = true))
        println("sharp1210-over")
        println()
    }

    @Test
    fun testCave() {
        doTestLab("labyrinths/cave.txt", Controller.GameResult(100500, exitReached = false))
        println("cave-over")
        println()
    }

    @Test
    fun testNightmare() {
        doTestLab("labyrinths/nightmare.txt", Controller.GameResult(83, exitReached = true))
        println("nightmare-over")
        println()
    }

    @Test
    fun testPhobia1() {
        doTestLab("labyrinths/phobia1.txt", Controller.GameResult(31, exitReached = true))
        println("phobia1-over")
        println()
    }

    @Test
    fun testPhobia2() {
        doTestLab("labyrinths/phobia2.txt", Controller.GameResult(2, exitReached = true))
        println("phobia2-over")
        println()
    }

    @Test
    fun testPhobia3() {
        doTestLab("labyrinths/phobia3.txt", Controller.GameResult(312, exitReached = true))
        println("phobia3-over")
        println()
    }
}