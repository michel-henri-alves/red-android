package com.m4.red_android.viewmodels

import com.m4.red_android.auth.*
import com.m4.red_android.data.api.*
import com.m4.red_android.testing.MainDispatcherRule
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.Base64
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CompanyAccessViewModelTest {
    @Test fun `approved company names support two characters but reject one`() {
        assertTrue(validAccessName("m4"))
        assertTrue(validAccessName("ramon-lopes"))
        assertFalse(validAccessName("a"))
    }
    @get:Rule val dispatcher = MainDispatcherRule()
    private val company = CompanyContext("company-1", "loja-a", "Loja A")
    private class Store(var value: CompanyContext? = null) : CompanyContextStore {
        override suspend fun read() = value
        override suspend fun write(company: CompanyContext) { value = company }
    }
    private val resolver = object : CompanyAccessApi {
        override suspend fun resolve(request: CompanyAccessRequest) = CompanyAccessResponse("company-1", request.accessName, "Loja A")
    }
    private fun loginApi(fail: Boolean = false, otherTenant: Boolean = false) = object : LoginApi {
        override suspend fun login(request: LoginRequest): LoginResponse {
            if (fail) throw IOException("offline")
            val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"exp\":2000}".toByteArray())
            return LoginResponse("header.$payload.signature", LoginUser("User", "user", if (otherTenant) "other" else request.companyId, true))
        }
        override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest) = PasswordRecoveryResponse("accepted")
    }
    @Test fun `wire response becomes versioned context without requiring schemaVersion from API`() {
        val response = Gson().fromJson("{\"companyId\":\"company-1\",\"accessName\":\"loja-a\",\"name\":\"Loja A\"}", CompanyAccessResponse::class.java)
        assertTrue(response.toContext().isValid())
    }
    @Test fun `successful restricted login persists company across logout and viewmodel recreation`() = runTest(dispatcher.dispatcher) {
        val store = Store()
        val manager = SessionManager(FakeSecureTokenStore(), { 1000 })
        manager.restore()
        val vm = AuthViewModel(manager, loginApi(), companyApi = resolver, companyStore = store)
        advanceUntilIdle()
        vm.selectCompany(" Loja-A ")
        advanceUntilIdle()
        assertNull(store.value)
        assertEquals(company, vm.companyState.value.company)
        vm.login("company-1", "user@example.com", "secret")
        advanceUntilIdle()
        assertEquals(company, store.value)
        assertTrue((manager.state.value as SessionState.Authenticated).session.user!!.requiresInitialPasswordChange)
        vm.logout(); advanceUntilIdle()
        val restored = AuthViewModel(manager, loginApi(), companyApi = resolver, companyStore = store)
        advanceUntilIdle()
        assertEquals(company, restored.companyState.value.company)
    }
    @Test fun `failed switched login preserves previous durable company`() = runTest(dispatcher.dispatcher) {
        val previous = company.copy(accessName = "previous")
        val store = Store(previous)
        val manager = SessionManager(FakeSecureTokenStore(), { 1000 }); manager.restore()
        val vm = AuthViewModel(manager, loginApi(fail = true), companyApi = resolver, companyStore = store)
        advanceUntilIdle(); vm.switchCompany(); advanceUntilIdle()
        vm.selectCompany("loja-a"); advanceUntilIdle()
        vm.login("company-1", "user@example.com", "secret"); advanceUntilIdle()
        assertEquals(previous, store.value)
        assertEquals(SessionState.Unauthenticated, manager.state.value)
    }
    @Test fun `mismatched login response never persists company or session`() = runTest(dispatcher.dispatcher) {
        val store = Store(company)
        val manager = SessionManager(FakeSecureTokenStore(), { 1000 }); manager.restore()
        val vm = AuthViewModel(manager, loginApi(otherTenant = true), companyApi = resolver, companyStore = store)
        advanceUntilIdle(); vm.login("company-1", "user@example.com", "secret"); advanceUntilIdle()
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertEquals(LoginUiState.Error(LoginFailure.INVALID_RESPONSE), vm.loginState.value)
    }
    @Test fun `saved mapping change blocks credentials and requires setup`() = runTest(dispatcher.dispatcher) {
        var loginCalled = false
        val store = Store(company)
        val manager = SessionManager(FakeSecureTokenStore(), { 1000 }); manager.restore()
        val changed = object : CompanyAccessApi {
            override suspend fun resolve(request: CompanyAccessRequest) = CompanyAccessResponse("other", request.accessName, "Other")
        }
        val api = object : LoginApi {
            override suspend fun login(request: LoginRequest): LoginResponse { loginCalled = true; error("unexpected") }
            override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest) = error("unexpected")
        }
        val vm = AuthViewModel(manager, api, companyApi = changed, companyStore = store)
        advanceUntilIdle(); vm.login("company-1", "user@example.com", "secret"); advanceUntilIdle()
        assertFalse(loginCalled); assertNull(vm.companyState.value.company); assertEquals(company, store.value)
    }
    @Test fun `late login after switch cannot authenticate previous company`() = runTest(dispatcher.dispatcher) {
        val pending = CompletableDeferred<LoginResponse>()
        val store = Store(company)
        val manager = SessionManager(FakeSecureTokenStore(), { 1000 }); manager.restore()
        val api = object : LoginApi {
            override suspend fun login(request: LoginRequest) = pending.await()
            override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequest) = error("unexpected")
        }
        val vm = AuthViewModel(manager, api, companyApi = resolver, companyStore = store)
        advanceUntilIdle(); vm.login("company-1", "user@example.com", "secret"); runCurrent()
        vm.switchCompany(); runCurrent()
        pending.complete(loginApi().login(LoginRequest("company-1", "user@example.com", "secret")))
        advanceUntilIdle()
        assertEquals(SessionState.Unauthenticated, manager.state.value)
        assertNull(vm.companyState.value.company)
        assertEquals(LoginUiState.Idle, vm.loginState.value)
    }
}
