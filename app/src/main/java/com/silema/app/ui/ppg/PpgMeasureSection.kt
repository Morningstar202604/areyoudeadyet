package com.silema.app.ui.ppg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.engine.StressMath
import com.silema.app.ppg.PpgAnalyzer
import com.silema.app.store.rememberAppRepository
import com.silema.app.ui.components.BigButton
import kotlinx.coroutines.delay

private const val MEASURE_SECONDS = 30

@Composable
fun PpgMeasureSection() {
    val context = LocalContext.current
    val repository = rememberAppRepository()

    var measuring by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("") }

    val analyzer = remember { PpgAnalyzer() }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            statusText = ""
            measuring = true
        } else {
            resultText = "未授予相机权限，无法进行光学测量。请到系统设置里允许本应用使用相机。"
        }
    }

    DisposableEffect(measuring) {
        var thread: HandlerThread? = null
        var device: CameraDevice? = null
        var session: CameraCaptureSession? = null
        var reader: ImageReader? = null

        if (measuring) {
            analyzer.reset()
            runCatching {
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val backId = manager.cameraIdList.firstOrNull { id ->
                    manager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                } ?: throw IllegalStateException("没有后置摄像头")

                thread = HandlerThread("ppg").apply { start() }
                val handler = Handler(thread!!.looper)

                reader = ImageReader.newInstance(240, 240, ImageFormat.YUV_420_888, 4)
                reader!!.setOnImageAvailableListener({ r ->
                    val img: Image? = r.acquireLatestImage()
                    if (img != null) {
                        val red = estimateRedAverage(img)
                        if (red >= 0) analyzer.addSample(System.currentTimeMillis(), red)
                        img.close()
                    }
                }, handler)

                @SuppressLint("MissingPermission")
                manager.openCamera(backId, object : CameraDevice.StateCallback() {
                    override fun onOpened(cam: CameraDevice) {
                        device = cam
                        val outputs = listOf(reader!!.surface)
                        runCatching {
                            cam.createCaptureSession(outputs, object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(s: CameraCaptureSession) {
                                    session = s
                                    val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                                        addTarget(reader!!.surface)
                                        set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                                    }
                                    s.setRepeatingRequest(req.build(), null, handler)
                                    statusText = "采集中"
                                }
                                override fun onConfigureFailed(s: CameraCaptureSession) {
                                    statusText = "相机会话配置失败"
                                    measuring = false
                                }
                            }, handler)
                        }.onFailure {
                            statusText = "会话创建失败：${it.message}"
                            measuring = false
                        }
                    }

                    override fun onDisconnected(cam: CameraDevice) { runCatching { cam.close() } }
                    override fun onError(cam: CameraDevice, error: Int) {
                        statusText = "相机错误(code=$error)"
                        measuring = false
                    }
                }, handler)
            }.onFailure {
                statusText = "相机打开失败：${it.message ?: it.javaClass.simpleName}"
                measuring = false
            }
        }

        onDispose {
            runCatching { session?.stopRepeating() }
            runCatching { session?.close() }
            runCatching { device?.close() }
            runCatching { reader?.close() }
            thread?.quitSafely()
        }
    }

    if (measuring) {
        LaunchedEffect(Unit) {
            while (elapsedSec < MEASURE_SECONDS) {
                delay(1000)
                elapsedSec++
            }
            measuring = false
            stopAndEvaluate(analyzer) { t, saved ->
                resultText = t
                savedMsg = saved
            }
        }
    }

    Column {
        Text("摄像头脉搏实测（PPG 光电容积波）", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "原理：指尖盖住后置摄像头，闪光灯穿透指尖；每次心跳都会微弱改变透光强度。逐帧提取红色通道信号，经去趋势滤波、自适应峰值检测与心跳间期统计，算出心率与 HRV(RMSSD)。这是真实的光学测量，不是模拟。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        if (!measuring) {
            BigButton(text = "开始 ${MEASURE_SECONDS} 秒实测", onClick = {
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    return@BigButton
                }
                analyzer.reset()
                elapsedSec = 0
                resultText = null
                savedMsg = null
                statusText = ""
                measuring = true
            })
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black)
            ) {
                Column(Modifier.align(Alignment.Center)) {
                    Text(
                        text = "指尖完全盖住\n后置摄像头",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "闪光灯已点亮 · 保持不动",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFCDD2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { elapsedSec.toFloat() / MEASURE_SECONDS },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Text(
                text = "${MEASURE_SECONDS - elapsedSec} 秒后自动出结果 · $statusText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        resultText?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
        savedMsg?.let {
            Spacer(Modifier.height(6.dp))
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun stopAndEvaluate(analyzer: PpgAnalyzer, onDone: (String, String?) -> Unit) {
    val r = analyzer.analyze()
    when {
        r == null -> onDone(
            "信号不足，无法给出结果。请确认指尖完全盖住镜头（不要漏光）、手不要抖，然后重试。",
            null
        )
        r.confidence < 0.45 -> onDone(
            "本次置信度只有 ${(r.confidence * 100).toInt()}%（检出 ${r.beatCount} 拍），不足以采信，请重测一次并保持全程静止。",
            null
        )
        else -> {
            repository.addRecord(
                VitalRecord.of(
                    VitalType.HEART_RATE,
                    Math.round(r.bpm).toDouble(),
                    System.currentTimeMillis(),
                    VitalSource.PPG_CAMERA
                )
            )
            val stress = StressMath.fromRmssd(r.rmssdMs).toDouble()
            repository.addRecord(
                VitalRecord.of(VitalType.STRESS, stress, System.currentTimeMillis(), VitalSource.PPG_CAMERA)
            )
            onDone(
                "实测心率 ${r.bpm.toInt()} 次/分 · 检出 ${r.beatCount} 拍 · 置信度 ${(r.confidence * 100).toInt()}% · HRV(RMSSD) ${r.rmssdMs} ms · 压力估算 ${stress.toInt()} 分（${StressMath.label(stress.toInt())}）",
                "已自动保存到心率与压力记录，首页风险评估随之更新"
            )
        }
    }
}

private fun estimateRedAverage(image: Image): Double = runCatching {
    val yPlane = image.planes[0]
    val vPlane = image.planes[2]
    val w = image.width
    val h = image.height
    val yBuf = yPlane.buffer
    val vBuf = vPlane.buffer
    val yRow = yPlane.rowStride
    val yPix = yPlane.pixelStride
    val vRow = vPlane.rowStride
    val vPix = vPlane.pixelStride

    var sum = 0.0
    var n = 0L
    var row = h / 4
    while (row < h * 3 / 4) {
        var col = w / 4
        while (col < w * 3 / 4) {
            val y = (yBuf.get(row * yRow + col * yPix).toInt() and 0xFF) - 16
            val uvRow = row / 2
            val uvCol = col / 2
            val v = (vBuf.get(uvRow * vRow + uvCol * vPix).toInt() and 0xFF) - 128
            val r = 1.164 * y + 1.596 * v
            sum += r
            n++
            col += 2
        }
        row += 2
    }
    if (n == 0L) -1.0 else sum / n
}.getOrDefault(-1.0)
