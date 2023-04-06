package com.timerpicker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var timePicker: SleepTimePicker
    private lateinit var tvBedTime: TextView
    private lateinit var tvWakeTime: TextView
    private lateinit var tvHours: TextView
    private lateinit var tvMins: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main)
        timePicker = findViewById(R.id.timePicker)
        tvBedTime = findViewById(R.id.tvBedTime)
        tvWakeTime = findViewById(R.id.tvWakeTime)
        tvHours = findViewById(R.id.tvHours)
        tvMins = findViewById(R.id.tvMins)

        timePicker.setTime(LocalTime.of(23, 0), LocalTime.of(7, 0))

        timePicker.listener = { bedTime, wakeTime ->
            handleUpdate(bedTime, wakeTime)
        }
        handleUpdate(timePicker.getBedTime(), timePicker.getWakeTime())
    }

    private fun handleUpdate(bedTime: LocalTime, wakeTime: LocalTime) {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        tvBedTime.text = bedTime.format(formatter)
        tvWakeTime.text = wakeTime.format(formatter)

        val bedDate = bedTime.atDate(LocalDate.now())
        var wakeDate = wakeTime.atDate(LocalDate.now())
        if (bedDate >= wakeDate) wakeDate = wakeDate.plusDays(1)
        val duration = Duration.between(bedDate, wakeDate)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        tvHours.text = hours.toString()
        tvMins.text = minutes.toString()
        //if (minutes > 0) llMins.visibility = View.VISIBLE else llMins.visibility = View.GONE
    }
}