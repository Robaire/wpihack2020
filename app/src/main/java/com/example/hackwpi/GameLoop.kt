package com.example.hackwpi

import android.util.Log
import com.bose.wearable.BoseWearableException
import com.bose.wearable.sensordata.Quaternion
import com.bose.wearable.sensordata.SensorValue
import com.bose.wearable.services.wearablesensor.SensorConfiguration
import com.bose.wearable.services.wearablesensor.SensorType
import com.bose.wearable.wearabledevice.BaseWearableDeviceListener
import com.bose.wearable.wearabledevice.WearableDevice
import com.bose.wearable.wearabledevice.WearableDeviceListener
import com.bose.wearable.sensordata.Vector



class GameLoop(val wearableDevice: WearableDevice) : Runnable  {

    var accelerometerVector: Vector = Vector(0.0, 0.0, 0.0)
    var orientationQuaternion: Quaternion = Quaternion(0.0, 0.0, 0.0, 1.0)

    override fun run() {

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

        var gameRunning = true
        while(gameRunning){

            Log.i("Accelerometer Vector:", accelerometerVector.toString())
            Log.i("Orientation Quaternion:", orientationQuaternion.toString())
            Thread.sleep(100)
        }
    }
}