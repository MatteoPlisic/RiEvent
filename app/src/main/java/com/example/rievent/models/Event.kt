import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint

data class Event(
    @get:Exclude @set:Exclude var id: String? = null,
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val ownerId: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val address: String = "",
    val location: GeoPoint? = null,
    val attendees: List<String> = emptyList(),
    val imageUrl: String? = null,
    val isPublic: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val ownerName: String = "",
    val imageUrls: List<String> = emptyList()
)
