package com.example.rievent.ui.chat // Or your package

import Chat
import Message
import ParticipantInfo
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

    private val _chatList = MutableStateFlow<List<Chat>>(emptyList())
    val chatList: StateFlow<List<Chat>> = _chatList.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var chatListListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var newChatParticipantDetails: Map<String, ParticipantInfo>? = null

    // [THE FIX] The listener is started here, when the ViewModel is first created.
    // It will remain active across all screens that share this ViewModel.
    init {
        listenForUserChats()
    }

    private fun listenForUserChats() {
        if (currentUserId == null) return
        chatListListener?.remove() // Safety check
        chatListListener = db.collection("chats")
            .whereArrayContains("participantIds", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "ChatList listener error", error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val chatObjects = it.toObjects(Chat::class.java)
                    _chatList.value = chatObjects.mapIndexed { index, chat ->
                        chat.copy(id = it.documents[index].id)
                    }
                    Log.d("ChatViewModel", "Chat list updated via INIT listener with ${_chatList.value.size} chats.")
                }
            }
    }

    // This function can now be removed or kept private, as it's called by init.
    // I will keep it for clarity.

    fun listenForMessages(chatId: String) {
        messagesListener?.remove()
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

    // This simplified sendMessage function is CRITICAL for the whole system to work.
    fun sendMessage(chatId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        val message = Message(senderId = currentUserId, text = text, timestamp = Timestamp.now())
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                val detailsToUse = newChatParticipantDetails ?: return@addOnSuccessListener
                val initialChatData = mapOf(
                    "participantIds" to detailsToUse.keys.toList(),
                    "participantDetails" to detailsToUse
                )
                chatRef.set(initialChatData).addOnSuccessListener {
                    chatRef.collection("messages").add(message)
                    newChatParticipantDetails = null
                }
            } else {
                chatRef.collection("messages").add(message)
            }
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Error sending message", e)
        }
    }

    fun prepareForNewChat(details: Map<String, ParticipantInfo>) {
        newChatParticipantDetails = details
    }

    // onCleared is the correct place to stop the listeners for a ViewModel.
    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        chatListListener?.remove()
        Log.d("ChatViewModel", "ViewModel cleared. All listeners removed.")
    }
}