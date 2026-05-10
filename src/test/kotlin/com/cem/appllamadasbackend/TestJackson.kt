import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ExportDataDto(
    val event_id: String,
    val event_type: String,
    val event_timestamp: Long,
    val attempt_number: Int,
    val is_valid_attempt: Boolean,
    val duration_seconds: Int?,
    val duration_minutes: Double?
)

fun main() {
    val mapper = jacksonObjectMapper()
    val obj = ExportDataDto(
        event_id = "123",
        event_type = "TYPE",
        event_timestamp = 0L,
        attempt_number = 1,
        is_valid_attempt = true,
        duration_seconds = 17,
        duration_minutes = 0.28
    )
    println(mapper.writeValueAsString(obj))
}
