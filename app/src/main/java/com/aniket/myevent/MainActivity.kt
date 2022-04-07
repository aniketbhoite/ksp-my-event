package com.aniket.myevent

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bookTicketButton: Button = findViewById(R.id.bookTicketButton)

        bookTicketButton.setOnClickListener {
            EventUtils.postEvent(
                TicketBookToClickedParamsEvent(
                    TicketBookToClickedParams(
                        eventName = "TicketBookClicked",
                        screenName = "MainActivity",
                        ticketNumber = 1223,
                        ticketAmount = "1220"
                    )
                )
            )
        }
    }
}