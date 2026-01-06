package com.tiffin.service.management.crm

import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


class MainActivity: FlutterActivity() {
    private val CHANNEL = "flutter_html_to_pdf"
  
    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
      super.configureFlutterEngine(flutterEngine)
      MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
        call, result ->
        if (call.method == "convertHtmlToPdf") {
            convertHtmlToPdf(call, result)
          } else {
            result.notImplemented()
          }
      }
    }

    private fun convertHtmlToPdf(call: MethodCall, result: Result) {
        val htmlFilePath = call.argument<String>("htmlFilePath")
        val printSize = call.argument<String>("printSize")
        val orientation = call.argument<String>("orientation")
    
        HtmlToPdfConverter().convert(htmlFilePath!!, applicationContext,  printSize!!, orientation!!, object : HtmlToPdfConverter.Callback {
          override fun onSuccess(filePath: String) {
            result.success(filePath)
          }
    
          override fun onFailure() {
            result.error("ERROR", "Unable to convert html to pdf document!", "")
          }
        })
      }
  }