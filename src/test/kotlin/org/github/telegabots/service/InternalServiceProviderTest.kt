package org.github.telegabots.service

import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.UserService
import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.test.mockStrict
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import java.util.function.Supplier
import kotlin.test.assertNotEquals

/**
 * Test class for [InternalServiceProvider].
 */
class InternalServiceProviderTest {
    /**
     * Test method for [InternalServiceProvider.getService].
     */
    @Test
    fun `getService must return the same instance`() {
        val userProviderMock = mockStrict(ServiceProvider::class.java)
        doReturn(TestService()).`when`(userProviderMock).getService(TestService::class.java)
        val serviceProvider: ServiceProvider = createProvider(userProviderMock)

        val service1 = serviceProvider.getService(TestService::class.java)
        assertNotNull(service1)
        val service2 = serviceProvider.getService(TestService::class.java)
        assertSame(service1, service2)

        verify(userProviderMock).getService(TestService::class.java)
    }

    /**
     * Test method for [InternalServiceProvider.getUserService].
     */
    @Test
    fun `getUserService must return the same instance`() {
        val userProviderMock = mockStrict(ServiceProvider::class.java)
        doReturn(UserTestService()).`when`(userProviderMock).getUserService(UserTestService::class.java, 42L)
        val serviceProvider: ServiceProvider = createProvider(userProviderMock)

        val service1 = serviceProvider.getUserService(UserTestService::class.java, 42L)
        assertNotNull(service1)
        val service2 = serviceProvider.getUserService(UserTestService::class.java, 42L)
        assertSame(service1, service2)

        verify(userProviderMock).getUserService(UserTestService::class.java, 42L)
    }

    /**
     * Test method for [InternalServiceProvider.getSupplier].
     */
    @Test
    fun `getSupplier must return the same instance`() {
        val userProviderMock = mockStrict(ServiceProvider::class.java)
        doReturn(Supplier { LocalDateTime.now() }).`when`(userProviderMock).getSupplier(LocalDateTime::class.java)
        val serviceProvider: ServiceProvider = createProvider(userProviderMock)

        val supplier1 = serviceProvider.getSupplier(LocalDateTime::class.java)
        val supplier2 = serviceProvider.getSupplier(LocalDateTime::class.java)
        assertSame(supplier1!!, supplier2!!)
        val time1 = supplier1.get()
        Thread.sleep(200)
        val time2 = supplier1.get()
        assertNotEquals(time1, time2, "Supplier can return different values")

        verify(userProviderMock).getSupplier(LocalDateTime::class.java)
    }

    private fun createProvider(userProvider: ServiceProvider): ServiceProvider {
        val jsonService = mockStrict(JsonService::class.java)
        val config = mockStrict(BotConfig::class.java)
        val serviceProvider: ServiceProvider = InternalServiceProvider(userProvider, jsonService, config)
        return serviceProvider
    }
}

open class TestService : Service

open class UserTestService : UserService {
    override fun userId(): Long = 42L
}
