import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Event(
    val id: String? = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val ownerId: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val address: String = "",
    val location: GeoPoint? = null,              // Geographical coordinates
    val attendees: List<String> = emptyList(),   // List of user IDs
    val imageUrl: String? = null,
    val isPublic: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
)
