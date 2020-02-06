package com.sample.rxandroidble2

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.polidea.rxandroidble2.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.LazyThreadSafetyMode.NONE

private const val TAG = "RxBle#SampleApp"
private const val ADDRESS = "CA:5F:21:15:CB:74"
private const val AUTO_CONNECT = false
private const val TIMEOUT = 15L

private const val ID_CONNECTION = 1
private const val ID_CONNECT = 2
private const val ID_RECONNECT = 3
private const val ID_UPDATE_UI = 4

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private val uiScheduler get() = AndroidSchedulers.mainThread()
    private val rxBleClient by lazy(NONE) { RxBleClient.create(this) }
    private val disposables: Disposables = Disposables()

    private var rxBleConnection: RxBleConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())
        RxBleClient.updateLogOptions(
            LogOptions.Builder()
                .setLogLevel(LogConstants.VERBOSE)
                .setShouldLogAttributeValues(true)
                .setShouldLogScannedPeripherals(true)
                .setMacAddressLogSetting(LogConstants.MAC_ADDRESS_FULL)
                .setUuidsLogSetting(LogConstants.UUIDS_FULL)
                .setLogger { level, tag, msg -> Timber.tag(tag).log(level, msg)}
                .build())
    }


    override fun onResume() {
        super.onResume()
        listenConnection()
        connect()
    }

    override fun onPause() {
        super.onPause()
        disposables.release()
        rxBleConnection = null
    }

    private fun listenConnection() {
        disposables.add(ID_CONNECTION, rxBleClient.getBleDevice().observeConnectionStateChanges()
            .doOnSubscribe { Timber.tag(TAG).d("[listenConnection] no args") }
            .observeOn(uiScheduler)
            .subscribe({
                Timber.tag(TAG).d("[listenConnection] emitted: %s", it)
                updateUI(it)
                if (it == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
                    scheduleReconnect()
                }
            }, {
                Timber.tag(TAG).e(it, "[listenConnection] failed: %s", it.toString())
            }))
    }

    private fun updateUI(state: RxBleConnection.RxBleConnectionState) {
        disposables.add(ID_UPDATE_UI, uiScheduler.scheduleDirect {
            connectionView.text = "[$ADDRESS] $state"
        })
    }

    private fun connect() {
        disposables.add(ID_CONNECT, rxBleClient.getBleDevice().establishConnection(AUTO_CONNECT, Timeout(TIMEOUT, TimeUnit.SECONDS))
            .doOnSubscribe { Timber.tag(TAG).d("[connect] no args") }
            .subscribe({
                Timber.tag(TAG).d("[connect] emitted: %s", it)
                rxBleConnection = it
            }, {
                Timber.tag(TAG).e(it, "[connect] failed: $it")
            })
        )
    }


    private fun scheduleReconnect() {
        disposables.add(ID_RECONNECT, Single.timer(2L, TimeUnit.SECONDS)
            .doOnSubscribe { Timber.tag(TAG).d("[scheduleReconnect] no args") }
            .subscribe({
                Timber.tag(TAG).d("[scheduleReconnect] emitted: %s", it)
                connect()
            }, {
                Timber.tag(TAG).e(it, "[scheduleReconnect] failed: $it")
            }))
    }


}

private fun RxBleClient.getBleDevice() = getBleDevice(ADDRESS)