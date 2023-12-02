package com.example.picscribe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.picscribe.ui.theme.PicScribeTheme


class MainActivity : ComponentActivity() {
    private lateinit var controller: LifecycleCameraController
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
        controller = LifecycleCameraController(applicationContext).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.VIDEO_CAPTURE
            )
        }

        setContent {
            PicScribeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    CameraView()
                }
            }
        }

    }

    @Composable
    fun CameraView() {
        Column(modifier = Modifier) {
            viewModel = viewModel()
            CameraPreview(
                controller = controller,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            BottomBar()

        }
    }

  @Composable
  fun BottomBar() {
        val context = LocalContext.current
      Column(modifier=Modifier.padding(8.dp)) {
          Text(text = "Image Description:", fontWeight = FontWeight.Bold)
          DisplayCaption(viewModel = viewModel)
      }
          Divider()
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
          ) {
              IconButton(
                  onClick = {
                      controller.cameraSelector =
                          if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                              CameraSelector.DEFAULT_FRONT_CAMERA
                          } else CameraSelector.DEFAULT_BACK_CAMERA
                  },
                  modifier = Modifier
              ) {
                  Icon(
                      imageVector = Icons.Default.Cameraswitch,
                      contentDescription = "Switch camera"
                  )
              }

              IconButton(

                  modifier = Modifier,
                  onClick = {
                      Toast.makeText(context, "Scanning image...", Toast.LENGTH_LONG).show()
                      takePhoto(
                          controller = controller,
                          onPhotoTaken = viewModel::onTakePhoto,

                          )
                  }
              ) {
                  Icon(
                      imageVector = Icons.Default.PhotoCamera,
                      contentDescription = "Take photo"
                  )
              }
          }

    }


    @Composable
    fun DisplayCaption(viewModel: MainViewModel) {
        val caption by viewModel.caption.collectAsState()
        val error by viewModel.error.collectAsState()
        if (error.isNotEmpty()) {
            Toast.makeText(LocalContext.current, error, Toast.LENGTH_SHORT).show()
        }
        Text(caption)
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)
                }


                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }
}

