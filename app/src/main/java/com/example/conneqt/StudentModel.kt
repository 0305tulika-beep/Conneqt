package com.example.conneqt

data class StudentModel(
    val id: String,
    val name: String,
    val studentNo: String,
    val phone: String,
    val email: String,
    val smsStatus: SmsStatus = SmsStatus.NONE
)

// Every possible SMS status for a student
enum class SmsStatus {
    NONE,       // no message sent yet
    SENT,       // message sent, not delivered
    DELIVERED,  // delivered to phone
    READ,       // student opened it
    REPLIED     // student replied
}