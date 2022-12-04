package com.udacity.project4.locationreminders.savereminder

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Result
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var  checkLocationSettings: ActivityResultLauncher<IntentSenderRequest>
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        setDisplayHomeAsUpEnabled(true)

        checkLocationSettings =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    saveReminder()
                }else{
                    _viewModel.showErrorMessage.value = requireActivity().getString(R.string.error_adding_geofence)
                }
            }

        locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[permission.ACCESS_BACKGROUND_LOCATION] == true
                && permissions[permission.ACCESS_FINE_LOCATION] == true) {
                saveReminder()
            } else {
                _viewModel.showErrorMessage.value =
                    requireActivity().getString(R.string.cant_save_reminder)
            }
        }

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            saveReminder()
        }
    }

    @SuppressLint("InlinedApi")
    private fun saveReminder() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        val reminder = ReminderDataItem(
            title,
            description, location, latitude, longitude
        )
        val valid = _viewModel.validateEnteredData(reminder)
        if (valid) {
            if (isBackgroundLocationAndForeGroundPermissionsIsGranted()) {
                checkDeviceLocationSettingsAndSaveReminder(getGeofencingRequest(createGeoFenceObject(reminder)),reminder)
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_BACKGROUND_LOCATION)) {
                    showBackgroundLocationPermissionDialog()
                } else {
                    locationPermissionRequest.launch(
                        arrayOf(permission.ACCESS_BACKGROUND_LOCATION,
                        permission.ACCESS_FINE_LOCATION)
                    )
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun showBackgroundLocationPermissionDialog() {
        AlertDialog.Builder(requireActivity())
            .setMessage(requireActivity().getString(R.string.bg_location_dialog_rationale_msg))
            .setPositiveButton(
                requireActivity().getString(R.string.accept)
            ) { _, _ ->
                locationPermissionRequest.launch(
                    arrayOf(permission.ACCESS_BACKGROUND_LOCATION,
                    permission.ACCESS_FINE_LOCATION)
                )
            }
            .setNegativeButton(requireActivity().getString(R.string.deny)) { _, _ ->
                _viewModel.showErrorMessage.value =
                    requireActivity().getString(R.string.cant_save_reminder)
            }
            .show()
    }

    private fun isBackgroundLocationAndForeGroundPermissionsIsGranted(): Boolean {
        val foregroundPermissionGranted = ActivityCompat.checkSelfPermission(
            requireActivity(),
            permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundPermissionGranted =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                requireActivity(),
                permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return foregroundPermissionGranted && backgroundPermissionGranted
    }
    private fun checkDeviceLocationSettingsAndSaveReminder(geofencingRequest: GeofencingRequest, reminder: ReminderDataItem) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                try {

                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    checkLocationSettings.launch(intentSenderRequest)

                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
        locationSettingsResponseTask.addOnSuccessListener {
            addGeofenceAndSaveReminder(geofencingRequest, reminder)
        }
        locationSettingsResponseTask.addOnCanceledListener{
            _viewModel.showErrorMessage.value = requireActivity().getString(R.string.error_adding_geofence)
        }
    }

    private fun createGeoFenceObject(reminderDataItem: ReminderDataItem): Geofence {
        return Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(reminderDataItem.id)

            // Set the circular region of this geofence.
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GeofenceConstants.GEOFENCE_RADIUS_IN_METERS
            )

            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

            // Create the geofence.
            .build()
    }


    private fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()
    }
    @SuppressLint("MissingPermission")
    private fun addGeofenceAndSaveReminder(geofenceRequest: GeofencingRequest, reminderDataItem: ReminderDataItem){
        geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                _viewModel.saveReminder(reminderDataItem)
            }
            addOnFailureListener {
                _viewModel.showErrorMessage.value =
                    requireActivity().getString(R.string.error_adding_geofence)
            }
        }
    }
    private fun removeGeofence(){
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
    override fun onDestroy() {
        super.onDestroy()
        removeGeofence()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }


}
