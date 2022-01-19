package com.hitmeows.qrgenerator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.hitmeows.qrgenerator.ui.theme.QRGeneratorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val scope = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {

            QRGeneratorTheme {

                val coroutineScope = rememberCoroutineScope()
                val snackBarState = remember {
                    SnackbarHostState()
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "QR Generator") },
                        )
                    }
                ) {

                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            var bitmap by remember {
                                mutableStateOf(generateQRCode(""))
                            }
                            var text by remember {
                                mutableStateOf("")
                            }

                            LaunchedEffect(key1 = text) {
                                delay(500)
                                bitmap = generateQRCode(text)
                            }


                            var uri: Uri? = null
                            val label = remember {
                                mutableStateOf("Enter anything to generate qr")
                            }


                            Spacer(modifier = Modifier.height(20.dp))
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "",
                                modifier = Modifier
                                    .padding(20.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(androidx.compose.ui.graphics.Color.White)
                                    .border(
                                        3.dp,
                                        MaterialTheme.colors.onBackground,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            if (uri == null && text.isNotBlank()) {
                                                uri = saveImage(bitmap)
                                            }
                                            if (uri != null) shareImage(uri!!)
                                            else if (text.isBlank()) snackBarState.showSnackbar(
                                                "please enter something in text field",
                                                duration = SnackbarDuration.Short,
                                                actionLabel = "okay"
                                            )
                                            else snackBarState.showSnackbar(
                                                "oh no! some bad things happened.",
                                                duration = SnackbarDuration.Short,
                                                actionLabel = "dismiss"
                                            )
                                        }
                                    },
                                contentScale = ContentScale.Fit
                            )

                            OutlinedTextField(value = text,
                                onValueChange = {
                                    text = if (it.isNotBlank() && (it.last()-'0')>127) {
                                        coroutineScope.launch {
                                            snackBarState.showSnackbar(
                                                "this character not allowed",
                                                duration = SnackbarDuration.Short,
                                                actionLabel = "okay"
                                            )
                                        }
                                        it.substring(0,it.lastIndex-1)
                                    } else it
                                    uri = null
                                    label.value = if(it.isNotBlank()) "tap image to share qr"
                                    else "Enter anything to generate qr"
                                },
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                label = { Text(text = label.value) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text
                                )
                            )

                        }

                        SnackbarHost(hostState = snackBarState, modifier = Modifier.offset(0.dp, 100.dp))

                    }
                }
            }
        }
    }

    private fun generateQRCode(text: String): Bitmap {
        val width = 512
        val height = 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val codeWriter = MultiFormatWriter()

        try {
            val bitMatrix = codeWriter.encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height
            )

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val color = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    bitmap.setPixel(x, y, color)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bitmap
    }

    private fun saveImage(bitmap: Bitmap): Uri? {
        val imgFolder = File(cacheDir, "images")
        var uri: Uri? = null
        try {
            imgFolder.mkdirs()
            val file = File(imgFolder, "shared_image.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(scope, "com.hitmeows.fileprovider", file)
        } catch (e: Exception) {
            Log.d("mmmm", e.toString())
        }
        return uri
    }

    private fun shareImage(uri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "check out this qr code I generated")
        }

        startActivity(Intent.createChooser(shareIntent, "qr generator"))
    }
}