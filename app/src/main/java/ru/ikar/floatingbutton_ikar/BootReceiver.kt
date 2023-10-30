package ru.ikar.floatingbutton_ikar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

// Извлекаем и обрабатываем сообщения бродкаста (интенты) от системы и сторонних приложений.
class BootReceiver : BroadcastReceiver() {

    private val INTENT_NAME = "FivePointReceiver"

    // Вызываем метод, когда приходит интент от бродкаста.
    override fun onReceive(context: Context?, intent: Intent?) {

        /*
         Ждем интент, оповещающий о запуске устройства (после перезагрузки) - Intent.ACTION_BOOT_COMPLETED,
         отсеивая остальные

         */
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {

            // Если загрузка устройства завершена, создаём интент для запуска
            // кода в классе FloatingButtonService
            val serviceIntent = Intent(context, FloatingButtonService::class.java)
            // Используем предоставленный контекст для старта класса FloatingButtonService.
            // Это гарантирует, что сервис запущен сразу после перезагрузки устройства.
            context?.startService(serviceIntent)
        }


    }
}