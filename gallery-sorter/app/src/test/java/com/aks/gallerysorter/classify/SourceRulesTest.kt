package com.aks.gallerysorter.classify

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.ExifData
import com.aks.gallerysorter.core.MediaKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SourceRulesTest {
    private val own = setOf("V2405A")
    private val vivoExif = ExifData("vivo", "V2405A", null, null, null)

    private fun cats(path: String, kind: MediaKind = MediaKind.IMAGE, exif: ExifData? = null) =
        sourceCategories(path, kind, exif, own)

    @Test fun cameraOriginal() {
        assertThat(cats("DCIM/Camera/IMG_1.jpg", exif = vivoExif)).containsExactly(Category.CAMERA)
        assertThat(isOwnCameraOriginal("DCIM/Camera/IMG_1.jpg", vivoExif, own)).isTrue()
    }

    @Test fun absolutePathCamera() {
        val p = "/storage/emulated/0/DCIM/Camera/IMG_1.jpg"
        assertThat(cats(p, exif = vivoExif)).containsExactly(Category.CAMERA)
        assertThat(isOwnCameraOriginal(p, vivoExif, own)).isTrue()
    }

    @Test fun cameraWithForeignModelIsCameraButNotOwn() {
        val other = ExifData("vivo", "X9999", null, null, null)
        assertThat(cats("DCIM/Camera/a.jpg", exif = other)).containsExactly(Category.CAMERA)
        assertThat(isOwnCameraOriginal("DCIM/Camera/a.jpg", other, own)).isFalse()
    }

    @Test fun ownModelMatchIsCaseInsensitive() {
        val lower = ExifData("vivo", "v2405a", null, null, null)
        assertThat(isOwnCameraOriginal("DCIM/Camera/a.jpg", lower, own)).isTrue()
    }

    @Test fun nullExifOrModelIsNotOwn() {
        assertThat(isOwnCameraOriginal("DCIM/Camera/a.jpg", null, own)).isFalse()
        assertThat(isOwnCameraOriginal("DCIM/Camera/a.jpg", ExifData("vivo", null, null, null, null), own)).isFalse()
    }

    @Test fun makeAloneIsNotEnough() {
        assertThat(isOwnCameraOriginal("DCIM/Camera/a.jpg", ExifData("vivo", "Y1", null, null, null), own)).isFalse()
    }

    @Test fun ownModelOutsideCameraFolderIsNotOwnOriginal() {
        assertThat(isOwnCameraOriginal("Download/a.jpg", vivoExif, own)).isFalse()
    }

    @Test fun screenshots() {
        assertThat(cats("Pictures/Screenshots/x.png")).containsExactly(Category.SCREENSHOTS)
        assertThat(cats("DCIM/Screenshots/x.jpg", exif = vivoExif)).containsExactly(Category.SCREENSHOTS)
        assertThat(cats("/storage/emulated/0/Pictures/Screenshots/x.png")).containsExactly(Category.SCREENSHOTS)
    }

    @Test fun screenRecordingsOnlyForVideo() {
        assertThat(cats("Movies/ScreenRecord/a.mp4", MediaKind.VIDEO)).containsExactly(Category.SCREEN_RECORDINGS)
        assertThat(cats("DCIM/ScreenRecorder/a.mp4", MediaKind.VIDEO)).containsExactly(Category.SCREEN_RECORDINGS)
        assertThat(cats("Movies/ScreenRecord/a.jpg", MediaKind.IMAGE)).isEmpty()
    }

    @Test fun cameraVideoIsCamera() {
        assertThat(cats("DCIM/Camera/VID_1.mp4", MediaKind.VIDEO)).containsExactly(Category.CAMERA)
    }

    @Test fun whatsapp() {
        assertThat(cats("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/a.jpg"))
            .containsExactly(Category.WHATSAPP)
    }

    @Test fun whatsappBusiness() {
        assertThat(cats("Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images/a.jpg"))
            .containsExactly(Category.WHATSAPP)
        assertThat(cats("WhatsApp Business/Media/WhatsApp Business Video/a.jpg")).containsExactly(Category.WHATSAPP)
    }

    @Test fun telegram() {
        assertThat(cats("Pictures/Telegram/a.jpg")).containsExactly(Category.TELEGRAM)
        assertThat(cats("Telegram/Telegram Images/a.jpg")).containsExactly(Category.TELEGRAM)
    }

    @Test fun downloads() {
        assertThat(cats("Download/a.jpg")).containsExactly(Category.DOWNLOADS)
    }

    @Test fun otherMessaging() {
        for (f in listOf("Instagram", "Messenger", "Signal", "Snapchat", "Facebook")) {
            assertThat(cats("Pictures/$f/a.jpg")).containsExactly(Category.OTHER_MESSAGING)
        }
    }

    @Test fun caseInsensitive() {
        assertThat(cats("dcim/camera/a.jpg")).containsExactly(Category.CAMERA)
        assertThat(cats("PICTURES/SCREENSHOTS/a.png")).containsExactly(Category.SCREENSHOTS)
        assertThat(cats("DOWNLOAD/a.jpg")).containsExactly(Category.DOWNLOADS)
    }

    @Test fun unknownFolderIsEmpty() {
        assertThat(cats("Documents/misc/a.jpg")).isEmpty()
    }

    @Test fun filenameDoesNotMatchFolderRule() {
        assertThat(cats("Pictures/Download")).isEmpty()
    }
}
