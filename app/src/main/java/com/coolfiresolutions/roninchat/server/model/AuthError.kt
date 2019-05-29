package com.coolfiresolutions.roninchat.server.model

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthError(var error: String, @JsonProperty("error_description") var errorDescription: String)