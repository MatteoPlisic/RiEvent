import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem


data class EventClusterItem(
    val event: Event
) : ClusterItem {

    private val position: LatLng =
        event.location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)

    private val title: String = event.name
    private val snippet: String? = event.category

    override fun getPosition(): LatLng = position
    override fun getTitle(): String = title
    override fun getSnippet(): String? = snippet


    override fun getZIndex(): Float? = 0f
}