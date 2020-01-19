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
import com.google.vr.sdk.audio.GvrAudioEngine.MaterialName.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

class GameLoop(val wearableDevice: WearableDevice, val context: Context) : Runnable  {

    // Sensor Data
    var accelerometerVector: Vector = Vector(0.0, 0.0, 0.0)
    var orientationQuaternion: Quaternion = Quaternion(0.0, 0.0, 0.0, 1.0)

    var initialYawAngle = 0.0
    var currentYawAngle = 0.0

    val timestep = .02
    var accelerationX = 0.0
    var accelerationY = 0.0
    var velocityX = 0.0
    var velocityY = 0.0
    var positionX = 25.0
    var positionY = 25.0

    var hasTap = false
    var hasNod = false

    var beginGame = false

        // Audio Stuff
//    val gvrAudioEngine = GvrAudioEngine(context, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY)
//    val soundFile = "audio/water_drop_2.ogg"
//    val sourceId = gvrAudioEngine.createSoundObject(soundFile)
//    val targetPosition = arrayOf(25f, 45f, 0f)


    val narratorAudioEngine = GvrAudioEngine(context, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY)
    val ambientAudioEngine = GvrAudioEngine(context, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY)

    override fun run() {

        // Create the sensor and gesture listeners
        createListeners()

        // Create Audio Stuff
        val narratorAudioIds = createNarratorAudio()
        val ambientAudioIds = createAmbientAudio()

        // Start all the ambient sounds
        ambientAudioEngine.playSound(ambientAudioIds[4], true)

        // Store this position as the initial yaw angle
        initialYawAngle = orientationQuaternion.zRotation()
        beginGame = true

        // Begin Game Sequence
        Thread.sleep(2300)

        // I see you found my glasses
        narratorAudioEngine.playSound(narratorAudioIds[0], false)
        Thread.sleep(4400)

        // We're both trapped here now
        narratorAudioEngine.playSound(narratorAudioIds[1], false)
        Thread.sleep(2100)

        // But if you do what I say
        narratorAudioEngine.playSound(narratorAudioIds[2], false)
        Thread.sleep(5800)

        // Nod if you understand
        narratorAudioEngine.playSound(narratorAudioIds[3], false)
        Thread.sleep(2400)

        // Wait for Nod gesture
        while(!hasNod){
            Thread.sleep(50)
        }
        hasNod = false
        Thread.sleep(200)

        // Very Good
        narratorAudioEngine.playSound(narratorAudioIds[4], false)
        Thread.sleep(1500)

        // Now look to you left
        narratorAudioEngine.playSound(narratorAudioIds[5], false)
        Thread.sleep(3400)

        // Wait for the Orientation to change significantly to the left
        while(!(currentYawAngle > 85 && currentYawAngle < 95) && !hasTap){
            Thread.sleep(50)
        }
        hasTap = false

        // There's a door
        narratorAudioEngine.playSound(narratorAudioIds[6], false)
        Thread.sleep(3800)

        // You'll have to unlock
        narratorAudioEngine.playSound(narratorAudioIds[7], false)
        Thread.sleep(2200)

        // Follow the sound of the water
        narratorAudioEngine.playSound(narratorAudioIds[8], false)
        Thread.sleep(4000)

        // Wait for the user to move towards the water or timeout
        var startTime = System.currentTimeMillis()
        while(!(positionX < 55 && positionX > 40 && positionY > 40 && positionY < 55) && !hasTap){
            Thread.sleep(50)
            Log.i("Time Difference", "${System.currentTimeMillis() - startTime}")
            if(System.currentTimeMillis() - startTime > 10000){
                hasTap = true
            }
        }
        hasTap = false

        // You made it
        narratorAudioEngine.playSound(narratorAudioIds[9], false)
        Thread.sleep(1200)

        // Tap my glasses to grab it
        narratorAudioEngine.playSound(narratorAudioIds[10], false)
        Thread.sleep(2500)

        // Wait for the user to tap the glasses
        while(!hasTap){
            Thread.sleep(50)
        }
        hasTap = false

        // Play the jingle
        ambientAudioEngine.playSound(ambientAudioIds[2], false)
        Thread.sleep(800)

        // I'm banging on the door
        narratorAudioEngine.playSound(narratorAudioIds[11], false)
        Thread.sleep(2500)

        ambientAudioEngine.playSound(ambientAudioIds[1], true)

        Thread.sleep(1000)

        // Wait for the user to navigate
        startTime = System.currentTimeMillis()
        while(!(positionX > -5 && positionX < 15 && positionY > 15 && positionY < 30) && !hasTap){
            Thread.sleep(50)
            if(System.currentTimeMillis() - startTime > 10000){
                hasTap = true
            }
        }
        hasTap = false


        ambientAudioEngine.stopSound(ambientAudioIds[1])

        // Tap my glasses to open the door
        narratorAudioEngine.playSound(narratorAudioIds[12], false)
        Thread.sleep(1800)

        // Wait for the user to tap
        while(!hasTap){
            Thread.sleep(50)
        }
        hasTap = false

        // Play the door creak noise
        ambientAudioEngine.playSound(ambientAudioIds[0], false)
        Thread.sleep(800)

        // Oops I guest that let's me out
        narratorAudioEngine.playSound(narratorAudioIds[13], false)
        Thread.sleep(3500)

        // Good luck getting out on your own
        narratorAudioEngine.playSound(narratorAudioIds[15], false)
        Thread.sleep(4000)

    }

