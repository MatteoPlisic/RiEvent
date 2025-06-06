// in Chat.kt
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Chat(
    @get:Exclude var id: String = "", // The document ID
    val participantIds: List<String> = emptyList(),
    val participantDetails: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessageText: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageTimestamp: Timestamp? = null
)

data class ParticipantInfo(
    val name: String = "",
    val imageUrl: String? = null
)

data class Message(
    @get:Exclude var id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)