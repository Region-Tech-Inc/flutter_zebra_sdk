package com.tlt.flutter_zebra_sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.zebra.sdk.btleComm.BluetoothLeConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.comm.TcpConnection
import com.zebra.sdk.printer.discovery.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

interface JSONConvertable {
  fun toJSON(): String = Gson().toJson(this)
}

inline fun <reified T : JSONConvertable> String.toObject(): T = Gson().fromJson(this, T::class.java)

class FlutterZebraSdkPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var channel: MethodChannel
  private lateinit var context: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_zebra_sdk")
    channel.setMethodCallHandler(this)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    Log.d("ZebraSDK", "Attached to Activity")
  }

  override fun onDetachedFromActivityForConfigChanges() {
    Log.d("ZebraSDK", "Detached from Activity for Config Changes")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    Log.d("ZebraSDK", "Reattached to Activity for Config Changes")
  }

  override fun onDetachedFromActivity() {
    Log.d("ZebraSDK", "Detached from Activity")
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "printZPLOverTCPIP" -> onPrintZPLOverTCPIP(call, result)
      "printZPLOverBluetooth" -> onPrintZplDataOverBluetooth(call, result)
      "onGetPrinterInfo" -> onGetPrinterInfo(call, result)
      else -> result.notImplemented()
    }
  }

  private fun onPrintZPLOverTCPIP(@NonNull call: MethodCall, @NonNull result: Result) {
    val ip = call.argument<String>("ip") ?: run {
      result.error("PrintZPLOverTCPIP", "IP Address is required", null)
      return
    }
    val data = call.argument<String>("data") ?: run {
      result.error("PrintZPLOverTCPIP", "Data is required", null)
      return
    }

    val conn: Connection = TcpConnection(ip, TcpConnection.DEFAULT_ZPL_TCP_PORT)
    try {
      conn.open()
      conn.write(data.toByteArray())
      result.success(mapOf("success" to true, "message" to "Print successful"))
    } catch (e: ConnectionException) {
      result.error("Error", "onPrintZPLOverTCPIP", e.message)
    } finally {
      conn.close()
    }
  }

  private fun onPrintZplDataOverBluetooth(@NonNull call: MethodCall, @NonNull result: Result) {
    val macAddress = call.argument<String>("mac")
    val data = call.argument<String>("data")

    if (macAddress == null || data == null) {
      result.error("Error", "MAC Address and Data are required", null)
      return
    }

    val conn = BluetoothLeConnection(macAddress, context)
    try {
      conn.open()
      conn.write(data.toByteArray())
      result.success(mapOf("success" to true, "message" to "Print successful"))
    } catch (e: Exception) {
      result.error("Error", "onPrintZplDataOverBluetooth", e.message)
    } finally {
      conn.close()
    }
  }

  private fun onGetPrinterInfo(@NonNull call: MethodCall, @NonNull result: Result) {
    val ip = call.argument<String>("ip") ?: run {
      result.error("Error", "IP Address is required", null)
      return
    }
    val conn: Connection = TcpConnection(ip, TcpConnection.DEFAULT_ZPL_TCP_PORT)
    try {
      conn.open()
      val dataMap = DiscoveryUtil.getDiscoveryDataMap(conn)
      result.success(dataMap)
    } catch (e: ConnectionException) {
      result.error("Error", "onGetPrinterInfo", e.message)
    } finally {
      conn.close()
    }
  }
}
