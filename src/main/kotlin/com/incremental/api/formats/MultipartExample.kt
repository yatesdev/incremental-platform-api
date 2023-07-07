package com.incremental.api.formats

import org.http4k.core.Body
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.lens.Validator
import org.http4k.lens.multipartForm

data class Name(val value: String)

// define fields using the standard lens syntax
val nameField = MultipartFormField.string().map(::Name, Name::value).required("name")
val imageFile = MultipartFormFile.optional("image")

// add fields to a form definition, along with a validator
val strictFormBody = Body.multipartForm(Validator.Strict, nameField, imageFile, diskThreshold = 5).toLens()
