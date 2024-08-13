package com.greylabs.audiorecordsample

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log
import kotlin.math.round
import kotlin.math.sin

class RealtimeAudioRecorder(private val context: Context) {

    private val rate = 44100
    private val channelConf = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(rate, channelConf, audioFormat)

    private val miniFFT = FFT()

    var isRecording by mutableStateOf(false)

//    private val fileName by lazy {
//        "${externalCacheDir?.absolutePath}/record.3gp"
//    }

    private var audioRecord: AudioRecord? = null
    private var player: MediaPlayer? = null

    private var fileName: String = ""

    private val _audioDataFlow = MutableStateFlow<List<Float>>(listOf(0f, 0f, 0f))

    val audioDataFlow: StateFlow<List<Float>>
        get() = _audioDataFlow

    fun resolveDefaultCacheDir(): File {
        return context.filesDir.resolve(SM_AUDIO_CACHE)
        // see WavRecorder as example to record in file
    }

    fun reInitRecording() {
        stopRecording()
        initRecord()
    }

    @SuppressLint("MissingPermission")
    fun initRecord(filename: String = "") {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            rate,
            channelConf,
            audioFormat,
            bufferSize,
        )
    }

    suspend fun startRecording() {
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            audioRecord?.let { record ->
                isRecording = true
                record.startRecording()
                while (isRecording) {
                    record.read(buffer, 0, buffer.size)
                    val fftResult = miniFFT.iterativeFFT(
                        buffer.map { Complex(it.toDouble(), 0.0) }
                            .toTypedArray()
                    )
                    val data = listOf(
                        abs(fftResult[128].i.toFloat() / 1000),
                        abs(fftResult[700].i.toFloat() / 1000),
                        abs(fftResult[1000].i.toFloat() / 1000)
                    )

                    if (data[0] > 1 && data[1] > 1) {
                        _audioDataFlow.emit(data)
                    }
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord = null
    }

    /**************************************************************************************************
     * Fast Fourier Transform -- Kotlin Version
     * This version implements Cooley-Tukey algorithm for powers of 2 only.
     *
     * José Alexandre Nalon
     **************************************************************************************************
     * This program can be compiled by issuing the command:
     *
     * $ kotlinc fft.kt
     *
     * It will generate a file named 'FftKt.class' in the same directory. It can be run by issuing the
     * command:
     *
     * $ kotlin FftKt
     *
     **************************************************************************************************/

    /**************************************************************************************************
     * Include necessary libraries:
     **************************************************************************************************/
    class FFT {

        /**************************************************************************************************
         * Function: iterativeFFT
         *   Fast Fourier Transform using an iterative in-place decimation in time algorithm. This has
         *   O(N log_2(N)) complexity, and since there are less function calls, it will probably be
         *   marginally faster than the recursive versions.
         *
         * Parameters:
         *   x
         *     The vector of which the FFT will be computed. This should always be called with a vector
         *     of a power of two length, or it will fail. No checks on this are made.
         *
         * Returns:
         *   A complex-number vector of the same size, with the coefficients of the DFT.
         **************************************************************************************************/
        fun iterativeFFT(x: Array<Complex>): Array<Complex> {
            val N = x.size
            val X = Array<Complex>(N) { _ -> Complex() }

            val r = (round(log(N.toDouble(), 2.0))).toInt()    // Number of bits;
            for (k in 0..N - 1) {
                var l = bitReverse(k, r)                       // Reorder the vector according to
                X[l] = x[k]                                    //   the bit-reversed order;
            }

            var step: Int = 1                                  // Computation of twiddle factors;
            for (k in 1..r) {
                var l = 0
                while (l < N) {
                    var W = Cexp(-PI / step.toDouble())          // Twiddle factors;
                    var Wkn = Complex(1.0, 0.0)
                    for (n in 0..step - 1) {
                        var p = l + n
                        var q = p + step
                        X[q] = X[p] - Wkn * X[q]               // Recombine results;
                        X[p] = X[p] * 2.0 - X[q]
                        Wkn = Wkn * W                          // Update twiddle factors;
                    }
                    l = l + 2 * step
                }
                step = step * 2
            }

            return X                                           // Return value;
        }

        // Complex exponential of an angle:
        private fun Cexp(a: Double): Complex {
            return Complex(cos(a), sin(a));
        }

        /**************************************************************************************************
         * Function: bitReverse
         *   Bit-reversed version of an integer number.
         *
         * Parameters:
         *   k
         *     The number to be bit-reversed;
         *   r
         *     The number of bits to take into consideration when reversing.
         *
         * Returns:
         *   The number k, bit-reversed according to integers with r bits.
         **************************************************************************************************/
        private fun bitReverse(k: Int, r: Int): Int {
            var l: Int = 0                                     // Accumulate the results;
            var k0: Int = k
            for (i in 1..r) {                                  // Loop on every bit;
                l = (l shl 1) + (k0 and 1)                     // Test less signficant bit and add;
                k0 = (k0 shr 1)                                // Test next bit;
            }
            return l
        }
    }

    /**************************************************************************************************
     * Mini-library to deal with complex numbers.
     **************************************************************************************************/
    class Complex(val r: Double, val i: Double) {

        // Constructor:
        constructor() : this(0.0, 0.0) {}

        // Add the argument to this, giving the result as a new complex number:
        operator fun plus(c: Complex): Complex {
            return Complex(r + c.r, i + c.i)
        }

        // Subtract the argument from this, giving the result as a new complex number:
        operator fun minus(c: Complex): Complex {
            return Complex(r - c.r, i - c.i)
        }

        // Multiply the argument with this, giving the result as a new complex number:
        operator fun times(c: Complex): Complex {
            return Complex(r * c.r - i * c.i, r * c.i + i * c.r)
        }

        // Multiply with an scalar, giving the reulst as a new complex number:
        operator fun times(a: Double): Complex {
            return Complex(a * r, a * i)
        }

        // Divide this by the argument, giving the result as a new complex number:
        operator fun div(a: Double): Complex {
            return Complex(r / a, i / a)
        }

        override fun toString(): String {
            return "$r : $i"
        }

    }

    private companion object {
        const val SM_AUDIO_CACHE = "smAudioCache"
    }
}