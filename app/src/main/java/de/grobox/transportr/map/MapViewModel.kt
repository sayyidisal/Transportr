package de.grobox.transportr.map

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import de.grobox.transportr.R
import de.grobox.transportr.TransportrApplication
import de.grobox.transportr.data.locations.FavoriteLocation
import de.grobox.transportr.data.locations.LocationRepository
import de.grobox.transportr.data.searches.SearchesRepository
import de.grobox.transportr.favorites.trips.SavedSearchesViewModel
import de.grobox.transportr.locations.WrapLocation
import de.grobox.transportr.networks.TransportNetworkManager
import de.grobox.transportr.utils.IntentUtils
import de.grobox.transportr.utils.SingleLiveEvent
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MapViewModel @Inject internal constructor(
        application: TransportrApplication,
        transportNetworkManager: TransportNetworkManager,
        locationRepository: LocationRepository,
        searchesRepository: SearchesRepository,
        val gpsController: GpsController) : SavedSearchesViewModel(application, transportNetworkManager, locationRepository, searchesRepository) {

    private val peekHeight = MutableLiveData<Int>()
    private val selectedLocationClicked = MutableLiveData<LatLng>()
    private val updatedLiveBounds = MutableLiveData<LatLngBounds>()
    private val selectedLocation = MutableLiveData<WrapLocation>()
    private val findNearbyStations = SingleLiveEvent<WrapLocation>()
    private val nearbyStationsFound = SingleLiveEvent<Boolean>()

    val isFreshStart = MutableLiveData<Boolean>()
    val mapClicked = SingleLiveEvent<Void>()
    val markerClicked = SingleLiveEvent<Void>()
    val liveBounds: LiveData<LatLngBounds> = Transformations.switchMap<List<FavoriteLocation>, LatLngBounds>(locations, this::switchMap)

    init {
        this.isFreshStart.value = true
    }

    fun getPeekHeight(): LiveData<Int> {
        return peekHeight
    }

    fun setPeekHeight(peekHeight: Int) {
        this.peekHeight.value = peekHeight
    }

    fun getSelectedLocationClicked(): LiveData<LatLng> {
        return selectedLocationClicked
    }

    fun selectedLocationClicked(latLng: LatLng) {
        selectedLocationClicked.value = latLng
        // reset the selected location right away, observers will ignore this update
        selectedLocationClicked.value = null
    }

    fun selectLocation(location: WrapLocation?) {
        selectedLocation.value = location
        // reset the selected location right away, observers will ignore this update
        selectedLocation.value = null
    }

    fun getSelectedLocation(): LiveData<WrapLocation> {
        return selectedLocation
    }

    fun findNearbyStations(location: WrapLocation) {
        findNearbyStations.value = location
    }

    fun getFindNearbyStations(): LiveData<WrapLocation> {
        return findNearbyStations
    }

    fun setNearbyStationsFound(found: Boolean) {
        nearbyStationsFound.value = found
    }

    fun nearbyStationsFound(): LiveData<Boolean> {
        return nearbyStationsFound
    }

    fun setGeoUri(geoUri: Uri) {
        val location = IntentUtils.getWrapLocation(geoUri.toString())
        if (location != null) {
            selectLocation(location)
        } else {
            Log.w(MapViewModel::class.java.simpleName, "Invalid geo intent: " + geoUri.toString())
            Toast.makeText(getApplication<Application>().applicationContext, R.string.error_geo_intent, Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchMap(input: List<FavoriteLocation>?): MutableLiveData<LatLngBounds> {
        if (input == null) {
            updatedLiveBounds.setValue(null)
        } else {
            val points = input
                    .filter { it.hasLocation() }
                    .map { it.latLng as LatLng }
                    .toMutableSet()
            val gpsLocation = gpsController.getWrapLocation()
            if (gpsLocation != null && gpsLocation.hasLocation()) points.add(gpsLocation.latLng)
            if (points.size < 2) {
                updatedLiveBounds.setValue(null)
            } else {
                updatedLiveBounds.setValue(LatLngBounds.Builder().includes(ArrayList(points)).build())
            }
        }
        return updatedLiveBounds
    }

}
