package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.experimental.xor

fun main() {

    while (true) {
        println("Task (hide, show, exit):")
        val action = readln()
        when (action) {
            "exit" -> {
                println("Bye!")
                break
            }
            "hide" -> {
                try {
                    hideMessage()
                } catch (e: IOException) {
                    println(e.message)
                    continue
                }
            }
            "show" -> {
                try {
                    showMessage()
                } catch (e: IOException) {
                    println(e.message)
                    continue
                }
            }
            else -> {
                println("Wrong task: $action")
                continue
            }
        }
    }
}

fun showMessage() {
    println("Input image file:")
    val inputFileName = readln()
    println("Password:")
    val password = readln()

    val inputFile = File(inputFileName)
    val image: BufferedImage = ImageIO.read(inputFile)

    val message = decodeMessageFromImage(image, password)
    println("Message:\n$message")

}

fun decodeMessageFromImage(image: BufferedImage, password: String): String {
    val bitsList = mutableListOf<Int>()
    val bytesList = mutableListOf<Byte>()
    var endOfMessage = false

    for (y in 0 until image.height) {
        if (endOfMessage) break
        for (x in 0 until image.width) {
            val pixelColor = Color(image.getRGB(x, y))
            bitsList.add(pixelColor.blue and 1)

            if (bitsList.size >= 32 && bitsList.size % 8 == 0) endOfMessage = checkForClosingBits(bitsList)
            if (endOfMessage) break
        }
    }

    val numberOfBytes = bitsList.size / 8

    for (i in 1..numberOfBytes - 3) {
        val endIdx = 8 * i
        val startIdx = endIdx - 8
        bytesList.add(bitsList.subList(startIdx, endIdx).joinToString("").toInt(2).toByte())
    }

    return encryptMessage(bytesList.toByteArray(), password).toString(Charsets.UTF_8)

}

fun checkForClosingBits(bitsList: MutableList<Int>): Boolean {
    val listSize = bitsList.size
    val lastThreeBytes = bitsList.subList(listSize - 24, listSize)

    if (lastThreeBytes.joinToString("") == "000000000000000000000011") return true
    return false
}

fun hideMessage() {
    println("Input image file:")
    val inputFileName = readln()
    println("Output image file:")
    val outputFileName = readln()
    println("Message to hide:")
    val message = readln()
    println("Password:")
    val password = readln()

    val inputFile = File(inputFileName)
    val outputFile = File(outputFileName)

    val image: BufferedImage = ImageIO.read(inputFile)

    val imageCapacity = image.width * image.height
    val messageSize = (message.length + 3) * 8

    if (messageSize > imageCapacity) {
        println("The input image is not large enough to hold this message.")
    } else {
        val newImage = encodeMessageToImage(image, message, password)
        ImageIO.write(newImage, "png", outputFile)
        println("Message saved in $outputFileName image.")
    }
}

fun encodeMessageToImage(image: BufferedImage, message: String, password: String): BufferedImage {
    val encryptedArray = encryptMessage(message.toByteArray(), password)
    val closingBytes = byteArrayOf(0, 0, 3)
    val bytesArray = encryptedArray + closingBytes
    var x = 0
    var y = 0

    for (byte in bytesArray) {
        val byteAsStringOfBits = String.format("%8s", Integer.toBinaryString(byte.toInt())).replace(" ", "0")
        for (bit in byteAsStringOfBits) {
            val oldColor = Color(image.getRGB(x, y))
            val r = oldColor.red
            val g = oldColor.green
            var b = oldColor.blue

            if (bit == '1') {
                if (b and 1 == 0) b += 1
            } else {
                if (b and 1 != 0) b -= 1
            }

            val newColor = Color(r, g, b)
            image.setRGB(x, y, newColor.rgb)

            x++
            if (x == image.width) {
                x = 0
                y++
            }
        }
    }
    return image
}

fun encryptMessage(messageByteArray: ByteArray, password: String): ByteArray {
    val passwordByteArray = password.toByteArray()
    var passIdx = 0

    for (i in messageByteArray.indices) {
        if (passIdx > passwordByteArray.lastIndex) passIdx = 0
        messageByteArray[i] = messageByteArray[i] xor passwordByteArray[passIdx]
        passIdx++
    }
    return messageByteArray
}