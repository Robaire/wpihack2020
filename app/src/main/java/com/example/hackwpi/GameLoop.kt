package com.example.hackwpi

import android.content.Context
import android.util.Log
import com.bose.wearable.BoseWearableException
import com.bose.wearable.sensordata.GestureData
import com.bose.wearable.sensordata.Quaternion
import com.bose.wearable.sensordata.SensorValue
import com.bose.wearable.sensordata.Vector
import com.bose.wearable.services.wearablesensor.GestureConfiguration
import com.bose.wearable.services.wearablesensor.GestureType
import com.bose.wearable.services.wearablesensor.SensorConfiguration
import com.bose.wearable.services.wearablesensor.SensorType
import com.bose.wearable.wearabledevice.BaseWearableDeviceListener
import com.bose.wearable.wearabledevice.WearableDevice
import com.bose.wearable.wearabledevice.WearableDeviceListener
import com.google.vr.sdk.audio.GvrAudioEngine
import com.google.vr.sdk.audio.GvrAudioEngine.MaterialName.ACOUSTIC_CEILING_TILES
import com.google.vr.sdk.audio.GvrAudioEngine.MaterialName.METAL

class GameLoop(val wearableDevice: WearableDevice, val context: Context) : Runnable  {

    // Sensor Data
    var accelerometerVector: Vector = Vector(0.0, 0.0, 0.0)
    var orientationQuaternion: Quaternion = Quaternion(0.0, 0.0, 0.0, 1.0)
    var hasTap = false
    var hasNod = false



    // Audio Stuff
    val gvrAudioEngine = GvrAudioEngine(context, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY)
    val soundFile = "audio/water_drop.ogg"
    val sourceId = gvrAudioEngine.createSoundObject(soundFile)
    val introId = gvrAudioEngine.createSoundObject("audio/narrator/audio-1.ogg")
    val targetPosition = arrayOf(25f, 45f, 0f)

    override fun run() {

        // Create the sensor and gesture listeners
        createListeners()





        gvrAudioEngine.playSound(introId, false)



        var gameRunning = true


        while(gameRunning){

            Log.i("Accelerometer Vector:", accelerometerVector.toString())
            Log.i("Orientation Quaternion:", orientationQuaternion.toString())

            gvrAudioEngine.setHeadRotation(orientationQuaternion.x().toFloat(), orientationQuaternion.y().toFloat(), orientationQuaternion.z().toFloat(), orientationQuaternion.w().toFloat())
            Thread.sleep(10)
        }
    }

    private fun createListeners() {

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

        val gestureListener: WearableDeviceListener = object : BaseWearableDeviceListener() {
            override fun onGestureConfigurationRead(gestureConfiguration: GestureConfiguration) {}
            override fun onGestureConfigurationChanged(gestureConfiguration: GestureConfiguration) {}
            override fun onGestureConfigurationError(wearableException: BoseWearableException) {}

            override fun onGestureDataRead(gestureData: GestureData) {
                when (gestureData.type()) {
                    GestureType.SINGLE_TAP -> {hasTap = true}
                    GestureType.DOUBLE_TAP -> {hasTap = true}
                    GestureType.HEAD_NOD -> {hasNod = true}
                }
            }
        }

        // Bind the listeners
        wearableDevice.addListener(listener)
        wearableDevice.addListener(gestureListener)

        // Setup Audio Playback Thread
        Thread(Runnable {
            gvrAudioEngine.preloadSoundFile(soundFile)
            gvrAudioEngine.setSoundObjectPosition(sourceId, targetPosition[0], targetPosition[1], targetPosition[2])
            gvrAudioEngine.playSound(sourceId, true)
        }).start()
        audioSetup()

        val config: GestureConfiguration = wearableDevice.gestureConfiguration()
            .disableAll()
            .gestureEnabled(GestureType.SINGLE_TAP, true)
            .gestureEnabled(GestureType.DOUBLE_TAP, true)
            .gestureEnabled(GestureType.HEAD_NOD, true)
        wearableDevice.changeGestureConfiguration(config)
    }

    private fun audioSetup() {
        gvrAudioEngine.enableRoom(true)
        gvrAudioEngine.setRoomProperties(
            50f,
            50f,
            50f,
            METAL,
            ACOUSTIC_CEILING_TILES,
            ACOUSTIC_CEILING_TILES
        )
        gvrAudioEngine.setRoomReverbAdjustments(0.4f, 0.6f, 1f)
        gvrAudioEngine.setHeadPosition(25f, 25f, 0.0f)
        gvrAudioEngine.setHeadRotation(0.0f, 0.0f, 0.0f, 1.0f)
        gvrAudioEngine.update()
    }
}


