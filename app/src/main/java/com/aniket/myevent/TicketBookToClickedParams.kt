package com.aniket.myevent

import com.aniket.myevent.annotations.MyEvent

@MyEvent
data class TicketBookToClickedParams(
    val eventName: String,
    val screenName: String,
    val ticketNumber: Int,
    val ticketAmount: String,
)
