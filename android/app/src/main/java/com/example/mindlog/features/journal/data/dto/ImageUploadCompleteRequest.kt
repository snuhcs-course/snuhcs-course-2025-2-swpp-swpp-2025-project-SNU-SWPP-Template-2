
import com.google.gson.annotations.SerializedName

data class ImageUploadCompleteRequest(
    @SerializedName("s3_key")
    val s3Key: String
)
    