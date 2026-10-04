package com.xeg911.appcontrol.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

abstract class BaseViewModel<E> : ViewModel() {

    private val _events = Channel<E>(Channel.BUFFERED)

    val events = _events.receiveAsFlow()

    protected suspend fun sendEvent(event: E) = _events.send(event)

    protected fun trySendEvent(event: E) {
        _events.trySend(event)
    }
}