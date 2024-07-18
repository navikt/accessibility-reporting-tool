package accessibility.reporting.tool.rest

import io.ktor.http.*

abstract class RequestException(val responseStatus: HttpStatusCode, message: String) : Throwable(message)
class ResourceNotFoundException(type: String, id: String) :
    RequestException(HttpStatusCode.NotFound, "Could not find any $type with id $id")

class BadPathParameterException(paramName:String) : RequestException(HttpStatusCode.BadRequest,"$paramName is missing or malformed")
