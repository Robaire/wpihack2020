package com.example.hackwpi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bose.blecore.ScanError
import com.bose.blecore.Session
import com.bose.bosewearableui.DeviceConnectorActivity
import com.bose.wearable.BoseWearable
import com.bose.wearable.BoseWearableException
import com.bose.wearable.sensordata.*
import com.bose.wearable.sensordata.Vector
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

    // Create Connection View Elements
    var connectButton: Button? = null
    var connectStatus: TextView? = null
    var startGame: Button? = null

    // Create Game View Elements
    var acceleration: TextView? = null
    var orientation: TextView? = null
    var accelerationLayout: LinearLayout? = null
    var orientationLayout: LinearLayout? = null

    var accelerations: Array<TextView?> = arrayOfNulls(3)
    var orientations: Array<TextView?> = arrayOfNulls(4)


    // Global Values for Sensor Data
    var accelerometerVector = Vector(0.0, 0.0, 0.0)
    var orientationQuaternion = Quaternion(0.0, 0.0, 0.0, 1.0)

    //
    var gameLoop: GameLoop? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content view to be used
        setContentView(R.layout.activity_main)

        // Get Connection View Elements
        connectButton = findViewById(R.id.connectButton)
        connectStatus = findViewById(R.id.connectStatus)
        startGame = findViewById(R.id.startGame)

        // Get Game View Elements
        acceleration = findViewById(R.id.acceleration)
        orientation = findViewById(R.id.orientation)
        accelerationLayout = findViewById(R.id.acceleration_layout)
        orientationLayout = findViewById(R.id.orientation_layout)

        accelerations = arrayOf(findViewById(R.id.accelX), findViewById(R.id.accelY), findViewById(R.id.accelZ))
        orientations = arrayOf(findViewById(R.id.orientX), findViewById(R.id.orientY), findViewById(R.id.orientZ), findViewById(R.id.orientW))

    }

    // Connect to a Bose Bluetooth device
    fun connect(view: View) {
        connectStatus?.text = "Attempting to connect..."

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
                connectStatus?.text = "Device Connected!"


                // Create a listener for sensor data
                val listener: WearableDeviceListener = object : BaseWearableDeviceListener() {

                    override fun onSensorConfigurationRead(sensorConfiguration: SensorConfiguration) {}
                    override fun onSensorConfigurationChanged(sensorConfiguration: SensorConfiguration) {}
                    override fun onSensorConfigurationError(wearableException: BoseWearableException) {}

                    override fun onSensorDataRead(sensorValue: SensorValue) {
                        when (sensorValue.sensorType()) {
                            SensorType.ACCELEROMETER -> {
                                accelerometerVector = sensorValue.vector() ?: Vector(0.0, 0.0, 0.0)
                            }
                            SensorType.GAME_ROTATION_VECTOR -> {
                                orientationQuaternion = sensorValue.quaternion()!! //Lets hope this is never null
                            }
                            else -> {} // Do Nothing
                        }
                    }
                }

                // Bind the listener
                wearableDevice.addListener(listener)

                // Update the sensor settings
                val samplePeriod: SamplePeriod = SamplePeriod._20_MS
                val configuration: SensorConfiguration = wearableDevice.sensorConfiguration()
                    .disableAll()
                    .enableSensor(SensorType.ACCELEROMETER, samplePeriod)
                    .enableSensor(SensorType.GAME_ROTATION_VECTOR, samplePeriod)
                wearableDevice.changeSensorConfiguration(configuration)

                // Make the Start Game button Visible
                startGame?.visibility = View.VISIBLE

                gameLoop = GameLoop(wearableDevice)

            } else if (resultCode == DeviceConnectorActivity.RESULT_SCAN_ERROR) {
                val scanError: ScanError = data!!.getSerializableExtra(DeviceConnectorActivity.FAILURE_REASON) as ScanError
                // An error occurred so inform the user
                connectStatus?.text = "Connection Error"



            } else if (resultCode == Activity.RESULT_CANCELED) {
                // The user cancelled the search operation.
                connectStatus?.text = "User Closed Connection"


            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    fun startGameLoop(view: View){

        // Indicate the game loop has begun
        connectStatus?.text = "Game Running"


        // Hide the Connection View Elements
        connectButton?.visibility = View.INVISIBLE
        startGame?.visibility = View.INVISIBLE

        // Make the sensor readings visible
        acceleration?.visibility = View.VISIBLE
        orientation?.visibility = View.VISIBLE
        accelerationLayout?.visibility = View.VISIBLE
        orientationLayout?.visibility = View.VISIBLE

        // Enter the main game loop
        //val gameLoop = GameLoop(wearableDevice)
        Thread(gameLoop).start()


//        var gameRunning = true
//        while(gameRunning) {
//
////            orientations[0]?.text = "REEEE"
//
//            // Update the sensor display with sensor readings
//            orientations[0]?.text = orientationQuaternion.x().toString()
//            orientations[1]?.text = orientationQuaternion.y().toString()
//            orientations[2]?.text = orientationQuaternion.z().toString()
//            orientations[3]?.text = orientationQuaternion.w().toString()
//
//            accelerations[0]?.text = accelerometerVector.x().toString()
//            accelerations[1]?.text = accelerometerVector.y().toString()
//            accelerations[2]?.text = accelerometerVector.z().toString()
//
//            SystemClock.sleep(100)
//            //gameRunning = false
//        }


    }



}
