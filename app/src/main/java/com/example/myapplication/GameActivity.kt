package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Убедись, что контекст передаётся правильно
        val gameView = GameView(this)
        setContentView(gameView)

        // Теперь запускаем игру после установки контента
        gameView.post {
            gameView.startGame()  // Запуск игры после того, как view будет готово
        }
    }
}


class GameView(context: Context) : SurfaceView(context), Runnable {
    private val playerRect = Rect(400, 1000, 500, 1050) // Игрок
    private val fallingObjects = mutableListOf<FallingObject>() // Список падающих объектов
    private var gameThread: Thread? = null
    private var isRunning = false
    private var score = 0 // Счет игры
    private val surfaceHolder: SurfaceHolder = holder

    init {
        isFocusable = true
        gameThread = Thread(this)
    }

    override fun run() {
        while (isRunning) {
            update()
            val canvas: Canvas? = surfaceHolder.lockCanvas()
            canvas?.let {
                synchronized(surfaceHolder) {
                    drawFrame(it)
                }
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
            try {
                Thread.sleep(16) // 60 FPS
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun update() {
        // Добавляем новые объекты
        if (Random.nextInt(100) < 5) {
            val x = Random.nextInt(0, width - 100)
            val newObject = FallingObject(x, 0, 100, 100) // Создаем падающий прямоугольник
            fallingObjects.add(newObject)
        }

        // Обновляем позиции падающих объектов
        for (obj in fallingObjects) {
            obj.y += 10
            obj.rect.top = obj.y // Обновляем координату "top" для Rect
            obj.rect.bottom = obj.rect.top + obj.height // Обновляем координату "bottom" для Rect
        }

        // Удаляем объекты, которые вышли за экран
        fallingObjects.removeAll { it.y > height }

        // Проверка на столкновение с игроком
        for (obj in fallingObjects) {
            if (Rect.intersects(obj.rect, playerRect)) {
                score += 10 // Добавляем очки за ловлю объекта
                fallingObjects.remove(obj) // Убираем пойманный объект
                break // Прерываем цикл после удаления объекта
            }
        }
    }

    private fun drawFrame(canvas: Canvas) {
        // Отрисовываем фон
        canvas.drawColor(Color.WHITE)

        // Отрисовываем игрока
        val paint = Paint()
        paint.color = Color.BLUE
        canvas.drawRect(playerRect, paint)

        // Отрисовываем падающие объекты как красные прямоугольники
        paint.color = Color.RED
        for (obj in fallingObjects) {
            canvas.drawRect(obj.rect, paint) // Отрисовываем прямоугольники
        }

        // Отображаем счет
        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Score: $score", 20f, 50f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                // Перемещаем игрока по экрану, используя координаты касания
                val newLeft = event.x.toInt() - playerRect.width() / 2
                // Ограничиваем движение игрока, чтобы он не выходил за границы экрана
                val newRight = newLeft + playerRect.width()
                playerRect.left = newLeft.coerceIn(0, width - playerRect.width())
                playerRect.right = newRight.coerceIn(playerRect.width(), width)
            }
        }
        return true
    }

    fun startGame() {
        isRunning = true
        gameThread?.start()
    }

    fun stopGame() {
        isRunning = false
        gameThread?.join()
    }
}

data class FallingObject(val x: Int, var y: Int, val width: Int, val height: Int) {
    var rect = Rect(x, y, x + width, y + height)
}