    // Creates and binds listeners for Bose AR devices
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


                        // The game has begun so start integrating sensor data
                        if(beginGame){

                            // Body fixed acceleration
                            val x_b = accelerometerVector.x() * 9.81 + sin(orientationQuaternion.yRotation()) * 9.81
                            val y_b = accelerometerVector.y() * 9.81 - sin(orientationQuaternion.xRotation()) * 9.81

                            // Log.i("Body-Fixed Acceleration", "($x_b, $y_b)")

                            // Earth fixed acceleration
                            val x_e = x_b * cos(currentYawAngle*(Math.PI/180)) - y_b * sin(currentYawAngle * (Math.PI/180))
                            val y_e = y_b * cos(currentYawAngle*(Math.PI/180)) + x_b * sin(currentYawAngle * (Math.PI/180))

                            // Log.i("Earth-Fixed Acceleration", "($x_e, $y_e)")


                            // Apply a low pass and high pass filter
                            val thresh = 0.1
                            val coef = 0.1


                            if(abs(x_e) < thresh) {
                                velocityX *= 0.9.pow(thresh - abs(x_e))
                            } else {
                                accelerationX = accelerationX * (1 - coef) + x_e * coef
                                velocityX += accelerationX * timestep
                            }

                            if(abs(y_e) < thresh) {
                                velocityY *= 0.9.pow(thresh - abs(y_e))
                            } else {
                                accelerationY = accelerationY * (1 - coef) + y_e * coef
                                velocityY += accelerationY * timestep
                            }


                            // Integrate the velocity data
                            positionX += velocityX * timestep
                            positionY += velocityY * timestep

                            ambientAudioEngine.setHeadPosition(positionX.toFloat(), positionX.toFloat(), 10f)

                            // Log.i("Velocity", "($velocityX, $velocityY)")
                            // Log.i("Position", "($positionX, $positionY)")

                        }

                    }
                    SensorType.GAME_ROTATION_VECTOR -> {

                        orientationQuaternion = sensorValue.quaternion()!! //Lets hope this is never null

                        ambientAudioEngine.setHeadRotation(orientationQuaternion.x().toFloat(), orientationQuaternion.y().toFloat(), orientationQuaternion.z().toFloat(), orientationQuaternion.w().toFloat())
                        ambientAudioEngine.update()

                        if (beginGame) {
                            currentYawAngle = ((orientationQuaternion.zRotation() - initialYawAngle) * (180/Math.PI))
                            Log.i("Yaw Angle", currentYawAngle.toString())
                            // Log.i("Pitch and Roll", "${orientationQuaternion.xRotation().toFloat() * (180/Math.PI)}, ${orientationQuaternion.yRotation().toFloat() * (180/Math.PI)}")
                        }
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

        val config: GestureConfiguration = wearableDevice.gestureConfiguration()
            .disableAll()
            .gestureEnabled(GestureType.SINGLE_TAP, true)
            .gestureEnabled(GestureType.DOUBLE_TAP, true)
            .gestureEnabled(GestureType.HEAD_NOD, true)
        wearableDevice.changeGestureConfiguration(config)
    }


    private fun createNarratorAudio(): ArrayList<Int> {

        // Set parameters for narrator voice
        narratorAudioEngine.enableRoom(true)
        narratorAudioEngine.setRoomProperties(50f, 50f, 50f, METAL, ACOUSTIC_CEILING_TILES, ACOUSTIC_CEILING_TILES)
        narratorAudioEngine.setRoomReverbAdjustments(0.3f, 0.6f, 1f)
        narratorAudioEngine.setHeadPosition(25f, 25f, 10f)
        narratorAudioEngine.setHeadRotation(0.0f, 0.0f, 0.0f, 1.0f)
        narratorAudioEngine.update()

        // Load all of the narrators audio files
        val narratorAudio = arrayOf(
            "audio/narrator/audio-1.ogg",
            "audio/narrator/audio-2.ogg",
            "audio/narrator/audio-3.ogg",
            "audio/narrator/audio-4.ogg",
            "audio/narrator/audio-5.ogg",
            "audio/narrator/audio-6.ogg",
            "audio/narrator/audio-7.ogg",
            "audio/narrator/audio-8.ogg",
            "audio/narrator/audio-9.ogg",
            "audio/narrator/audio-10.ogg",
            "audio/narrator/audio-11.ogg",
            "audio/narrator/audio-12.ogg",
            "audio/narrator/audio-13.ogg",
            "audio/narrator/audio-14.ogg",
            "audio/narrator/audio-15.ogg",
            "audio/narrator/audio-16.ogg"
            )

        val narratorAudioIds = ArrayList<Int>()

        // Load all the narrator sounds
        narratorAudio.forEach {
            narratorAudioEngine.preloadSoundFile(it)
            val soundId = narratorAudioEngine.createSoundObject(it)
            narratorAudioEngine.setSoundObjectPosition(soundId, 25f, 15f, 10f)
            narratorAudioEngine.setSoundVolume(soundId, 1f)
            narratorAudioIds.add(soundId)
        }

        return narratorAudioIds
    }

    private fun createAmbientAudio(): ArrayList<Int> {

        // Set parameters for the ambient audio
        ambientAudioEngine.enableRoom(true)
        ambientAudioEngine.setRoomProperties(50f, 50f, 50f, ACOUSTIC_CEILING_TILES, MARBLE, ACOUSTIC_CEILING_TILES)
        ambientAudioEngine.setRoomReverbAdjustments(0.7f, 0.6f, 1f)
        ambientAudioEngine.setHeadPosition(25f, 25f, 10f)
        ambientAudioEngine.setHeadRotation(0.0f, 0.0f, 0.0f, 1.0f)
        ambientAudioEngine.update()

        val ambientAudio = arrayOf(
            "audio/ambient/door_creak.ogg",
            "audio/ambient/door_knock.ogg",
            "audio/ambient/jingling_keys.ogg",
            "audio/ambient/scream.ogg",
            "audio/ambient/water_drop_2.ogg"
        )

        val ambientAudioIds = ArrayList<Int>()

        // Load all the ambient sounds
        ambientAudio.forEach {
            ambientAudioEngine.preloadSoundFile(it)
            val soundId = ambientAudioEngine.createSoundObject(it)
            ambientAudioEngine.setSoundObjectPosition(soundId, 0f, 0f, 0f) // Default for each
            ambientAudioEngine.setSoundVolume(soundId, 0.8f)
            ambientAudioIds.add(soundId)
        }

        ambientAudioEngine.setSoundObjectPosition(ambientAudioIds[0], 5f, 25f, 10f)
        ambientAudioEngine.setSoundObjectPosition(ambientAudioIds[1], 5f, 25f, 10f)
        ambientAudioEngine.setSoundObjectPosition(ambientAudioIds[2], 45f, 5f, 10f)
        ambientAudioEngine.setSoundObjectPosition(ambientAudioIds[4], 45f, 45f, 10f)


        return ambientAudioIds
    }

}


