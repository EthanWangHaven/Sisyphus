package cn.wangce.lumi.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 图片仓库：相册图压缩入库（filesDir/<subdir>/uuid.jpg）+ 子采样加载 + 删除清理
@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun dir(name: String): File =
        File(context.filesDir, name).apply { if (!exists()) mkdirs() }

    // 相册 uri → 采样解码 → EXIF 方向修正 → 限长边 1280px → JPEG 80 写入私有目录
    suspend fun compress(uri: Uri, subdir: String = "moments"): String? {
        return withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val s1 = resolver.openInputStream(uri) ?: return@runCatching null
            BitmapFactory.decodeStream(s1, null, bounds)
            s1.close()
            // bounds 模式下 decodeStream 返回 null 是正常行为（只填 bounds），以 outWidth 判定真失败
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val maxDim = 1280
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val src = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (src == null) {
                android.util.Log.e("ImageStore", "decodeStream returned null for $uri sample=$sample")
            }
            src ?: return@runCatching null

            val oriented = applyExifOrientation(uri, src)
            if (oriented !== src) src.recycle()
            val scaled = limitTo(oriented, maxDim)
            if (scaled !== oriented) oriented.recycle()

            val file = File(dir(subdir), "${UUID.randomUUID()}.jpg")
            file.outputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 80, out) }
            scaled.recycle()
            file.absolutePath
        }.onFailure { e ->
            android.util.Log.e("ImageStore", "compress failed for $uri", e)
        }.getOrNull()
        }
    }

    // 从私有目录按需求尺寸子采样加载（卡片缩略图用，同步调用）
    fun loadBitmap(path: String, reqSize: Int = 512): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqSize || bounds.outHeight / (sample * 2) >= reqSize) {
            sample *= 2
        }
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    // 相册 uri 子采样加载（发布弹层展示新选图用，图片尚未入库）
    fun loadBitmapFromUri(uri: Uri, reqSize: Int = 512): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqSize || bounds.outHeight / (sample * 2) >= reqSize) {
            sample *= 2
        }
        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    // 删除私有目录图片文件（编辑移除图片 / 级联清理用）
    fun delete(path: String) {
        runCatching { File(path).delete() }
    }

    private fun limitTo(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            max(1, (bitmap.width * ratio).toInt()),
            max(1, (bitmap.height * ratio).toInt()),
            true,
        )
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                @Suppress("DEPRECATION")
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
