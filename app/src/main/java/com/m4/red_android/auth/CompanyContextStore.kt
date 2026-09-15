package com.m4.red_android.auth

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CompanyContext(val companyId: String, val accessName: String, val name: String, val schemaVersion: Int = 1) {
    fun isValid(): Boolean = schemaVersion == 1 && !companyId.isNullOrBlank() && !name.isNullOrBlank() && validAccessName(accessName)
}
fun validAccessName(value: String?): Boolean = value != null && value.length in 2..63 &&
    Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$").matches(value) &&
    value !in setOf("www", "app", "api", "admin", "auth", "mail", "support", "static", "cdn", "localhost")

interface CompanyContextStore {
    suspend fun read(): CompanyContext?
    suspend fun write(company: CompanyContext)
}

/** Company metadata only; noBackupFilesDir survives logout, but never OS backup/reinstall. */
class FileCompanyContextStore(context: Context) : CompanyContextStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "company-context.json"))
    private val gson = Gson()
    override suspend fun read(): CompanyContext? = withContext(Dispatchers.IO) {
        runCatching { gson.fromJson(file.readFully().toString(Charsets.UTF_8), CompanyContext::class.java) }
            .getOrNull()?.takeIf { it.isValid() }
    }
    override suspend fun write(company: CompanyContext) = withContext(Dispatchers.IO) {
        require(company.isValid())
        val stream = file.startWrite()
        try {
            stream.write(gson.toJson(company).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
}
