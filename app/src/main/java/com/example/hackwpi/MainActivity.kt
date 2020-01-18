package com.example.hackwpi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.bose.blecore.ScanError
import com.bose.blecore.Session
import com.bose.bosewearableui.DeviceConnectorActivity
import com.bose.wearable.BoseWearable
import com.bose.wearable.BoseWearableException
import com.bose.wearable.sensordata.GestureIntent
import com.bose.wearable.sensordata.SensorIntent
import com.bose.wearable.sensordata.SensorValue
import com.bose.wearable.services.wearablesensor.GestureType
import com.bose.wearable.services.wearablesensor.SamplePeriod
import com.bose.wearable.services.wearablesensor.SensorConfiguration
import com.bose.wearable.services.wearablesensor.SensorType
import com.bose.wearable.wearabledevice.BaseWearableDeviceListener
import com.bose.wearable.wearabledevice.WearableDevice
import com.bose.wearable.wearabledevice.WearableDeviceListener
import java.util.*


// This is the activity that is launched when the application is launched
class MainActivity : AppCompatActivity() {

    var editText: EditText? = null
    var sensorText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to be used
        setContentView(R.layout.activity_main)

        // Get view elements
        editText = findViewById(R.id.editText)
        editText?.setText("Default Text")

        sensorText = findViewById(R.id.sensorData)
    }

    // Connect to a Bose Bluetooth device
    fun connect(view: View) {
        editText?.setText("Attempting to connect")

        // Define which sensors we will be using
        val sensorTypes: Array<SensorType> = arrayOf(SensorType.ACCELEROMETER, SensorType.ROTATION_VECTOR)

        // Set the sensors and sample frequency
        val sensorIntent: SensorIntent = SensorIntent(sensorTypes.toSet(), Collections.singleton(SamplePeriod._20_MS))
        val gestureIntent: GestureIntent = GestureIntent(Collections.singleton(GestureType.INPUT))

        // Start the connector activity
        val timeout = 5 // Time in seconds before automatic reconnection timeout
        val intent = DeviceConnectorActivity.newIntent(this, timeout, sensorIntent, gestureIntent)

        startActivityForResult(intent, 1)
    }

    // Handles Connection Activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                val deviceAddress: String = data!!.getStringExtra(DeviceConnectorActivity.CONNECTED_DEVICE)!!
                val btManager: com.bose.blecore.BluetoothManager = BoseWearable.getInstance().bluetoothManager()
                val session: Session = btManager.session(btManager.deviceByAddress(deviceAddress)!!)
                val wearableDevice: WearableDevice = session.device() as WearableDevice

                // Device is connected and ready to use
                editText?.setText("Device connected")

                val listener: WearableDeviceListener = object : BaseWearableDeviceListener() {

                    override fun onSensorConfigurationRead(sensorConfiguration: SensorConfiguration) {}
                    override fun onSensorConfigurationChanged(sensorConfiguration: SensorConfiguration) {}
                    override fun onSensorConfigurationError(wearableException: BoseWearableException) {}

                    override fun onSensorDataRead(sensorValue: SensorValue) {
                        when (sensorValue.sensorType()) {
                            SensorType.ACCELEROMETER -> sensorText?.setText(sensorValue.vector().toString())
                            else -> 0 // Do Nothing
                        }
                    }
                }

                wearableDevice.addListener(listener)

                val samplePeriod: SamplePeriod = SamplePeriod._20_MS

                val configuration: SensorConfiguration = wearableDevice.sensorConfiguration()
                    .disableAll()
                    .enableSensor(SensorType.ACCELEROMETER, samplePeriod)

                wearableDevice.changeSensorConfiguration(configuration)

            } else if (resultCode == DeviceConnectorActivity.RESULT_SCAN_ERROR) {
                val scanError: ScanError = data!!.getSerializableExtra(DeviceConnectorActivity.FAILURE_REASON) as ScanError
                // An error occurred so inform the user
                editText?.setText("Error Occurred")


            } else if (resultCode == Activity.RESULT_CANCELED) {
                // The user cancelled the search operation.
                editText?.setText("User cancelled")

            }
        } else {
            super.onActivityResult(requestCode!!, resultCode!!, data)
        }

    }






}
