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
        doTestLab("labyrinths/lab2.txt", Controller.GameResult(48, exitReached = true))
        println("2-over")
        println()
    }

    @Test
    fun testLab3() {
        doTestLab("labyrinths/lab3.txt", Controller.GameResult(9, exitReached = true))
        println("3-over")
        println()
    }

    @Test
    fun testLab4() {
        doTestLab("labyrinths/lab4.txt", Controller.GameResult(43, exitReached = true))
        println("4-over")
        println()
    }

    @Test
    fun testLab5() {
        doTestLab("labyrinths/lab5.txt", Controller.GameResult(100, exitReached = false))
        println("5-over")
        println()
    }

    @Test
    fun testLab6() {
        doTestLab("labyrinths/lab6.txt", Controller.GameResult(100, exitReached = false))
        println("6-over")
        println()
    }

    @Test
    fun testLab7() {
        doTestLab("labyrinths/lab7.txt", Controller.GameResult(100, exitReached = false))
        println("7-over")
        println()
    }
}