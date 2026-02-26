package com.m4.red_android.data.validation

import com.m4.red_android.data.models.FormField

fun <T> validateField(
    field: FormField<T>,
    validator: (FormField<T>) -> String?
): FormField<T> {
    return field.copy(error = validator(field))
}