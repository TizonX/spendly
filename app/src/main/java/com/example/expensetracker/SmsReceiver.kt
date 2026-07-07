package com.example.expensetracker

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
class SmsReceiver : BroadcastReceiver() {

    companion object {

        private val _newTodoEvent = MutableSharedFlow<Unit>()

        val newTodoEvent = _newTodoEvent.asSharedFlow()

        suspend fun triggerRefresh() { _newTodoEvent.emit(Unit) }
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {

        val bundle = intent.extras ?: return

        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format") // 👈 important

        for (pdu in pdus) {

            val sms = if (format != null) {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                SmsMessage.createFromPdu(pdu as ByteArray)
            }

            val messageBody = sms.messageBody

            if (UpiParser.isDebit(messageBody)) {

                val amount = UpiParser.extractAmount(messageBody) ?: "0"
                val payee = UpiParser.extractPayee(messageBody) ?: "Unknown"

                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    // ⭐ Smart tag prediction
                    val predictedTag = db.todoDao()
                        .getMostUsedTagForPayee(payee)

                    val finalTag = predictedTag ?: "UPI"

                    db.todoDao().insert(
                        TodoItem(
                            payee = payee,
                            amount = amount,
                            source = "SMS",
                            tag = finalTag,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isEdited = false
                        )
                    )
                    //  Todo created with sms event trigger
                    _newTodoEvent.emit(Unit)
                }

                Toast.makeText(
                    context,
                    "UPI Debit saved!\nPayee: $payee\nAmount: $amount",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
