package com.m4.red_android

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.m4.red_android.auth.CompanyContext
import com.m4.red_android.auth.FileCompanyContextStore
import com.m4.red_android.ui.login.CompanyAccessScreen
import com.m4.red_android.viewmodels.CompanyUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class CompanyAccessFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun firstSetupAcceptsFriendlyNameWithoutInternalId() {
        var input: String? = null
        compose.setContent {
            CompanyAccessScreen(CompanyUiState(), { input = it }, {})
        }
        compose.onNodeWithTag("company_access_resolve").assertIsNotEnabled()
        compose.onNodeWithTag("company_access_name").performTextInput("minha-loja")
        compose.onNodeWithTag("company_access_resolve").performClick()
        assertEquals("minha-loja", input)
        compose.onAllNodesWithText("Identificador da empresa").assertCountEquals(0)
    }
    @Test fun atomicCompanyFileSurvivesStoreRecreationAndRejectsCorruption() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Context>()
        // Isolated test directory: never overwrite the installed application's company preference.
        val directory = File(application.cacheDir, "company-access-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(application) { override fun getNoBackupFilesDir() = directory }
        try {
            val company = CompanyContext("a", "loja-a", "Loja A")
            FileCompanyContextStore(context).write(company)
            assertEquals(company, FileCompanyContextStore(context).read())
            File(directory, "company-context.json").writeText("corrupt")
            assertNull(FileCompanyContextStore(context).read())
        } finally { directory.deleteRecursively() }
    }
}
