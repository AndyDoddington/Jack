import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        // Jump to the current indicator position
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        // Set the gestures plugin's focal point to the current indicator location.
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationPermissionHelper = LocationPermissionHelper(this)
        locationPermissionHelper.checkPermissions {
            mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { _ ->
                // Disable scroll gesture, since we are updating the camera position based on the indicator location.
                mapView.gestures.scrollEnabled = false
                mapView.gestures.addOnMapClickListener { point ->
                    mapView.location
                        .isLocatedAt(point) { isPuckLocatedAtPoint ->
                            if (isPuckLocatedAtPoint) {
                                Toast.makeText(this, "Clicked on location puck", Toast.LENGTH_SHORT).show()
                            }
                        }
                    true
                }
                mapView.gestures.addOnMapLongClickListener { point ->
                    mapView.location
                        .isLocatedAt(point) { isPuckLocatedAtPoint ->
                            if (isPuckLocatedAtPoint) {
                                Toast.makeText(this, "Long-clicked on location puck", Toast.LENGTH_SHORT).show()
                            }
                        }
                    true
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapView.location
            .addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}

class LocationPermissionHelper(val activity: AppCompatActivity) {
    private lateinit var permissionsManager: PermissionsManager

    fun checkPermissions(onMapReady: () -> Unit) {
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            onMapReady()
        } else {
            permissionsManager = PermissionsManager(object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) {
                    Toast.makeText(
                        activity, "You need to accept location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        onMapReady()
                    } else {
                        activity.finish()
                    }
                }
            })
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
