// in ui/chat/ChatViewModel.kt
// ... import your data classes
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    // For the ChatListScreen
    private val _chatList = MutableStateFlow<List<Chat>>(emptyList())
    val chatList: StateFlow<List<Chat>> = _chatList.asStateFlow()

    // For the ConversationScreen
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var newChatParticipantDetails: Map<String, ParticipantInfo>? = null
    private var messagesListener: ListenerRegistration? = null

    init {
        loadUserChats()
    }

    fun prepareForNewChat(details: Map<String, ParticipantInfo>) {
        newChatParticipantDetails = details
    }

    private fun loadUserChats() {
        if (currentUserId == null) return
        db.collection("chats")
            .whereArrayContains("participantIds", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _chatList.value = it.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    fun listenForMessages(chatId: String) {
        messagesListener?.remove() // Stop listening to previous chat
        messagesListener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _messages.value = it.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    fun sendMessage(chatId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        val message = Message(
            senderId = currentUserId,
            text = text,
            timestamp = Timestamp.now()
        )

        val chatRef = db.collection("chats").document(chatId)

        // First, check if the main chat document exists.
        chatRef.get().addOnSuccessListener { documentSnapshot ->
            db.runBatch { batch ->
                val chatData: Map<String, Any>

                if (!documentSnapshot.exists()) {
                    // --- CASE 1: THIS IS A BRAND NEW CHAT ---
                    // The main chat document does not exist. We must create it.
                    // This will only happen when sending the very first message.
                    val detailsToUse = newChatParticipantDetails
                    if (detailsToUse == null) {
                        // This is a critical error state. We cannot create a chat without participant details.
                        // This might happen if the user navigates away from the profile screen before sending a message.
                        Log.e("ChatViewModel", "Cannot create a new chat ($chatId) without participant details.")
                        return@runBatch // Abort the batch
                    }

                    chatData = mapOf(
                        "participantIds" to detailsToUse.keys.toList(),
                        "participantDetails" to detailsToUse,
                        "lastMessageText" to text,
                        "lastMessageSenderId" to currentUserId,
                        "lastMessageTimestamp" to message.timestamp as Any
                    )
                    // Set the full data for the new chat document.
                    batch.set(chatRef, chatData)

                    // Clear the temporary details after use.
                    newChatParticipantDetails = null

                } else {
                    // --- CASE 2: THIS IS AN EXISTING CHAT ---
                    // The main chat document already exists. We only need to update the 'last message' fields.
                    chatData = mapOf(
                        "lastMessageText" to text,
                        "lastMessageSenderId" to currentUserId,
                        "lastMessageTimestamp" to message.timestamp as Any
                    )
                    // Update the existing document with the new 'last message' info.
                    batch.update(chatRef, chatData)
                }

                // --- THIS RUNS FOR BOTH CASES ---
                // Now that we are sure the parent 'chat' document exists (or will exist),
                // we can safely add the new message to its subcollection.
                val newMessageRef = chatRef.collection("messages").document()
                batch.set(newMessageRef, message)

            }.addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed to send message for chat $chatId", e)
            }
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Failed to check for existence of chat $chatId", e)
        }
    }

    fun getOtherParticipantName(chatId: String): String {
        val currentUserId = auth.currentUser?.uid
        // Find the chat in the list we already have.
        val chat = _chatList.value.find { it.id == chatId }

        if (chat == null || currentUserId == null) {
            return "Chat" // Return a default name if chat or user is not found
        }

        // Find the ID of the other participant
        val otherParticipantId = chat.participantIds.firstOrNull { it != currentUserId }

        // Get the name from the details map using the other participant's ID
        return otherParticipantId?.let {
            chat.participantDetails[it]?.name
        } ?: "Unknown User" // Fallback if the participant info is missing for some reason
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}