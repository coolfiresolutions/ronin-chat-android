package com.coolfiresolutions.roninchat.server.enums

enum class ResponseCode constructor(code: Int, var description: String) {
    SUCCESS(200, "Success"),
    CREATED(201, "Created"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized to server"),
    CONFLICT(409, "Conflict");

    var code: Int = 0
        internal set

    init {
        this.code = code
    }
}